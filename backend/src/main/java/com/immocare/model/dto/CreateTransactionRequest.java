package com.immocare.model.dto;

import com.immocare.model.enums.TransactionDirection;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateTransactionRequest(
    @NotNull TransactionDirection direction,
    @NotNull LocalDate transactionDate,
    LocalDate valueDate,
    @NotNull LocalDate accountingMonth,
    @NotNull @Positive BigDecimal amount,
    String description,
    String counterpartyName,
    @Size(max = 50) String counterpartyAccount,
    Long bankAccountId,
    Long subcategoryId,
    Long leaseId,
    Long housingUnitId,
    Long buildingId,
    List<SaveAssetLinkRequest> assetLinks
) {}
