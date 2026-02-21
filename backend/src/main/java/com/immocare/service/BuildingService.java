package com.immocare.service;

import com.immocare.exception.BuildingHasUnitsException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.mapper.BuildingMapper;
import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import com.immocare.repository.BuildingRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Building management.
 * Implements business logic for UC001 - Manage Buildings.
 */
@Service
@Transactional(readOnly = true)
public class BuildingService {

  private final BuildingRepository buildingRepository;
  private final BuildingMapper buildingMapper;

  public BuildingService(BuildingRepository buildingRepository, BuildingMapper buildingMapper,HousingUnitRepository housingUnitRepository) {
    this.buildingRepository = buildingRepository;
    this.buildingMapper = buildingMapper;
    this.housingUnitRepository = housingUnitRepository;
  }

  /**
   * Get all buildings with pagination, filtering, and search.
   * Implements US004 - View Buildings List.
   * 
   * @param city optional city filter
   * @param search optional search term
   * @param pageable pagination parameters
   * @return page of building DTOs
   */
  public Page<BuildingDTO> getAllBuildings(String city, String search, Pageable pageable) {
    Page<Building> buildingsPage;

    if (city != null && !city.isBlank() && search != null && !search.isBlank()) {
      buildingsPage = buildingRepository.searchBuildingsByCity(city, search, pageable);
    } else if (search != null && !search.isBlank()) {
      buildingsPage = buildingRepository.searchBuildings(search, pageable);
    } else if (city != null && !city.isBlank()) {
      buildingsPage = buildingRepository.findByCity(city, pageable);
    } else {
      buildingsPage = buildingRepository.findAll(pageable);
    }

    return buildingsPage.map(building -> buildingMapper.toDTOWithUnitCount(building, 0L));
  }

  /**
   * Get a building by ID.
   * 
   * @param id the building ID
   * @return the building DTO
   * @throws BuildingNotFoundException if building not found
   */
  public BuildingDTO getBuildingById(Long id) {
    Building building = findBuildingEntityById(id);
    return buildingMapper.toDTO(building);
  }

  /**
   * Create a new building.
   * Implements US001 - Create Building.
   * 
   * @param request the creation request
   * @return the created building DTO
   */
  @Transactional
  public BuildingDTO createBuilding(CreateBuildingRequest request) {
    Building building = buildingMapper.toEntity(request);
    
    // TODO: Set createdBy from authenticated user (security context)
    // building.setCreatedBy(getCurrentUser());
    
    Building savedBuilding = buildingRepository.save(building);
    return buildingMapper.toDTO(savedBuilding);
  }

  /**
   * Update an existing building.
   * Implements US002 - Edit Building.
   * 
   * @param id the building ID
   * @param request the update request
   * @return the updated building DTO
   * @throws BuildingNotFoundException if building not found
   */
  @Transactional
  public BuildingDTO updateBuilding(Long id, UpdateBuildingRequest request) {
    Building building = findBuildingEntityById(id);
    
    buildingMapper.updateEntityFromRequest(request, building);
    
    Building updatedBuilding = buildingRepository.save(building);
    return buildingMapper.toDTO(updatedBuilding);
  }

  /**
   * Delete a building.
   * Implements US003 - Delete Building.
   * Business Rule BR-UC001-03: Cannot delete building with housing units.
   * 
   * @param id the building ID
   * @throws BuildingNotFoundException if building not found
   * @throws BuildingHasUnitsException if building has housing units
   */
  @Transactional
  public void deleteBuilding(Long id) {
    Building building = findBuildingEntityById(id);
    
   long unitCount = housingUnitRepository.countByBuildingId(id);
    
    if (unitCount > 0) {
      throw new BuildingHasUnitsException(id, unitCount);
    }
    
    buildingRepository.delete(building);
  }

  /**
   * Get all distinct cities from buildings.
   * Used for filtering in US004.
   * 
   * @return list of city names
   */
  public List<String> getAllCities() {
    return buildingRepository.findDistinctCities();
  }

  /**
   * Find building entity by ID or throw exception.
   * 
   * @param id the building ID
   * @return the building entity
   * @throws BuildingNotFoundException if not found
   */
  private Building findBuildingEntityById(Long id) {
    return buildingRepository.findById(id)
        .orElseThrow(() -> new BuildingNotFoundException(id));
  }
}
