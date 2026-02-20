package com.immocare.model.dto;

/**
 * DTO returned by GET /api/v1/auth/me.
 * Never exposes password_hash or internal IDs.
 */
public record AuthUserDTO(String username, String role) {}
