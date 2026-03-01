package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a boiler — UC011.
 *
 * <p>{@code daysUntilNextService} is negative when the service date is overdue.
 * {@code serviceAlert} is true when service is due within the configured warning window.
 */
public record BoilerDTO(
        Long id,
        String ownerType,
        Long ownerId,
        String brand,
        String model,
        String serialNumber,
        String fuelType,
        LocalDate installationDate,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // Computed fields
        Long daysUntilNextService,
        boolean serviceAlert
) {
}
