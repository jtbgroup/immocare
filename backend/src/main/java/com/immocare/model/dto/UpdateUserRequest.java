package com.immocare.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/v1/users/{id}.
 * Password is NOT part of this DTO — use {@link ChangePasswordRequest} instead.
 *
 * UC004_ESTATE_PLACEHOLDER Phase 1: replaced {@code role} with {@code isPlatformAdmin}.
 */
public record UpdateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                 message = "Username may only contain letters, digits, and underscores")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        boolean isPlatformAdmin
) {}
