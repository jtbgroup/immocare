package com.immocare.model.dto;

import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionSummaryDTO(
        Long id,
        String reference,
        LocalDate transactionDate,
        LocalDate accountingMonth,
        TransactionDirection direction,
        BigDecimal amount,
        String counterpartyAccount,
        TransactionStatus status,
        TransactionSource source,
        String bankAccountLabel,
        String categoryName,
        String subcategoryName,
        String buildingName,
        String unitNumber,
        Long leaseId,
        Long suggestedLeaseId,
        Long buildingId,
        Long housingUnitId
) {}
