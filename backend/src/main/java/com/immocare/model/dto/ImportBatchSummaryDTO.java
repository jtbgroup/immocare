package com.immocare.model.dto;

import java.time.LocalDateTime;

/**
 * Lightweight summary of an import batch — used for the import history list.
 * UC004_ESTATE_PLACEHOLDER Phase 4.
 */
public record ImportBatchSummaryDTO(
        Long id,
        String filename,
        int totalRows,
        int importedCount,
        int duplicateCount,
        int errorCount,
        LocalDateTime importedAt,
        String createdByUsername
) {}
