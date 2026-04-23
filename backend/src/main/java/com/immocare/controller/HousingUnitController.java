package com.immocare.controller;

import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.service.HousingUnitService;

import jakarta.validation.Valid;

/**
 * REST controller for Housing Unit management.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all routes are now scoped to an estate.
 *
 * Endpoints:
 * GET    /api/v1/estates/{estateId}/buildings/{buildingId}/units  - list units in a building
 * GET    /api/v1/estates/{estateId}/units                        - list all units in estate
 * GET    /api/v1/estates/{estateId}/units/{id}                   - get unit details
 * POST   /api/v1/estates/{estateId}/units                        - create unit
 * PUT    /api/v1/estates/{estateId}/units/{id}                   - update unit
 * DELETE /api/v1/estates/{estateId}/units/{id}                   - delete unit
 */
@RestController
public class HousingUnitController {

    private final HousingUnitService housingUnitService;

    public HousingUnitController(HousingUnitService housingUnitService) {
        this.housingUnitService = housingUnitService;
    }

    /**
     * Get all units in a specific building within the estate.
     * UC006.004 - View Housing Unit Details (list view).
     */
    @GetMapping("/api/v1/estates/{estateId}/buildings/{buildingId}/units")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<HousingUnitDTO>> getUnitsByBuilding(
            @PathVariable UUID estateId,
            @PathVariable Long buildingId) {
        List<HousingUnitDTO> units = housingUnitService.getUnitsByBuilding(estateId, buildingId);
        return ResponseEntity.ok(units);
    }

    /**
     * Get all units across all buildings of the estate.
     */
    @GetMapping("/api/v1/estates/{estateId}/units")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<HousingUnitDTO>> getAllUnits(@PathVariable UUID estateId) {
        return ResponseEntity.ok(housingUnitService.getAllUnits(estateId));
    }

    /**
     * Get a single unit by ID within the estate.
     * UC006.004.
     */
    @GetMapping("/api/v1/estates/{estateId}/units/{id}")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<HousingUnitDTO> getUnitById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        HousingUnitDTO unit = housingUnitService.getUnitById(estateId, id);
        return ResponseEntity.ok(unit);
    }

    /**
     * Create a new housing unit within the estate.
     * UC006.001 - Create Housing Unit.
     */
    @PostMapping("/api/v1/estates/{estateId}/units")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<HousingUnitDTO> createUnit(
            @PathVariable UUID estateId,
            @Valid @RequestBody CreateHousingUnitRequest request) {
        HousingUnitDTO created = housingUnitService.createUnit(estateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing housing unit within the estate.
     * UC006.002 - Edit Housing Unit.
     */
    @PutMapping("/api/v1/estates/{estateId}/units/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<HousingUnitDTO> updateUnit(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateHousingUnitRequest request) {
        HousingUnitDTO updated = housingUnitService.updateUnit(estateId, id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a housing unit within the estate.
     * UC006.003 - Delete Housing Unit.
     */
    @DeleteMapping("/api/v1/estates/{estateId}/units/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Map<String, String>> deleteUnit(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        housingUnitService.deleteUnit(estateId, id);
        return ResponseEntity.ok(Map.of("message", "Housing unit deleted successfully"));
    }
}
