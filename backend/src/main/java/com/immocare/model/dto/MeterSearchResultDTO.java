package com.immocare.model.dto;

/**
 * Lightweight DTO for the meter asset picker in transaction forms.
 * unitNumber and buildingName provide context for display — no additional call needed.
 */
public record MeterSearchResultDTO(
        Long id,
        String label,
        String type,
        String ownerType,
        Long ownerId,
        String unitNumber,
        String buildingName
) {}
