package com.immocare.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitHasDataException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.mapper.HousingUnitMapper;
import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PebScoreRepository;
import com.immocare.repository.PersonRepository;
import com.immocare.repository.RentHistoryRepository;
import com.immocare.repository.RoomRepository;

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
  private final PersonRepository personRepository;
  private final HousingUnitMapper housingUnitMapper;
  private final RoomRepository roomRepository;
  @Autowired
  private RentHistoryRepository rentHistoryRepository;
  @Autowired
  private PebScoreRepository pebScoreRepository;

  public HousingUnitService(HousingUnitRepository housingUnitRepository,
      BuildingRepository buildingRepository,
      PersonRepository personRepository,
      HousingUnitMapper housingUnitMapper,
      RoomRepository roomRepository) {
    this.housingUnitRepository = housingUnitRepository;
    this.buildingRepository = buildingRepository;
    this.personRepository = personRepository;
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

    // Resolve owner from ownerId
    unit.setOwner(resolveOwner(request.getOwnerId()));

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

    // Resolve owner from ownerId (null clears the unit-level owner)
    unit.setOwner(resolveOwner(request.getOwnerId()));

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
   * Resolve a Person entity from an ownerId.
   * Returns null if ownerId is null (clears the owner).
   * Throws PersonNotFoundException if the ID does not exist.
   */
  private Person resolveOwner(Long ownerId) {
    if (ownerId == null) {
      return null;
    }
    return personRepository.findById(ownerId)
        .orElseThrow(() -> new PersonNotFoundException(ownerId));
  }

  /**
   * Map entity to DTO and enrich with computed fields.
   */
  private HousingUnitDTO toEnrichedDTO(HousingUnit unit) {
    HousingUnitDTO dto = housingUnitMapper.toDTO(unit);

    // BR-UC002-09: effective owner = unit owner ?? building owner
    String effective = resolveOwnerName(unit.getOwner());
    if (effective == null) {
      effective = resolveOwnerName(unit.getBuilding().getOwner());
    }
    dto.setEffectiveOwnerName(effective);

    // UC003: real room count from repository
    dto.setRoomCount(roomRepository.countByHousingUnitId(unit.getId()));

    // Current rent
    rentHistoryRepository
        .findByHousingUnitIdAndEffectiveToIsNull(unit.getId())
        .ifPresent(r -> dto.setCurrentMonthlyRent(r.getMonthlyRent()));

    // Current PEB score
    pebScoreRepository
        .findFirstByHousingUnitIdOrderByScoreDateDesc(unit.getId())
        .ifPresent(p -> dto.setCurrentPebScore(p.getPebScore()));

    return dto;
  }

  /**
   * Build a display name from a Person entity.
   * Returns null if the person is null.
   */
  private String resolveOwnerName(com.immocare.model.entity.Person person) {
    if (person == null)
      return null;
    return (person.getFirstName() + " " + person.getLastName()).trim();
  }
}