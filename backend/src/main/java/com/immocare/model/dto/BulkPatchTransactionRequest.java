package com.immocare.model.dto;

import java.util.List;

import com.immocare.model.enums.TransactionStatus;

import jakarta.validation.constraints.NotEmpty;

/**
 * Request body for PATCH /api/v1/transactions/bulk.
 *
 * All patch fields are optional — only non-null fields are applied.
 * At least one patch field must be present (validated in service).
 */
public record BulkPatchTransactionRequest(

        /** IDs of transactions to patch. Required, min 1. */
        @NotEmpty(message = "At least one transaction ID is required") List<Long> ids,

        /**
         * New status to apply. Null = no change.
         * RECONCILED transactions are always skipped.
         */
        TransactionStatus status,

        /**
         * New subcategory to apply. Null = no change.
         * 0 = explicit clear (remove subcategory).
         */
        Long subcategoryId

) {
}