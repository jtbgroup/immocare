package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Building entity.
 * UC004_ESTATE_PLACEHOLDER Phase 2: estateId added.
 */
public record BuildingDTO(
    Long id,
    UUID estateId,
    String name,
    String streetAddress,
    String postalCode,
    String city,
    String country,
    Long ownerId,
    String ownerName,
    String createdByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long unitCount
) {
}
