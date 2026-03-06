package com.immocare.model.dto;

import com.immocare.model.enums.TransactionDirection;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedCsvRow(
    int rowNumber,
    String rawLine,
    LocalDate transactionDate,
    LocalDate valueDate,
    BigDecimal amount,
    TransactionDirection direction,
    String description,
    String counterpartyName,
    String counterpartyAccount,
    String externalReference,
    String bankAccountIban,
    String parseError
) {}
