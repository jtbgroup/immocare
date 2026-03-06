package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;

public record FinancialTransactionDTO(
        Long id,
        String reference,
        String externalReference,
        LocalDate transactionDate,
        LocalDate valueDate,
        LocalDate accountingMonth,
        BigDecimal amount,
        TransactionDirection direction,
        String description,
        String counterpartyName,
        String counterpartyAccount,
        TransactionStatus status,
        TransactionSource source,
        Long bankAccountId,
        String bankAccountLabel,
        Long subcategoryId,
        String subcategoryName,
        Long categoryId,
        String categoryName,
        Long leaseId,
        Long suggestedLeaseId,
        Long housingUnitId,
        String unitNumber,
        Long buildingId,
        String buildingName,
        Long importBatchId,
        List<TransactionAssetLinkDTO> assetLinks,
        boolean editable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}