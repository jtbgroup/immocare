package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for adding a new meter.
 * Cross-field business rules (eanCode, installationNumber, customerNumber)
 * are enforced in {@link com.immocare.service.MeterService}.
 */
public record AddMeterRequest(

        @NotBlank(message = "Meter type is required")
        String type,

        @NotBlank(message = "Meter number is required")
        @Size(max = 50, message = "Meter number must not exceed 50 characters")
        String meterNumber,

        @Size(max = 18, message = "EAN code must not exceed 18 characters")
        String eanCode,

        @Size(max = 50, message = "Installation number must not exceed 50 characters")
        String installationNumber,

        @Size(max = 50, message = "Customer number must not exceed 50 characters")
        String customerNumber,

        @NotNull(message = "Start date is required")
        LocalDate startDate

) {}
