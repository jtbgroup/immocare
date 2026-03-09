package com.immocare.model.dto;

/**
 * Result of a bulk patch operation.
 */
public record BulkPatchTransactionResult(
        /** Number of transactions successfully patched. */
        int updatedCount,

        /** Number of transactions skipped (RECONCILED or not found). */
        int skippedCount) {
}