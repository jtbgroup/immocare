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
 * Parser for Keytrade Bank CSV exports (format observed from Jan 2026).
 *
 * Expected columns (semicolon-separated, 1 header row):
 * Date ; Description ; De ; IBAN ; Montant
 *
 * Amount format: "1234.56 EUR" — always positive, direction unknown.
 * Counterparty IBAN may be "-" for card payments / direct debits.
 * BOM (UTF-8 \uFEFF) stripped automatically.
 *
 * Direction: NULL — must be assigned manually during review.
 */
@Component
public class KeytradeCsvParser implements TransactionParser {

    public static final String CODE = "keytrade-csv-20260102";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DELIMITER = ";";

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDescription() {
        return "Keytrade CSV — colonnes: Date;Description;De;IBAN;Montant (montant positif, direction manuelle)";
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
                if (line.isBlank())
                    continue;

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
        // Handle quoted fields (description may contain semicolons)
        String[] cols = splitCsvLine(line);

        if (cols.length < 5) {
            throw new ParseException("Expected 5 columns, got " + cols.length);
        }

        String rawDate = cols[0].trim();
        String description = cols[1].trim().replaceAll("^\"|\"$", "");
        String counterpartyName = cols[2].trim().replaceAll("^\"|\"$", "");
        String counterpartyAccount = cols[3].trim();
        String rawAmount = cols[4].trim().replaceAll("^\"|\"$", "");

        // Parse date
        LocalDate date;
        try {
            date = LocalDate.parse(rawDate, DATE_FMT);
        } catch (Exception e) {
            throw new ParseException("Invalid date: " + rawDate);
        }

        // Parse amount: "1234.56 EUR" → 1234.56
        BigDecimal amount;
        try {
            String amountStr = rawAmount
                    .replaceAll("(?i)\\s*EUR\\s*$", "")
                    .trim()
                    .replace(",", ".");
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                amount = amount.abs(); // normalize — direction set manually
            }
        } catch (Exception e) {
            throw new ParseException("Invalid amount: " + rawAmount);
        }

        // Normalize IBAN (may be "-" for card payments)
        String iban = "-".equals(counterpartyAccount) ? null : counterpartyAccount;
        String name = "-".equals(counterpartyName) ? null : counterpartyName;

        String fingerprint = FingerprintUtil.compute(date, amount, iban, name, description);

        return ParsedTransaction.builder()
                .transactionDate(date)
                .amount(amount)
                .direction(null) // unknown — manual assignment during review
                .description(description)
                .counterpartyName(name)
                .counterpartyAccount(iban)
                .fingerprint(fingerprint)
                .rawLine(line)
                .rowNumber(rowNumber)
                .build();
    }

    /**
     * Simple CSV splitter that respects double-quoted fields.
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
