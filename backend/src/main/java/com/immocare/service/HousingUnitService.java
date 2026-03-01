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

  public HousingUnitDTO getUnitById(Long id) {
    HousingUnit unit = findEntityById(id);
    return toEnrichedDTO(unit);
  }

  // ─── Commands ───────────────────────────────────────────────────────────────

  @Transactional
  public HousingUnitDTO createUnit(CreateHousingUnitRequest request) {
    Building building = buildingRepository.findById(request.getBuildingId())
        .orElseThrow(() -> new BuildingNotFoundException(request.getBuildingId()));

    if (housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(
        building.getId(), request.getUnitNumber())) {
      throw new IllegalArgumentException(
          "Unit number '" + request.getUnitNumber() + "' already exists in this building.");
    }

    HousingUnit unit = housingUnitMapper.toEntity(request);
    unit.setBuilding(building);
    unit.setOwner(resolveOwner(request.getOwnerId()));

    // BR-UC002-04 / 05: clear terrace/garden data when flags are false
    if (!Boolean.TRUE.equals(request.getHasTerrace())) {
      unit.setTerraceSurface(null);
      unit.setTerraceOrientation(null);
    } else if (request.getTerraceSurface() == null
        || request.getTerraceSurface().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Terrace surface is required and must be greater than 0.");
    }
    if (!Boolean.TRUE.equals(request.getHasGarden())) {
      unit.setGardenSurface(null);
      unit.setGardenOrientation(null);
    } else if (request.getGardenSurface() == null
        || request.getGardenSurface().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Garden surface is required and must be greater than 0.");
    }

    HousingUnit saved = housingUnitRepository.save(unit);
    return toEnrichedDTO(saved);
  }

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

    unit.setOwner(resolveOwner(request.getOwnerId()));

    // BR-UC002-04 / 05: clear terrace/garden data when flags are false
    if (!Boolean.TRUE.equals(request.getHasTerrace())) {
      unit.setTerraceSurface(null);
      unit.setTerraceOrientation(null);
    } else {
      if (request.getTerraceSurface() == null
          || request.getTerraceSurface().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Terrace surface is required and must be greater than 0.");
      }
      unit.setTerraceOrientation(normalizeOrientation(request.getTerraceOrientation()));
    }

    if (!Boolean.TRUE.equals(request.getHasGarden())) {
      unit.setGardenSurface(null);
      unit.setGardenOrientation(null);
    } else {
      if (request.getGardenSurface() == null || request.getGardenSurface().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Garden surface is required and must be greater than 0.");
      }
      unit.setGardenOrientation(normalizeOrientation(request.getGardenOrientation()));
    }

    HousingUnit updated = housingUnitRepository.save(unit);
    return toEnrichedDTO(updated);
  }

  @Transactional
  public void deleteUnit(Long id) {
    HousingUnit unit = findEntityById(id);
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

  private Person resolveOwner(Long ownerId) {
    if (ownerId == null)
      return null;
    return personRepository.findById(ownerId)
        .orElseThrow(() -> new PersonNotFoundException(ownerId));
  }

  /**
   * Converts an empty string orientation to null.
   * Needed because the Angular select sends "" when "— Select —" is chosen,
   * and NullValuePropertyMappingStrategy.IGNORE prevents the mapper from
   * overwriting the existing entity value with null.
   */
  private String normalizeOrientation(String orientation) {
    return (orientation == null || orientation.isBlank()) ? null : orientation;
  }

  private HousingUnitDTO toEnrichedDTO(HousingUnit unit) {
    HousingUnitDTO dto = housingUnitMapper.toDTO(unit);

    // BR-UC002-09: effective owner = unit owner ?? building owner
    String effective = resolveOwnerName(unit.getOwner());
    if (effective == null) {
      effective = resolveOwnerName(unit.getBuilding().getOwner());
    }
    dto.setEffectiveOwnerName(effective);

    dto.setRoomCount(roomRepository.countByHousingUnitId(unit.getId()));

    rentHistoryRepository
        .findByHousingUnitIdAndEffectiveToIsNull(unit.getId())
        .ifPresent(r -> dto.setCurrentMonthlyRent(r.getMonthlyRent()));

    pebScoreRepository
        .findFirstByHousingUnitIdOrderByScoreDateDesc(unit.getId())
        .ifPresent(p -> dto.setCurrentPebScore(p.getPebScore()));

    return dto;
  }

  private String resolveOwnerName(Person person) {
    if (person == null)
      return null;
    return (person.getFirstName() + " " + person.getLastName()).trim();
  }
}