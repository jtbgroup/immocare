package com.immocare.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for setting the initial rent on a housing unit (UC010.001).
 * Also used for updating rent (UC010.002) — the service decides which flow applies.
 */
public record SetRentRequest(

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.01", message = "Rent must be positive")
        BigDecimal monthlyRent,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {}
