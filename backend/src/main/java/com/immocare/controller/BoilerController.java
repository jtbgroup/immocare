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
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.SaveBoilerRequest;
import com.immocare.service.BoilerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for UC011 — Manage Boilers.
 *
 * <p>Two sets of endpoints sharing the same service:
 * <ul>
 *   <li>{@code /api/v1/housing-units/{unitId}/boilers}</li>
 *   <li>{@code /api/v1/buildings/{buildingId}/boilers}</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BoilerController {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING     = "BUILDING";

    private final BoilerService boilerService;

    // ═════════════════════════════════════════════════════════════════════════
    // ALERTS (cross-owner — used by /alerts page)
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /api/v1/boilers/alerts → all boilers with service due soon */
    @GetMapping("/api/v1/boilers/alerts")
    public ResponseEntity<List<BoilerDTO>> getServiceAlerts() {
        return ResponseEntity.ok(boilerService.getServiceAlerts());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HOUSING UNIT BOILERS
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /api/v1/housing-units/{unitId}/boilers */
    @GetMapping("/api/v1/housing-units/{unitId}/boilers")
    public ResponseEntity<List<BoilerDTO>> getUnitBoilers(@PathVariable Long unitId) {
        return ResponseEntity.ok(boilerService.getBoilers(HOUSING_UNIT, unitId));
    }

    /** POST /api/v1/housing-units/{unitId}/boilers → 201 */
    @PostMapping("/api/v1/housing-units/{unitId}/boilers")
    public ResponseEntity<BoilerDTO> createUnitBoiler(
            @PathVariable Long unitId,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boilerService.create(HOUSING_UNIT, unitId, req));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUILDING BOILERS
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /api/v1/buildings/{buildingId}/boilers */
    @GetMapping("/api/v1/buildings/{buildingId}/boilers")
    public ResponseEntity<List<BoilerDTO>> getBuildingBoilers(@PathVariable Long buildingId) {
        return ResponseEntity.ok(boilerService.getBoilers(BUILDING, buildingId));
    }

    /** POST /api/v1/buildings/{buildingId}/boilers → 201 */
    @PostMapping("/api/v1/buildings/{buildingId}/boilers")
    public ResponseEntity<BoilerDTO> createBuildingBoiler(
            @PathVariable Long buildingId,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boilerService.create(BUILDING, buildingId, req));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHARED (by boiler id — owner-agnostic)
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /api/v1/boilers/{id} */
    @GetMapping("/api/v1/boilers/{id}")
    public ResponseEntity<BoilerDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(boilerService.getById(id));
    }

    /** PUT /api/v1/boilers/{id} */
    @PutMapping("/api/v1/boilers/{id}")
    public ResponseEntity<BoilerDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.ok(boilerService.update(id, req));
    }

    /** DELETE /api/v1/boilers/{id} → 204 */
    @DeleteMapping("/api/v1/boilers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boilerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
