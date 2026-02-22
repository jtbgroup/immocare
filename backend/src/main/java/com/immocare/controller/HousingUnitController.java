package com.immocare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Implements UC002 - Manage Housing Units.
 *
 * Endpoints:
 * GET /api/v1/buildings/{buildingId}/units — list units in a building
 * GET /api/v1/units/{id} — get unit details
 * POST /api/v1/units — create unit
 * PUT /api/v1/units/{id} — update unit
 * DELETE /api/v1/units/{id} — delete unit
 */
@RestController
public class HousingUnitController {

  private final HousingUnitService housingUnitService;

  public HousingUnitController(HousingUnitService housingUnitService) {
    this.housingUnitService = housingUnitService;
  }

  /**
   * Get all units in a building.
   * Supports US009 - View Housing Unit Details (list view).
   */
  @GetMapping("/api/v1/buildings/{buildingId}/units")
  public ResponseEntity<List<HousingUnitDTO>> getUnitsByBuilding(
      @PathVariable Long buildingId) {
    List<HousingUnitDTO> units = housingUnitService.getUnitsByBuilding(buildingId);
    return ResponseEntity.ok(units);
  }

  /**
   * Get a single unit by ID.
   * Supports US009.
   */
  @GetMapping("/api/v1/units/{id}")
  public ResponseEntity<HousingUnitDTO> getUnitById(@PathVariable Long id) {
    HousingUnitDTO unit = housingUnitService.getUnitById(id);
    return ResponseEntity.ok(unit);
  }

  /**
   * Create a new housing unit.
   * Implements US006 - Create Housing Unit.
   */
  @PostMapping("/api/v1/units")
  public ResponseEntity<HousingUnitDTO> createUnit(
      @Valid @RequestBody CreateHousingUnitRequest request) {
    HousingUnitDTO created = housingUnitService.createUnit(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Update an existing housing unit.
   * Implements US007 - Edit Housing Unit.
   */
  @PutMapping("/api/v1/units/{id}")
  public ResponseEntity<HousingUnitDTO> updateUnit(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHousingUnitRequest request) {
    HousingUnitDTO updated = housingUnitService.updateUnit(id, request);
    return ResponseEntity.ok(updated);
  }

  /**
   * Delete a housing unit.
   * Implements US008 - Delete Housing Unit.
   */
  @DeleteMapping("/api/v1/units/{id}")
  public ResponseEntity<Map<String, String>> deleteUnit(@PathVariable Long id) {
    housingUnitService.deleteUnit(id);
    return ResponseEntity.ok(Map.of("message", "Housing unit deleted successfully"));
  }
}
