package com.immocare.model.dto;

import java.util.List;

public record ImportBatchResultDTO(
    Long batchId, int totalRows, int importedCount,
    int duplicateCount, int errorCount,
    List<ImportRowErrorDTO> errors
) {}
