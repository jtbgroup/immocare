package com.immocare.model.dto;

import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionStatus;
import java.time.LocalDate;

public record TransactionFilter(
    TransactionDirection direction,
    LocalDate from,
    LocalDate to,
    LocalDate accountingFrom,
    LocalDate accountingTo,
    Long categoryId,
    Long subcategoryId,
    Long bankAccountId,
    Long buildingId,
    Long unitId,
    TransactionStatus status,
    String search,
    Long importBatchId,
    AssetType assetType,
    Long assetId
) {}
