package com.immocare.model.dto;

import java.time.LocalDateTime;

/**
 * Read-only DTO returned by all user endpoints.
 * {@code passwordHash} is intentionally excluded.
 */
public record UserDTO(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
