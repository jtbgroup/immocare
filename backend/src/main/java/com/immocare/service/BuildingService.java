package com.immocare.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BuildingHasUnitsException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.mapper.BuildingMapper;
import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import com.immocare.repository.BuildingRepository;

/**
 * Service layer for Building management.
 * Implements business logic for UC001 - Manage Buildings.
 */
@Service
@Transactional(readOnly = true)
public class BuildingService {

  private final BuildingRepository buildingRepository;
  private final BuildingMapper buildingMapper;

  public BuildingService(BuildingRepository buildingRepository, BuildingMapper buildingMapper) {
    this.buildingRepository = buildingRepository;
    this.buildingMapper = buildingMapper;
  }

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

  public BuildingDTO getBuildingById(Long id) {
    Building building = findBuildingEntityById(id);
    return buildingMapper.toDTO(building);
  }

  @Transactional
  public BuildingDTO createBuilding(CreateBuildingRequest request) {
    Building building = buildingMapper.toEntity(request);
    Building savedBuilding = buildingRepository.save(building);
    return buildingMapper.toDTO(savedBuilding);
  }

  @Transactional
  public BuildingDTO updateBuilding(Long id, UpdateBuildingRequest request) {
    Building building = findBuildingEntityById(id);
    buildingMapper.updateEntityFromRequest(request, building);
    Building updatedBuilding = buildingRepository.save(building);
    return buildingMapper.toDTO(updatedBuilding);
  }

  @Transactional
  public void deleteBuilding(Long id) {
    Building building = findBuildingEntityById(id);

    // HousingUnit not yet implemented — unit count hardcoded to 0
    long unitCount = 0L;

    if (unitCount > 0) {
      throw new BuildingHasUnitsException(id, unitCount);
    }

    buildingRepository.delete(building);
  }

  public List<String> getAllCities() {
    return buildingRepository.findDistinctCities();
  }

  private Building findBuildingEntityById(Long id) {
    return buildingRepository.findById(id)
        .orElseThrow(() -> new BuildingNotFoundException(id));
  }
}