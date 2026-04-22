package com.immocare.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.EstateDTOs.CreateEstateRequest;
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.service.EstateService;

import jakarta.validation.Valid;

/**
 * REST controller for platform-level estate management.
 * All endpoints require PLATFORM_ADMIN access.
 *
 * UC016 — Manage Estates (Phase 1).
 *
 * Endpoints:
 * GET /api/v1/admin/estates → US095 list all estates
 * POST /api/v1/admin/estates → US092 create estate
 * PUT /api/v1/admin/estates/{id} → US093 edit estate
 * DELETE /api/v1/admin/estates/{id} → US094 delete estate
 */
@RestController
@RequestMapping("/api/v1/admin/estates")
@PreAuthorize("@security.isPlatformAdmin()")
public class EstateAdminController {

    private final EstateService estateService;

    public EstateAdminController(EstateService estateService) {
        this.estateService = estateService;
    }

    /**
     * US095 — List all estates.
     *
     * @param search optional name filter (case-insensitive, partial match)
     * @param page   zero-based page number (default 0)
     * @param size   page size (default 20)
     * @param sort   sort field and direction (default "name,asc")
     */
    @GetMapping
    public ResponseEntity<Page<EstateDTO>> getAllEstates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(estateService.getAllEstates(search, pageable));
    }

    /**
     * Get a single estate by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EstateDTO> getEstate(@PathVariable UUID id) {
        return ResponseEntity.ok(estateService.getEstateById(id));
    }

    /**
     * US092 — Create a new estate.
     * Optional {@code firstManagerId} assigns a MANAGER role at creation time
     * (US096).
     */
    @PostMapping
    public ResponseEntity<EstateDTO> createEstate(
            @Valid @RequestBody CreateEstateRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        EstateDTO created = estateService.createEstate(req, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * US093 — Edit an existing estate.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EstateDTO> updateEstate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstateRequest req) {
        return ResponseEntity.ok(estateService.updateEstate(id, req));
    }

    /**
     * US094 — Delete an estate.
     * Blocked if the estate contains buildings (BR-UC016-09).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEstate(@PathVariable UUID id) {
        estateService.deleteEstate(id);
        return ResponseEntity.noContent().build();
    }
}
