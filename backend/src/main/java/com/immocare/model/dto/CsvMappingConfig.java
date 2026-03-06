package com.immocare.model.dto;

public record CsvMappingConfig(
    String delimiter,
    String dateFormat,
    int skipHeaderRows,
    int colDate,
    int colAmount,
    int colDescription,
    int colCounterpartyName,
    int colCounterpartyAccount,
    int colExternalReference,
    int colBankAccount,
    int colValueDate,
    int suggestionConfidenceThreshold
) {}
