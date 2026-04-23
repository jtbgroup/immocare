package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/** DTOs for boiler service records — UC012 (UC011.005/UC011.006/UC011.007). */
public final class BoilerServiceDTOs {

    private BoilerServiceDTOs() {
    }

    public enum ServiceStatus {
        VALID, EXPIRING_SOON, EXPIRED, NO_SERVICE
    }

    /** Response: one maintenance record. */
    public record BoilerServiceRecordDTO(
            Long id,
            Long boilerId,
            LocalDate serviceDate,
            LocalDate validUntil,
            String notes,
            ServiceStatus status,
            LocalDateTime createdAt) {
    }

    /**
     * Body for POST /api/v1/boilers/{id}/services.
     * validUntil is optional: if null, backend calculates serviceDate + warning
     * days.
     */
    public record AddBoilerServiceRecordRequest(
            @NotNull(message = "Service date is required") LocalDate serviceDate,
            LocalDate validUntil, // nullable — admin override
            String notes) {
    }
}