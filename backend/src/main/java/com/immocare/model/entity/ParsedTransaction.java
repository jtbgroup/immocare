package com.immocare.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

/**
 * Raw transaction data extracted by a parser.
 * Direction is always set by the parser — never null for CSV imports.
 * Null direction would only occur for hypothetical parsers that cannot
 * determine it from the file format.
 */
@Value
@Builder
public class ParsedTransaction {

    /** Transaction date from the file. */
    LocalDate transactionDate;

    /**
     * Value date from the file (may differ from transactionDate).
     * Null if the file format does not provide it.
     */
    LocalDate valueDate;

    /** Amount — always positive. */
    BigDecimal amount;

    /**
     * Direction determined from the file (sign of amount for Keytrade CSV).
     * Should not be null for well-formed parsers.
     */
    Direction direction;

    /** Full description / communication line. */
    String description;

    /**
     * Counterparty display name. May be null if not available as a dedicated
     * column.
     */
    String counterpartyName;

    /** Counterparty IBAN (may be null for card payments or direct debits). */
    String counterpartyAccount;

    /**
     * Bank's own reference for this transaction (e.g. Keytrade "Extrait" field).
     * Used as externalReference and contributes to deduplication.
     * Null if the file format does not provide it.
     */
    String externalReference;

    /** Computed SHA-256 fingerprint for duplicate detection. */
    String fingerprint;

    /** Original raw line (for error reporting). */
    String rawLine;

    /** Row number in source file (1-based, for error reporting). */
    int rowNumber;

    public enum Direction {
        INCOME, EXPENSE
    }
}