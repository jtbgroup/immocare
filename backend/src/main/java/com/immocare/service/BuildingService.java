package com.immocare.service;

import com.immocare.exception.BuildingHasUnitsException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.mapper.BuildingMapper;
import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PersonRepository;
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
  private final HousingUnitRepository housingUnitRepository;
  private final PersonRepository personRepository;
  private final BuildingMapper buildingMapper;

  public BuildingService(
      BuildingRepository buildingRepository,
      HousingUnitRepository housingUnitRepository,
      PersonRepository personRepository,
      BuildingMapper buildingMapper) {
    this.buildingRepository = buildingRepository;
    this.housingUnitRepository = housingUnitRepository;
    this.personRepository = personRepository;
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

    return buildingsPage.map(building -> {
      long unitCount = housingUnitRepository.countByBuildingId(building.getId());
      return buildingMapper.toDTOWithUnitCount(building, unitCount);
    });
  }

  public BuildingDTO getBuildingById(Long id) {
    Building building = findBuildingEntityById(id);
    long unitCount = housingUnitRepository.countByBuildingId(id);
    return buildingMapper.toDTOWithUnitCount(building, unitCount);
  }

  @Transactional
  public BuildingDTO createBuilding(CreateBuildingRequest request) {
    Building building = buildingMapper.toEntity(request);
    building.setOwner(resolveOwner(request.ownerId()));
    Building savedBuilding = buildingRepository.save(building);
    return buildingMapper.toDTOWithUnitCount(savedBuilding, 0L);
  }

  @Transactional
  public BuildingDTO updateBuilding(Long id, UpdateBuildingRequest request) {
    Building building = findBuildingEntityById(id);
    buildingMapper.updateEntityFromRequest(request, building);
    building.setOwner(resolveOwner(request.ownerId()));
    Building updatedBuilding = buildingRepository.save(building);
    long unitCount = housingUnitRepository.countByBuildingId(id);
    return buildingMapper.toDTOWithUnitCount(updatedBuilding, unitCount);
  }

  @Transactional
  public void deleteBuilding(Long id) {
    Building building = findBuildingEntityById(id);
    long unitCount = housingUnitRepository.countByBuildingId(id);
    if (unitCount > 0) {
      throw new BuildingHasUnitsException(id, unitCount);
    }
    buildingRepository.delete(building);
  }

  public List<String> getAllCities() {
    return buildingRepository.findDistinctCities();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private Building findBuildingEntityById(Long id) {
    return buildingRepository.findById(id)
        .orElseThrow(() -> new BuildingNotFoundException(id));
  }

  /**
   * Resolves a Person entity from an optional ownerId.
   * Returns null if ownerId is null (owner cleared).
   * Throws PersonNotFoundException if the id does not exist.
   */
  private Person resolveOwner(Long ownerId) {
    if (ownerId == null) return null;
    return personRepository.findById(ownerId)
        .orElseThrow(() -> new PersonNotFoundException(ownerId));
  }
}
