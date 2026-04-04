package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.immocare.model.enums.TransactionDirection;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(
        @NotNull TransactionDirection direction,
        @NotNull LocalDate transactionDate,
        LocalDate valueDate,
        @NotNull LocalDate accountingMonth,
        @NotNull @Positive BigDecimal amount,
        String description,
        @Size(max = 50) String counterpartyAccount,
        Long bankAccountId,
        Long subcategoryId,
        Long leaseId,
        Long housingUnitId,
        Long buildingId,
        List<SaveAssetLinkRequest> assetLinks) {
}
