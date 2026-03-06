package com.immocare.model.dto;

import java.time.LocalDate;

public record ConfirmTransactionRequest(
    Long subcategoryId,
    LocalDate accountingMonth,
    Long leaseId,
    Long buildingId,
    Long housingUnitId
) {}
