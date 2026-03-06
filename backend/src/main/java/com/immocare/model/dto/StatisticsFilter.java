package com.immocare.model.dto;

import com.immocare.model.enums.TransactionDirection;
import java.time.LocalDate;

public record StatisticsFilter(
    LocalDate accountingFrom,
    LocalDate accountingTo,
    Long buildingId,
    Long unitId,
    Long bankAccountId,
    TransactionDirection direction
) {}
