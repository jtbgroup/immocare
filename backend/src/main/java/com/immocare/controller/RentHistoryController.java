package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.service.RentHistoryService;

import jakarta.validation.Valid;

/**
 * REST controller for UC006 — Manage Rents. Full CRUD.
 * UC004_ESTATE_PLACEHOLDER: all routes are now scoped to an estate,
 * consistent with HousingUnitController and other estate-scoped controllers.
 *
 * GET    /api/v1/estates/{estateId}/housing-units/{unitId}/rents
 * GET    /api/v1/estates/{estateId}/housing-units/{unitId}/rents/current
 * POST   /api/v1/estates/{estateId}/housing-units/{unitId}/rents
 * PUT    /api/v1/estates/{estateId}/housing-units/{unitId}/rents/{rentId}
 * DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/rents/{rentId}
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/housing-units/{unitId}/rents")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class RentHistoryController {

    private final RentHistoryService rentHistoryService;

    public RentHistoryController(RentHistoryService rentHistoryService) {
        this.rentHistoryService = rentHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<RentHistoryDTO>> getRentHistory(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return ResponseEntity.ok(rentHistoryService.getRentHistory(unitId));
    }

    @GetMapping("/current")
    public ResponseEntity<RentHistoryDTO> getCurrentRent(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return rentHistoryService.getCurrentRent(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<RentHistoryDTO> addRent(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @Valid @RequestBody SetRentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rentHistoryService.addRent(unitId, request));
    }

    @PutMapping("/{rentId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<RentHistoryDTO> updateRent(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @PathVariable Long rentId,
            @Valid @RequestBody SetRentRequest request) {
        return ResponseEntity.ok(rentHistoryService.updateRent(unitId, rentId, request));
    }

    @DeleteMapping("/{rentId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Void> deleteRent(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @PathVariable Long rentId) {
        rentHistoryService.deleteRent(unitId, rentId);
        return ResponseEntity.noContent().build();
    }
}