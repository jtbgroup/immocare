package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.immocare.model.enums.TransactionDirection;

/**
 * One row returned by the preview endpoint.
 * Nothing is persisted — this is a pure read-only analysis.
 */
public record ImportPreviewRowDTO(

                /** Row number in source file (1-based). */
                int rowNumber,

                /** Raw line from file, for error display. */
                String rawLine,

                /** Null if the row could not be parsed. */
                LocalDate transactionDate,

                /** Always positive. */
                BigDecimal amount,

                /**
                 * Direction from parser. Null for CSV (Keytrade) — must be set manually.
                 */
                TransactionDirection direction,

                String description,
                String counterpartyName,
                String counterpartyAccount,

                /**
                 * SHA-256 fingerprint — matches import_fingerprint column.
                 * Used to detect duplicates already in DB.
                 */
                String fingerprint,

                /**
                 * True if a transaction with the same fingerprint already exists in DB.
                 */
                boolean duplicateInDb,

                /**
                 * ID of the existing transaction when duplicateInDb is true.
                 * Allows the UI to link directly to that transaction for review.
                 */
                Long duplicateTransactionId,

                /**
                 * Best subcategory suggestion from learning rules.
                 * Null if no match found.
                 */
                SubcategorySuggestionDTO suggestedSubcategory,

                /**
                 * Suggested lease from person IBAN matching.
                 * Null if no match found.
                 */
                SuggestedLeaseDTO suggestedLease,

                /** Parse error message — non-null means this row is invalid. */
                String parseError

) {

        /** Nested DTO for the suggested lease info. */
        public record SuggestedLeaseDTO(
                        Long leaseId,
                        Long unitId,
                        String unitNumber,
                        Long buildingId,
                        String buildingName,
                        Long personId,
                        String personFullName) {
        }
}