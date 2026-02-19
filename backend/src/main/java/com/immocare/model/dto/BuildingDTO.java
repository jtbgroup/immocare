package com.immocare.model.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Building entity.
 * Used for API responses to avoid exposing entity internals.
 */
public record BuildingDTO(
    Long id,
    String name,
    String streetAddress,
    String postalCode,
    String city,
    String country,
    String ownerName,
    String createdByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long unitCount
) {
}
