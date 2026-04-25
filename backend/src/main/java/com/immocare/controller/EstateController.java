package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.AlertDTO;
import com.immocare.model.dto.EstateDTOs.AddEstateMemberRequest;
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstateDTOs.EstateDashboardDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberDTO;
import com.immocare.model.dto.EstateDTOs.EstateSummaryDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.dto.EstateDTOs.UpdateEstateRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.service.AlertService;
import com.immocare.service.EstateService;

import jakarta.validation.Valid;

/**
 * REST controller for estate-scoped endpoints (members, dashboard, "my
 * estates").
 * Fine-grained access control is handled per method via {@code @PreAuthorize}.
 *
 * UC003 — Manage Estates.
 *
 * Endpoints:
 * GET /api/v1/estates/mine → UC003.012 my estates
 * GET /api/v1/estates/{id}/dashboard → UC003.011 estate dashboard
 * PUT /api/v1/estates/{id} → UC003.002 edit estate (MANAGER route)
 * GET /api/v1/estates/{id}/members → UC003.006 view members
 * POST /api/v1/estates/{id}/members → UC003.007 add member
 * PATCH /api/v1/estates/{id}/members/{userId} → UC003.008 edit member role
 * DELETE /api/v1/estates/{id}/members/{userId} → UC003.009 remove member
 * GET /api/v1/estates/{id}/alerts/count → estate alerts count
 * GET /api/v1/estates/{id}/alerts → estate alerts list
 */
@RestController
public class EstateController {

    private final EstateService estateService;
    private final AlertService alertService;

    public EstateController(EstateService estateService, AlertService alertService) {
        this.estateService = estateService;
        this.alertService = alertService;
    }

    /**
     * UC003.012 — View my estates.
     * Returns all estates where the current user is a member (with their role),
     * or all estates if the user is PLATFORM_ADMIN (myRole = null).
     */
    @GetMapping("/api/v1/estates/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EstateSummaryDTO>> getMyEstates(
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(
                estateService.getMyEstates(currentUser.getId(), currentUser.isPlatformAdmin()));
    }

    /**
     * UC003.011 — View estate dashboard.
     */
    @GetMapping("/api/v1/estates/{id}/dashboard")
    @PreAuthorize("@security.isMemberOf(#id)")
    public ResponseEntity<EstateDashboardDTO> getDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getDashboard(id));
    }

    /**
     * UC003.002 — Get a single estate by ID.
     * Used by AdminEstateFormComponent in edit mode for both MANAGER and
     * PLATFORM_ADMIN.
     */
    @GetMapping("/api/v1/estates/{id}")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<EstateDTO> getEstate(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getEstateById(id));
    }

    /**
     * UC003.002 (MANAGER route) — Edit estate metadata.
     * Accessible to the estate MANAGER and to PLATFORM_ADMIN.
     * The PLATFORM_ADMIN also has the admin route in {@link EstateAdminController}.
     */
    @PutMapping("/api/v1/estates/{id}")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<com.immocare.model.dto.EstateDTOs.EstateDTO> updateEstate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstateRequest req) {
        return ResponseEntity.ok(estateService.updateEstate(id, req));
    }

    /**
     * UC003.006 — View estate members.
     * Accessible to MANAGER and PLATFORM_ADMIN only (not VIEWER).
     */
    @GetMapping("/api/v1/estates/{id}/members")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<List<EstateMemberDTO>> getMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getMembers(id));
    }

    /**
     * UC003.007 — Add a member to the estate.
     */
    @PostMapping("/api/v1/estates/{id}/members")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<EstateMemberDTO> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddEstateMemberRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        EstateMemberDTO created = estateService.addMember(id, req, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * UC003.008 — Edit a member's role.
     */
    @PatchMapping("/api/v1/estates/{id}/members/{userId}")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<EstateMemberDTO> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateEstateMemberRoleRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(estateService.updateMemberRole(id, userId, req, currentUser.getId()));
    }

    /**
     * UC003.009 — Remove a member from the estate.
     */
    @DeleteMapping("/api/v1/estates/{id}/members/{userId}")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUser currentUser) {
        estateService.removeMember(id, userId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get estate alerts count.
     */
    @GetMapping("/api/v1/estates/{id}/alerts/count")
    @PreAuthorize("@security.isMemberOf(#id)")
    public ResponseEntity<Integer> getAlertCount(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.getCount(id));
    }

    /**
     * Get estate alerts list.
     */
    @GetMapping("/api/v1/estates/{id}/alerts")
    @PreAuthorize("@security.isMemberOf(#id)")
    public ResponseEntity<List<AlertDTO>> getAlerts(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.getAll(id));
    }
}