package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for replacing an active meter (append-only replace operation).
 * The old meter is closed (endDate = newStartDate) and a new meter is created atomically.
 */
public record ReplaceMeterRequest(

        @NotBlank(message = "New meter number is required")
        @Size(max = 50, message = "New meter number must not exceed 50 characters")
        String newMeterNumber,

        @Size(max = 18, message = "EAN code must not exceed 18 characters")
        String newEanCode,

        @Size(max = 50, message = "Installation number must not exceed 50 characters")
        String newInstallationNumber,

        @Size(max = 50, message = "Customer number must not exceed 50 characters")
        String newCustomerNumber,

        @NotNull(message = "New start date is required")
        LocalDate newStartDate,

        /** Optional — BROKEN | END_OF_LIFE | UPGRADE | CALIBRATION_ISSUE | OTHER */
        String reason

) {}
