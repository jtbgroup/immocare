package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.immocare.model.enums.TransactionDirection;

public record ParsedCsvRow(
        int rowNumber,
        String rawLine,
        LocalDate transactionDate,
        LocalDate valueDate,
        BigDecimal amount,
        TransactionDirection direction,
        String description,
        String counterpartyAccount,
        String externalReference,
        String bankAccountIban,
        String parseError) {
}
