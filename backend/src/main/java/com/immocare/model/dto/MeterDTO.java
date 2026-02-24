package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a meter record.
 * {@code status} is computed: "ACTIVE" when endDate is null, "CLOSED" otherwise.
 */
public record MeterDTO(
        Long id,
        String type,
        String meterNumber,
        String label,               // optional human-readable label (e.g. "Cuisine", "Cave")
        String eanCode,
        String installationNumber,
        String customerNumber,
        String ownerType,
        Long ownerId,
        LocalDate startDate,
        LocalDate endDate,
        String status,              // computed: "ACTIVE" | "CLOSED"
        LocalDateTime createdAt
) {}
