package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.immocare.model.enums.EstateRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * All DTOs for UC003 — Manage Estates.
 */
public final class EstateDTOs {

    private EstateDTOs() {}

    // ─── Response DTOs ────────────────────────────────────────────────────────

    /**
     * Full estate response — used by admin endpoints.
     */
    public record EstateDTO(
            UUID id,
            String name,
            String description,
            int memberCount,
            int buildingCount,
            LocalDateTime createdAt,
            String createdByUsername
    ) {}

    /**
     * Lightweight estate summary — used by the estate selector and "my estates" list.
     * {@code myRole} is null when the caller is a PLATFORM_ADMIN accessing transversally.
     */
    public record EstateSummaryDTO(
            UUID id,
            String name,
            String description,
            EstateRole myRole,
            int buildingCount,
            int unitCount
    ) {}

    /**
     * Estate dashboard — populated in Phase 6.
     */
    public record EstateDashboardDTO(
            UUID estateId,
            String estateName,
            int totalBuildings,
            int totalUnits,
            int activeLeases,
            EstatePendingAlertsDTO pendingAlerts
    ) {}

    /**
     * Pending alert counts per category.
     */
    public record EstatePendingAlertsDTO(
            int boiler,
            int fireExtinguisher,
            int leaseEnd,
            int indexation
    ) {}

    /**
     * Estate member detail.
     */
    public record EstateMemberDTO(
            Long userId,
            String username,
            String email,
            EstateRole role,
            LocalDateTime addedAt
    ) {}

    // ─── Request DTOs ─────────────────────────────────────────────────────────

    /**
     * A single member entry within a create or update request.
     */
    public record EstateMemberInput(
            @NotNull(message = "User ID is required")
            Long userId,

            @NotNull(message = "Role is required")
            EstateRole role
    ) {}

    /**
     * Request body for POST /api/v1/admin/estates.
     *
     * The {@code members} list is optional but, if provided, must contain at
     * least one entry with role MANAGER (BR-UC003-02).
     */
    public record CreateEstateRequest(
            @NotBlank(message = "Estate name is required")
            @Size(max = 100, message = "Estate name must not exceed 100 characters")
            String name,

            String description,

            @Valid
            List<EstateMemberInput> members
    ) {}

    /**
     * Request body for PUT /api/v1/admin/estates/{id}
     * and PUT /api/v1/estates/{id} (estate-manager route).
     */
    public record UpdateEstateRequest(
            @NotBlank(message = "Estate name is required")
            @Size(max = 100, message = "Estate name must not exceed 100 characters")
            String name,

            String description
    ) {}

    /**
     * Request body for POST /api/v1/estates/{id}/members.
     */
    public record AddEstateMemberRequest(
            @NotNull(message = "User ID is required")
            Long userId,

            @NotNull(message = "Role is required")
            EstateRole role
    ) {}

    /**
     * Request body for PATCH /api/v1/estates/{id}/members/{userId}.
     */
    public record UpdateEstateMemberRoleRequest(
            @NotNull(message = "Role is required")
            EstateRole role
    ) {}
}
