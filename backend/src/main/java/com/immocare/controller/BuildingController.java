package com.immocare.controller;

import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.service.BuildingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Building management.
 * Implements API endpoints for UC001 - Manage Buildings.
 * 
 * API Endpoints:
 * - GET    /api/v1/buildings - List all buildings with filters
 * - GET    /api/v1/buildings/{id} - Get building details
 * - POST   /api/v1/buildings - Create new building
 * - PUT    /api/v1/buildings/{id} - Update building
 * - DELETE /api/v1/buildings/{id} - Delete building
 * - GET    /api/v1/buildings/cities - Get all distinct cities
 */
@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

  private final BuildingService buildingService;

  public BuildingController(BuildingService buildingService) {
    this.buildingService = buildingService;
  }

  /**
   * Get all buildings with optional filtering and search.
   * Implements US004 - View Buildings List and US005 - Search Buildings.
   * 
   * @param city optional city filter
   * @param search optional search term
   * @param pageable pagination parameters (default: page 0, size 20)
   * @return page of buildings
   */
  @GetMapping
  public ResponseEntity<Page<BuildingDTO>> getAllBuildings(
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String search,
      @PageableDefault(size = 20) Pageable pageable) {
    Page<BuildingDTO> buildings = buildingService.getAllBuildings(city, search, pageable);
    return ResponseEntity.ok(buildings);
  }

  /**
   * Get a building by ID.
   * 
   * @param id the building ID
   * @return the building details
   */
  @GetMapping("/{id}")
  public ResponseEntity<BuildingDTO> getBuildingById(@PathVariable Long id) {
    BuildingDTO building = buildingService.getBuildingById(id);
    return ResponseEntity.ok(building);
  }

  /**
   * Create a new building.
   * Implements US001 - Create Building.
   * 
   * @param request the building creation request
   * @return the created building with HTTP 201 status
   */
  @PostMapping
  public ResponseEntity<BuildingDTO> createBuilding(
      @Valid @RequestBody CreateBuildingRequest request) {
    BuildingDTO createdBuilding = buildingService.createBuilding(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdBuilding);
  }

  /**
   * Update an existing building.
   * Implements US002 - Edit Building.
   * 
   * @param id the building ID
   * @param request the building update request
   * @return the updated building
   */
  @PutMapping("/{id}")
  public ResponseEntity<BuildingDTO> updateBuilding(
      @PathVariable Long id,
      @Valid @RequestBody UpdateBuildingRequest request) {
    BuildingDTO updatedBuilding = buildingService.updateBuilding(id, request);
    return ResponseEntity.ok(updatedBuilding);
  }

  /**
   * Delete a building.
   * Implements US003 - Delete Building.
   * 
   * @param id the building ID
   * @return success message with HTTP 200 status
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Map<String, String>> deleteBuilding(@PathVariable Long id) {
    buildingService.deleteBuilding(id);
    return ResponseEntity.ok(Map.of("message", "Building deleted successfully"));
  }

  /**
   * Get all distinct cities for filtering.
   * Supports US004 filter functionality.
   * 
   * @return list of city names
   */
  @GetMapping("/cities")
  public ResponseEntity<List<String>> getAllCities() {
    List<String> cities = buildingService.getAllCities();
    return ResponseEntity.ok(cities);
  }
}
