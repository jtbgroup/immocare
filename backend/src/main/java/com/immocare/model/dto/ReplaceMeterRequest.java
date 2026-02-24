package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for replacing an active meter.
 * The current meter is closed (endDate set) and a new meter is created atomically.
 */
public record ReplaceMeterRequest(

        @NotBlank(message = "New meter number is required")
        @Size(max = 50, message = "Meter number must not exceed 50 characters")
        String newMeterNumber,

        @Size(max = 100, message = "Label must not exceed 100 characters")
        String newLabel,

        @Size(max = 18, message = "EAN code must not exceed 18 characters")
        String newEanCode,

        @Size(max = 50, message = "Installation number must not exceed 50 characters")
        String newInstallationNumber,

        @Size(max = 50, message = "Customer number must not exceed 50 characters")
        String newCustomerNumber,

        @NotNull(message = "Start date is required")
        LocalDate newStartDate,

        String reason   // optional: BROKEN, END_OF_LIFE, UPGRADE, CALIBRATION_ISSUE, OTHER

) {}
