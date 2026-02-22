package com.immocare.controller;

import java.util.List;

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
 * REST controller for UC005 — Manage Rents. Full CRUD.
 *
 * GET /api/v1/housing-units/{unitId}/rents → history
 * GET /api/v1/housing-units/{unitId}/rents/current → current rent
 * POST /api/v1/housing-units/{unitId}/rents → add rent record
 * PUT /api/v1/housing-units/{unitId}/rents/{rentId} → edit rent record
 * DELETE /api/v1/housing-units/{unitId}/rents/{rentId} → delete rent record
 */
@RestController
@RequestMapping("/api/v1/housing-units/{unitId}/rents")
@PreAuthorize("hasRole('ADMIN')")
public class RentHistoryController {

    private final RentHistoryService rentHistoryService;

    public RentHistoryController(RentHistoryService rentHistoryService) {
        this.rentHistoryService = rentHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<RentHistoryDTO>> getRentHistory(@PathVariable Long unitId) {
        return ResponseEntity.ok(rentHistoryService.getRentHistory(unitId));
    }

    @GetMapping("/current")
    public ResponseEntity<RentHistoryDTO> getCurrentRent(@PathVariable Long unitId) {
        return rentHistoryService.getCurrentRent(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<RentHistoryDTO> addRent(
            @PathVariable Long unitId,
            @Valid @RequestBody SetRentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rentHistoryService.addRent(unitId, request));
    }

    @PutMapping("/{rentId}")
    public ResponseEntity<RentHistoryDTO> updateRent(
            @PathVariable Long unitId,
            @PathVariable Long rentId,
            @Valid @RequestBody SetRentRequest request) {
        return ResponseEntity.ok(rentHistoryService.updateRent(unitId, rentId, request));
    }

    @DeleteMapping("/{rentId}")
    public ResponseEntity<Void> deleteRent(
            @PathVariable Long unitId,
            @PathVariable Long rentId) {
        rentHistoryService.deleteRent(unitId, rentId);
        return ResponseEntity.noContent().build();
    }
}