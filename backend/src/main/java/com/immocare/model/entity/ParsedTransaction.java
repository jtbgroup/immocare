package com.immocare.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

/**
 * Raw transaction data extracted by a parser.
 * Direction may be null if the parser cannot determine it (e.g. CSV without
 * sign).
 */
@Value
@Builder
public class ParsedTransaction {

    /** Transaction date from the file. */
    LocalDate transactionDate;

    /** Amount — always positive. */
    BigDecimal amount;

    /**
     * Direction if determinable from the file.
     * Null means DRAFT with direction to be set manually during review.
     */
    Direction direction;

    /** Full description / communication line. */
    String description;

    /** Counterparty display name. */
    String counterpartyName;

    /** Counterparty IBAN (may be null for card payments). */
    String counterpartyAccount;

    /** Computed fingerprint for duplicate detection. */
    String fingerprint;

    /** Original raw line (for error reporting). */
    String rawLine;

    /** Row number in source file (1-based, for error reporting). */
    int rowNumber;

    public enum Direction {
        INCOME, EXPENSE
    }
}
