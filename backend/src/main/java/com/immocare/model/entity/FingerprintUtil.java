package com.immocare.model.entity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Generates a deterministic SHA-256 fingerprint for duplicate detection.
 *
 * Fields used: date + amount (normalized) + counterparty_account +
 * description[:60]
 * The IBAN is preferred over name because names can have encoding artefacts.
 */
public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    public static String compute(
            LocalDate date,
            BigDecimal amount,
            String counterpartyAccount,
            String description) {

        // Prefer IBAN over name (more stable)
        String counterparty = (counterpartyAccount != null && !counterpartyAccount.isBlank()
                && !"-".equals(counterpartyAccount.trim()))
                        ? counterpartyAccount.trim().toUpperCase()
                        : "";

        // Normalize amount: strip trailing zeros → "1234.5" not "1234.50"
        String amt = amount.stripTrailingZeros().toPlainString();

        // Description: first 60 chars, normalized
        String desc = description != null ? normalize(description, 60) : "";

        String raw = date + "|" + amt + "|" + counterparty + "|" + desc;
        return sha256(raw);
    }

    private static String normalize(String s, int maxLen) {
        if (s == null)
            return "";
        String cleaned = s.trim().toUpperCase().replaceAll("\\s+", " ");
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
