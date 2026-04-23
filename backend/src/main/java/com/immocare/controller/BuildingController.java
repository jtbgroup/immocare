package com.immocare.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.service.BuildingService;

import jakarta.validation.Valid;

/**
 * REST controller for Building management.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all routes are now scoped to an estate.
 *
 * API Endpoints:
 * - GET    /api/v1/estates/{estateId}/buildings         - List buildings (UC005.004, UC005.005)
 * - GET    /api/v1/estates/{estateId}/buildings/{id}    - Get building details
 * - GET    /api/v1/estates/{estateId}/buildings/cities  - Get distinct cities
 * - POST   /api/v1/estates/{estateId}/buildings         - Create building (UC005.001)
 * - PUT    /api/v1/estates/{estateId}/buildings/{id}    - Update building (UC005.002)
 * - DELETE /api/v1/estates/{estateId}/buildings/{id}    - Delete building (UC005.003)
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    /**
     * Get all buildings within an estate with optional filtering and search.
     * UC005.004 - View Buildings List, UC005.005 - Search Buildings.
     */
    @GetMapping
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<Page<BuildingDTO>> getAllBuildings(
            @PathVariable UUID estateId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BuildingDTO> buildings = buildingService.getAllBuildings(estateId, city, search, pageable);
        return ResponseEntity.ok(buildings);
    }

    /**
     * Get a building by ID, scoped to the estate.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<BuildingDTO> getBuildingById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        BuildingDTO building = buildingService.getBuildingById(estateId, id);
        return ResponseEntity.ok(building);
    }

    /**
     * Get all distinct cities for buildings in the estate.
     */
    @GetMapping("/cities")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<String>> getAllCities(@PathVariable UUID estateId) {
        List<String> cities = buildingService.getAllCities(estateId);
        return ResponseEntity.ok(cities);
    }

    /**
     * Create a new building within the estate.
     * UC005.001 - Create Building.
     */
    @PostMapping
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<BuildingDTO> createBuilding(
            @PathVariable UUID estateId,
            @Valid @RequestBody CreateBuildingRequest request) {
        BuildingDTO createdBuilding = buildingService.createBuilding(estateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBuilding);
    }

    /**
     * Update an existing building within the estate.
     * UC005.002 - Edit Building.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<BuildingDTO> updateBuilding(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBuildingRequest request) {
        BuildingDTO updatedBuilding = buildingService.updateBuilding(estateId, id, request);
        return ResponseEntity.ok(updatedBuilding);
    }

    /**
     * Delete a building within the estate.
     * UC005.003 - Delete Building.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Map<String, String>> deleteBuilding(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        buildingService.deleteBuilding(estateId, id);
        return ResponseEntity.ok(Map.of("message", "Building deleted successfully"));
    }
}
