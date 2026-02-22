package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for water meter history entries.
 * UC006 - US026, US027, US028, US029, US030.
 *
 * <p>Computed fields:
 * <ul>
 *   <li>{@code isActive}: true when {@code removalDate} is null.</li>
 *   <li>{@code durationMonths}: months between installationDate and removalDate (or today).</li>
 *   <li>{@code status}: "Active" or "Replaced".</li>
 * </ul>
 */
public record WaterMeterHistoryDTO(
        Long id,
        Long housingUnitId,
        String meterNumber,
        String meterLocation,
        LocalDate installationDate,
        LocalDate removalDate,
        LocalDateTime createdAt,
        boolean isActive,
        long durationMonths,
        String status
) {}
