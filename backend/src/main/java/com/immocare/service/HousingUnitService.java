package com.immocare.service;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitHasDataException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.HousingUnitMapper;
import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.HousingUnit;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Housing Unit management.
 * Implements business logic for UC002 - Manage Housing Units.
 */
@Service
@Transactional(readOnly = true)
public class HousingUnitService {

  private final HousingUnitRepository housingUnitRepository;
  private final BuildingRepository buildingRepository;
  private final HousingUnitMapper housingUnitMapper;

  public HousingUnitService(HousingUnitRepository housingUnitRepository,
                             BuildingRepository buildingRepository,
                             HousingUnitMapper housingUnitMapper) {
    this.housingUnitRepository = housingUnitRepository;
    this.buildingRepository = buildingRepository;
    this.housingUnitMapper = housingUnitMapper;
  }

  // ─── Queries ────────────────────────────────────────────────────────────────

  /**
   * Get all units belonging to a building.
   * Implements US009 - View Housing Unit Details (list within building).
   */
  public List<HousingUnitDTO> getUnitsByBuilding(Long buildingId) {
    // Verify building exists
    if (!buildingRepository.existsById(buildingId)) {
      throw new BuildingNotFoundException(buildingId);
    }
    return housingUnitRepository
        .findByBuildingIdOrderByFloorAscUnitNumberAsc(buildingId)
        .stream()
        .map(this::toEnrichedDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get a single unit by ID.
   * Implements US009 - View Housing Unit Details.
   */
  public HousingUnitDTO getUnitById(Long id) {
    HousingUnit unit = findEntityById(id);
    return toEnrichedDTO(unit);
  }

  // ─── Commands ───────────────────────────────────────────────────────────────

  /**
   * Create a new housing unit.
   * Implements US006 - Create Housing Unit.
   */
  @Transactional
  public HousingUnitDTO createUnit(CreateHousingUnitRequest request) {
    Building building = buildingRepository.findById(request.getBuildingId())
        .orElseThrow(() -> new BuildingNotFoundException(request.getBuildingId()));

    // BR-UC002-01: unit number unique within building
    if (housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(
            building.getId(), request.getUnitNumber())) {
      throw new IllegalArgumentException(
          "Unit number '" + request.getUnitNumber() + "' already exists in this building.");
    }

    // BR-UC002-04 / 05: conditional terrace & garden validation
    validateTerraceAndGarden(request.getHasTerrace(), request.getTerraceSurface(),
        request.getTerraceOrientation(), "Terrace");
    validateTerraceAndGarden(request.getHasGarden(), request.getGardenSurface(),
        request.getGardenOrientation(), "Garden");

    HousingUnit unit = housingUnitMapper.toEntity(request);
    unit.setBuilding(building);

    // Clear terrace/garden data when flags are false
    if (!Boolean.TRUE.equals(request.getHasTerrace())) {
      unit.setTerraceSurface(null);
      unit.setTerraceOrientation(null);
    }
    if (!Boolean.TRUE.equals(request.getHasGarden())) {
      unit.setGardenSurface(null);
      unit.setGardenOrientation(null);
    }

    HousingUnit saved = housingUnitRepository.save(unit);
    return toEnrichedDTO(saved);
  }

  /**
   * Update an existing housing unit.
   * Implements US007 - Edit Housing Unit.
   */
  @Transactional
  public HousingUnitDTO updateUnit(Long id, UpdateHousingUnitRequest request) {
    HousingUnit unit = findEntityById(id);

    // BR-UC002-01: unit number unique within building (excluding self)
    if (!unit.getUnitNumber().equalsIgnoreCase(request.getUnitNumber()) &&
        housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCaseExcluding(
            unit.getBuilding().getId(), request.getUnitNumber(), id)) {
      throw new IllegalArgumentException(
          "Unit number '" + request.getUnitNumber() + "' already exists in this building.");
    }

    // BR-UC002-04 / 05: conditional terrace & garden validation
    validateTerraceAndGarden(request.getHasTerrace(), request.getTerraceSurface(),
        request.getTerraceOrientation(), "Terrace");
    validateTerraceAndGarden(request.getHasGarden(), request.getGardenSurface(),
        request.getGardenOrientation(), "Garden");

    housingUnitMapper.updateEntityFromRequest(request, unit);

    // Clear terrace/garden data when flags are false
    if (!Boolean.TRUE.equals(request.getHasTerrace())) {
      unit.setTerraceSurface(null);
      unit.setTerraceOrientation(null);
    }
    if (!Boolean.TRUE.equals(request.getHasGarden())) {
      unit.setGardenSurface(null);
      unit.setGardenOrientation(null);
    }

    HousingUnit updated = housingUnitRepository.save(unit);
    return toEnrichedDTO(updated);
  }

  /**
   * Delete a housing unit.
   * Implements US008 - Delete Housing Unit.
   * BR-UC002-06: blocked when unit has associated data.
   */
  @Transactional
  public void deleteUnit(Long id) {
    HousingUnit unit = findEntityById(id);

    // TODO: extend when Room / PEB / Rent / WaterMeter entities are implemented
    long roomCount = 0L; // housingUnitRepository.countRoomsByUnitId(id);

    if (roomCount > 0) {
      throw new HousingUnitHasDataException(id, roomCount);
    }

    housingUnitRepository.delete(unit);
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private HousingUnit findEntityById(Long id) {
    return housingUnitRepository.findById(id)
        .orElseThrow(() -> new HousingUnitNotFoundException(id));
  }

  /**
   * Map entity to DTO and enrich with computed fields.
   */
  private HousingUnitDTO toEnrichedDTO(HousingUnit unit) {
    HousingUnitDTO dto = housingUnitMapper.toDTO(unit);

    // BR-UC002-09: effective owner = unit owner ?? building owner
    String effective = unit.getOwnerName() != null
        ? unit.getOwnerName()
        : unit.getBuilding().getOwnerName();
    dto.setEffectiveOwnerName(effective);

    // Room count placeholder (0 until UC003)
    dto.setRoomCount(0L);

    return dto;
  }

  /**
   * Validate that surface and orientation are provided when the flag is true.
   */
  private void validateTerraceAndGarden(Boolean hasFeature, Object surface,
                                        Object orientation, String featureName) {
    if (!Boolean.TRUE.equals(hasFeature)) return;

    if (surface == null) {
      throw new IllegalArgumentException(featureName + " surface is required when Has "
          + featureName + " is checked.");
    }
    if (orientation == null || (orientation instanceof String s && s.isBlank())) {
      throw new IllegalArgumentException(featureName + " orientation is required when Has "
          + featureName + " is checked.");
    }
  }
}
