package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a rent history record.
 * Includes computed fields {@code isCurrent} and {@code durationMonths}.
 */
public record RentHistoryDTO(
        Long id,
        Long housingUnitId,
        BigDecimal monthlyRent,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,   // null = current rent
        String notes,
        LocalDateTime createdAt,
        boolean isCurrent,
        long durationMonths
) {}
