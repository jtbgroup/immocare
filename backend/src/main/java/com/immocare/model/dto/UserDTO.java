package com.immocare.model.dto;

import java.time.LocalDateTime;

/**
 * Read-only DTO returned by all user endpoints.
 * {@code passwordHash} is intentionally excluded.
 *
 * UC004_ESTATE_PLACEHOLDER Phase 1: replaced {@code role} with {@code isPlatformAdmin}.
 */
public record UserDTO(
        Long id,
        String username,
        String email,
        boolean isPlatformAdmin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
