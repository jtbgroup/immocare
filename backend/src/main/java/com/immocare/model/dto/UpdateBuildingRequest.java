package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing building.
 * ownerId references a Person entity (optional, null = clear owner).
 */
public record UpdateBuildingRequest(
    @NotBlank(message = "Building name is required")
    @Size(max = 100, message = "Building name must be 100 characters or less")
    String name,

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must be 200 characters or less")
    String streetAddress,

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must be 20 characters or less")
    String postalCode,

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be 100 characters or less")
    String city,

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must be 100 characters or less")
    String country,

    Long ownerId
) {
}
