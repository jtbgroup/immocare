package com.immocare.model.dto;

/**
 * Lightweight DTO for the boiler asset picker in transaction forms.
 * unitNumber and buildingName provide context for display — no additional call needed.
 */
public record BoilerSearchResultDTO(
        Long id,
        String label,
        String ownerType,
        Long ownerId,
        String unitNumber,
        String buildingName
) {}
