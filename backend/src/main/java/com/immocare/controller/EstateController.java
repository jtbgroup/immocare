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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.EstateDTOs.AddEstateMemberRequest;
import com.immocare.model.dto.EstateDTOs.EstateDashboardDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberDTO;
import com.immocare.model.dto.EstateDTOs.EstateSummaryDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.service.EstateService;

import jakarta.validation.Valid;

/**
 * REST controller for estate-scoped endpoints (members, dashboard, "my estates").
 * Fine-grained access control is handled per method via {@code @PreAuthorize}.
 *
 * UC016 — Manage Estates (Phase 1).
 *
 * Endpoints:
 *   GET    /api/v1/estates/mine                        → US103 my estates
 *   GET    /api/v1/estates/{id}/dashboard              → US102 estate dashboard
 *   GET    /api/v1/estates/{id}/members                → US097 view members
 *   POST   /api/v1/estates/{id}/members                → US098 add member
 *   PATCH  /api/v1/estates/{id}/members/{userId}       → US099 edit member role
 *   DELETE /api/v1/estates/{id}/members/{userId}       → US100 remove member
 */
@RestController
public class EstateController {

    private final EstateService estateService;

    public EstateController(EstateService estateService) {
        this.estateService = estateService;
    }

    /**
     * US103 — View my estates.
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
     * US102 — View estate dashboard.
     * All counts are 0 in Phase 1.
     */
    @GetMapping("/api/v1/estates/{id}/dashboard")
    @PreAuthorize("@security.isMemberOf(#id)")
    public ResponseEntity<EstateDashboardDTO> getDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getDashboard(id));
    }

    /**
     * US097 — View estate members.
     * Accessible to MANAGER and PLATFORM_ADMIN only (not VIEWER).
     */
    @GetMapping("/api/v1/estates/{id}/members")
    @PreAuthorize("@security.isManagerOf(#id)")
    public ResponseEntity<List<EstateMemberDTO>> getMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getMembers(id));
    }

    /**
     * US098 — Add a member to the estate.
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
     * US099 — Edit a member's role.
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
     * US100 — Remove a member from the estate.
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
}
