package com.immocare.model.dto;

/**
 * Lightweight DTO for the fire extinguisher asset picker in transaction forms.
 * unitNumber and buildingName provide context for display — no additional call needed.
 */
public record FireExtinguisherSearchResultDTO(
        Long id,
        String label,
        Long buildingId,
        String buildingName,
        Long unitId,
        String unitNumber
) {}
