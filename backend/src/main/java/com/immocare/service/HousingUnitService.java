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
import com.immocare.repository.RoomRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Housing Unit management.
 * Implements business logic for UC002 - Manage Housing Units.
 *
 * Updated for UC003: roomCount is now populated from the real RoomRepository.
 */
@Service
@Transactional(readOnly = true)
public class HousingUnitService {

  private final HousingUnitRepository housingUnitRepository;
  private final BuildingRepository buildingRepository;
  private final HousingUnitMapper housingUnitMapper;
  private final RoomRepository roomRepository;

  public HousingUnitService(HousingUnitRepository housingUnitRepository,
                             BuildingRepository buildingRepository,
                             HousingUnitMapper housingUnitMapper,
                             RoomRepository roomRepository) {
    this.housingUnitRepository = housingUnitRepository;
    this.buildingRepository = buildingRepository;
    this.housingUnitMapper = housingUnitMapper;
    this.roomRepository = roomRepository;
  }

  // ─── Queries ────────────────────────────────────────────────────────────────

  /**
   * Get all units belonging to a building.
   * Implements US009 - View Housing Unit Details (list within building).
   */
  public List<HousingUnitDTO> getUnitsByBuilding(Long buildingId) {
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

    HousingUnit unit = housingUnitMapper.toEntity(request);
    unit.setBuilding(building);

    // BR-UC002-04 / 05: clear terrace/garden data when flags are false
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

    housingUnitMapper.updateEntityFromRequest(request, unit);

    // BR-UC002-04 / 05: clear terrace/garden data when flags are false
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
   * BR-UC002-06: blocked when unit has rooms.
   */
  @Transactional
  public void deleteUnit(Long id) {
    HousingUnit unit = findEntityById(id);

    // UC003: check real room count
    long roomCount = roomRepository.countByHousingUnitId(id);

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

    // UC003: real room count from repository
    dto.setRoomCount(roomRepository.countByHousingUnitId(unit.getId()));

    return dto;
  }
}
