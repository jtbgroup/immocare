package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.immocare.model.enums.EstateRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * All DTOs for UC016 — Manage Estates.
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
     * Estate dashboard — counts are all 0 in Phase 1.
     * Will be enriched in Phase 6 when all entities are estate-scoped.
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
     * Pending alert counts per category — all 0 in Phase 1.
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
     * Request body for POST /api/v1/admin/estates.
     */
    public record CreateEstateRequest(
            @NotBlank(message = "Estate name is required")
            @Size(max = 100, message = "Estate name must not exceed 100 characters")
            String name,

            String description,

            Long firstManagerId
    ) {}

    /**
     * Request body for PUT /api/v1/admin/estates/{id}.
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
