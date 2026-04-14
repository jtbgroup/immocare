package com.immocare.model.dto;

/**
 * DTO returned by GET /api/v1/auth/me.
 * Never exposes password_hash or internal IDs.
 *
 * UC016 Phase 1: replaced {@code role} with {@code isPlatformAdmin}.
 */
public record AuthUserDTO(String username, boolean isPlatformAdmin) {}
