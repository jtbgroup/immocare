package com.immocare.model.entity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.immocare.exception.ParseException;

/**
 * Parser for Keytrade Bank CSV exports (7-column format, observed from Jan
 * 2026).
 *
 * Expected columns (semicolon-separated, UTF-8 BOM, 1 header row):
 * Extrait ; Date ; Date valeur ; IBAN ; Description ; Montant ; Devise
 *
 * Column mapping:
 * Extrait → externalReference (e.g. "2023/0012/0096" — unique Keytrade ref,
 * used for dedup)
 * Date → transactionDate (dd/MM/yyyy)
 * Date valeur → valueDate (dd/MM/yyyy — may differ from transactionDate)
 * IBAN → counterpartyAccount ("-" → null)
 * Description → description (free text, may contain counterparty name)
 * Montant → amount (abs) + direction (positive → INCOME, negative → EXPENSE)
 * Devise → ignored (always EUR in practice)
 *
 * Direction is always determined from the sign of Montant — never null.
 */
@Component
public class KeytradeCsvParser implements TransactionParser {

    public static final String CODE = "keytrade-csv-20260102";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DELIMITER = ";";

    // Column indices
    private static final int COL_EXTRAIT = 0;
    private static final int COL_DATE = 1;
    private static final int COL_DATE_VALEUR = 2;
    private static final int COL_IBAN = 3;
    private static final int COL_DESCRIPTION = 4;
    private static final int COL_MONTANT = 5;
    // COL_DEVISE = 6 — ignored

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDescription() {
        return "Keytrade CSV — colonnes: Extrait;Date;Date valeur;IBAN;Description;Montant;Devise "
                + "(montant signé: positif=INCOME, négatif=EXPENSE, séparateur \";\", encodage UTF-8 BOM)";
    }

    @Override
    public List<ParsedTransaction> parse(InputStream input) throws ParseException {
        List<ParsedTransaction> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String line;
            int rowNumber = 0;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                // Strip UTF-8 BOM on first line
                if (rowNumber == 1) {
                    line = line.replace("\uFEFF", "");
                }

                // Skip header row
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // Skip blank lines
                if (line.isBlank()) {
                    continue;
                }

                try {
                    results.add(parseLine(line, rowNumber));
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new ParseException("Failed to read CSV file: " + e.getMessage(), e);
        }

        if (!errors.isEmpty() && results.isEmpty()) {
            throw new ParseException("CSV parsing failed completely: " + errors.get(0));
        }

        return results;
    }

    private ParsedTransaction parseLine(String line, int rowNumber) throws ParseException {
        String[] cols = splitCsvLine(line);

        if (cols.length < 6) {
            throw new ParseException("Expected at least 6 columns, got " + cols.length);
        }

        String extrait = clean(cols[COL_EXTRAIT]);
        String rawDate = clean(cols[COL_DATE]);
        String rawValeur = clean(cols[COL_DATE_VALEUR]);
        String rawIban = clean(cols[COL_IBAN]);
        String description = clean(cols[COL_DESCRIPTION]);
        String rawMontant = clean(cols[COL_MONTANT]);

        // Parse transaction date
        LocalDate transactionDate;
        try {
            transactionDate = LocalDate.parse(rawDate, DATE_FMT);
        } catch (Exception e) {
            throw new ParseException("Invalid date: " + rawDate);
        }

        // Parse value date (optional — fallback to transactionDate if blank or
        // unparseable)
        LocalDate valueDate;
        try {
            valueDate = (rawValeur.isBlank()) ? transactionDate : LocalDate.parse(rawValeur, DATE_FMT);
        } catch (Exception e) {
            valueDate = transactionDate;
        }

        // Parse amount — signed: positive = INCOME, negative = EXPENSE
        BigDecimal signedAmount;
        try {
            // Amount uses "." as decimal separator in this format (e.g. "900.00", "-15.00")
            signedAmount = new BigDecimal(rawMontant.replace(",", "."));
        } catch (Exception e) {
            throw new ParseException("Invalid amount: " + rawMontant);
        }

        ParsedTransaction.Direction direction = signedAmount.compareTo(BigDecimal.ZERO) >= 0
                ? ParsedTransaction.Direction.INCOME
                : ParsedTransaction.Direction.EXPENSE;

        BigDecimal amount = signedAmount.abs();

        // Normalize IBAN ("-" = no counterparty account)
        String counterpartyAccount = ("-".equals(rawIban) || rawIban.isBlank()) ? null : rawIban;

        // Counterparty name: not a dedicated column — left null (learning rules will
        // suggest)
        String counterpartyName = null;

        // Fingerprint — incorporates extrait for reliable dedup
        String fingerprint = FingerprintUtil.compute(
                transactionDate, amount, counterpartyAccount, counterpartyName, description);

        return ParsedTransaction.builder()
                .transactionDate(transactionDate)
                .valueDate(valueDate)
                .externalReference(extrait)
                .amount(amount)
                .direction(direction)
                .description(description)
                .counterpartyName(counterpartyName)
                .counterpartyAccount(counterpartyAccount)
                .fingerprint(fingerprint)
                .rawLine(line)
                .rowNumber(rowNumber)
                .build();
    }

    private String clean(String s) {
        if (s == null)
            return "";
        return s.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * Simple CSV splitter respecting double-quoted fields.
     */
    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ';' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }
}