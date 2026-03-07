package com.immocare.model.entity;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.immocare.exception.ParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * Parser for Keytrade Bank PDF statements (format observed from March 2026).
 *
 * Each transaction block looks like:
 * 03/02/2026 FRANCIS VANDERSLYEN BE32310076563402 Loyers Saint Gilles 2500 EUR
 * vers : FRANCIS VANDERSLYEN BE32310076563402
 * - 2500 EUR
 *
 * "vers :" → EXPENSE (money leaving the account)
 * "de :" → INCOME (money entering the account)
 *
 * Amount line: "- 1 234,56 EUR" or "+ 1 234,56 EUR" or "- 2500 EUR"
 * Spaces may appear in amounts (thousands separator): "1 234,56" → 1234.56
 */
@Slf4j
@Component
public class KeytradePdfParser implements TransactionParser {

    public static final String CODE = "keytrade-pdf-20260301";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Date at line start: dd/MM/yyyy
    private static final Pattern DATE_LINE = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\s+(.+)$");

    // Counterparty line: "vers : NAME IBAN" or "de : NAME IBAN"
    // group(1) = direction (vers|de)
    // group(2) = counterparty name
    // group(3) = IBAN (optional)
    private static final Pattern COUNTERPARTY_LINE = Pattern.compile(
            "^(vers|de)\\s*:\\s*(.+?)\\s+(BE\\d{2}[\\dA-Z]{12,})\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Fallback: COUNTERPARTY_LINE without IBAN (name only)
    private static final Pattern COUNTERPARTY_LINE_NO_IBAN = Pattern.compile(
            "^(vers|de)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    // Amount line: "+ 1 234,56 EUR" or "- 2500 EUR"
    private static final Pattern AMOUNT_LINE = Pattern.compile(
            "^([+\\-])\\s*([\\d\\s]+[,.]?\\d*)\\s*EUR\\s*$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDescription() {
        return "Keytrade PDF — blocs multi-lignes, signe +/- explicite, vers:=dépense, de:=recette";
    }

    @Override
    public List<ParsedTransaction> parse(InputStream input) throws ParseException {
        String text;
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        } catch (Exception e) {
            throw new ParseException("Failed to read PDF: " + e.getMessage(), e);
        }

        String[] lines = text.split("\\r?\\n");
        log.debug("PDF extracted {} lines", lines.length);

        List<ParsedTransaction> results = new ArrayList<>();

        LocalDate currentDate = null;
        String currentDescription = null;
        ParsedTransaction.Direction currentDirection = null;
        String currentCounterpartyName = null;
        String currentCounterpartyIban = null;
        int currentRow = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isBlank())
                continue;

            // ── Date + description line ───────────────────────────────────
            Matcher dateMatcher = DATE_LINE.matcher(line);
            if (dateMatcher.matches()) {
                currentDate = null;
                currentDescription = null;
                currentDirection = null;
                currentCounterpartyName = null;
                currentCounterpartyIban = null;

                try {
                    currentDate = LocalDate.parse(dateMatcher.group(1), DATE_FMT);
                    currentDescription = dateMatcher.group(2).trim();
                    currentRow = i + 1;
                } catch (Exception ignored) {
                    currentDate = null;
                }
                continue;
            }

            // ── Counterparty line with IBAN ───────────────────────────────
            Matcher cpMatcher = COUNTERPARTY_LINE.matcher(line);
            if (cpMatcher.matches() && currentDate != null) {
                String dir = cpMatcher.group(1).toLowerCase();
                currentDirection = "de".equals(dir)
                        ? ParsedTransaction.Direction.INCOME
                        : ParsedTransaction.Direction.EXPENSE;
                currentCounterpartyName = cpMatcher.group(2).trim();
                currentCounterpartyIban = cpMatcher.group(3).trim();
                log.debug("  → COUNTERPARTY: name=[{}] iban=[{}]",
                        currentCounterpartyName, currentCounterpartyIban);
                continue;
            }

            // ── Counterparty line without IBAN (fallback) ─────────────────
            Matcher cpNoIbanMatcher = COUNTERPARTY_LINE_NO_IBAN.matcher(line);
            if (cpNoIbanMatcher.matches() && currentDate != null) {
                String dir = cpNoIbanMatcher.group(1).toLowerCase();
                currentDirection = "de".equals(dir)
                        ? ParsedTransaction.Direction.INCOME
                        : ParsedTransaction.Direction.EXPENSE;
                currentCounterpartyName = cpNoIbanMatcher.group(2).trim();
                currentCounterpartyIban = null;
                log.debug("  → COUNTERPARTY (no IBAN): name=[{}]", currentCounterpartyName);
                continue;
            }

            // ── Amount line ───────────────────────────────────────────────
            Matcher amtMatcher = AMOUNT_LINE.matcher(line);
            if (amtMatcher.matches() && currentDate != null) {
                String sign = amtMatcher.group(1);
                String rawAmt = amtMatcher.group(2);

                try {
                    String normalized = rawAmt.replace(" ", "").replace(",", ".");
                    BigDecimal amount = new BigDecimal(normalized);

                    ParsedTransaction.Direction direction = currentDirection;
                    if (direction == null) {
                        direction = "+".equals(sign)
                                ? ParsedTransaction.Direction.INCOME
                                : ParsedTransaction.Direction.EXPENSE;
                    }

                    String fingerprint = FingerprintUtil.compute(
                            currentDate, amount,
                            currentCounterpartyIban,
                            currentCounterpartyName,
                            currentDescription);

                    log.debug("  → SAVED: date={} amount={} direction={} name=[{}] iban=[{}]",
                            currentDate, amount, direction,
                            currentCounterpartyName, currentCounterpartyIban);

                    results.add(ParsedTransaction.builder()
                            .transactionDate(currentDate)
                            .amount(amount)
                            .direction(direction)
                            .description(currentDescription)
                            .counterpartyName(currentCounterpartyName)
                            .counterpartyAccount(currentCounterpartyIban)
                            .fingerprint(fingerprint)
                            .rawLine(line)
                            .rowNumber(currentRow)
                            .build());

                } catch (Exception e) {
                    log.warn("  → AMOUNT parse error line {}: [{}] — {}", i, line, e.getMessage());
                } finally {
                    currentDate = null;
                    currentDescription = null;
                    currentDirection = null;
                    currentCounterpartyName = null;
                    currentCounterpartyIban = null;
                }
            }
        }

        log.debug("Parsing complete: {} transactions found", results.size());

        if (results.isEmpty()) {
            throw new ParseException(
                    "No transactions found in PDF. Check that the file matches the expected Keytrade format.");
        }

        return results;
    }
}