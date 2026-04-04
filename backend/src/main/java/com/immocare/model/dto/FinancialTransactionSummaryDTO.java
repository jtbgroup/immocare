package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;

public record FinancialTransactionSummaryDTO(
        Long id,
        String reference,
        LocalDate transactionDate,
        LocalDate accountingMonth,
        TransactionDirection direction,
        BigDecimal amount,
        TransactionStatus status,
        TransactionSource source,
        String bankAccountLabel,
        String categoryName,
        String subcategoryName,
        String buildingName,
        String unitNumber,
        Long leaseId,
        // ── Suggestion fields (set at import, confirmed during review) ──
        Long suggestedLeaseId,
        Long buildingId,
        Long housingUnitId) {
}