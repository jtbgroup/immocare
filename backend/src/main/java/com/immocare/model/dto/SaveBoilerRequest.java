package com.immocare.model.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a boiler — UC011.
 * Used for both POST (create) and PUT (update).
 */
public record SaveBoilerRequest(

        @NotBlank(message = "Fuel type is required")
        String fuelType,

        @NotNull(message = "Installation date is required")
        LocalDate installationDate,

        @Size(max = 100)
        String brand,

        @Size(max = 100)
        String model,

        @Size(max = 100)
        String serialNumber,

        LocalDate lastServiceDate,

        LocalDate nextServiceDate,

        String notes
) {
}
