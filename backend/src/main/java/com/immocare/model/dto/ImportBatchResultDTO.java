package com.immocare.model.dto;

import java.util.List;

import lombok.Value;

@Value
public class ImportBatchResultDTO {
    Long batchId;
    int totalRows;
    int importedCount;
    int duplicateCount;
    int errorCount;
    List<RowError> errors;

    @Value
    public static class RowError {
        int rowNumber;
        String rawLine;
        String errorMessage;
    }

    /** Factory for parse-level failures (no batch created). */
    public static ImportBatchResultDTO error(String message) {
        return new ImportBatchResultDTO(null, 0, 0, 0, 1,
                List.of(new RowError(0, "", message)));
    }
}
