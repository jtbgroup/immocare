package com.immocare.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.EstateAccessDeniedException;
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
import com.immocare.model.enums.LeaseStatus;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PebScoreRepository;
import com.immocare.repository.PersonRepository;
import com.immocare.repository.RentHistoryRepository;
import com.immocare.repository.RoomRepository;

/**
 * Service layer for Housing Unit management.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all operations are now scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
public class HousingUnitService {

    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final PersonRepository personRepository;
    private final HousingUnitMapper housingUnitMapper;
    private final RoomRepository roomRepository;
    private final LeaseRepository leaseRepository;
    @Autowired
    private RentHistoryRepository rentHistoryRepository;
    @Autowired
    private PebScoreRepository pebScoreRepository;

    public HousingUnitService(HousingUnitRepository housingUnitRepository,
            BuildingRepository buildingRepository,
            PersonRepository personRepository,
            HousingUnitMapper housingUnitMapper,
            RoomRepository roomRepository,
            LeaseRepository leaseRepository) {
        this.housingUnitRepository = housingUnitRepository;
        this.buildingRepository = buildingRepository;
        this.personRepository = personRepository;
        this.housingUnitMapper = housingUnitMapper;
        this.roomRepository = roomRepository;
        this.leaseRepository = leaseRepository;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<HousingUnitDTO> getUnitsByBuilding(UUID estateId, Long buildingId) {
        verifyBuildingBelongsToEstate(estateId, buildingId);
        return housingUnitRepository
                .findByEstateIdAndBuildingId(estateId, buildingId)
                .stream()
                .map(this::toEnrichedDTO)
                .collect(Collectors.toList());
    }

    public HousingUnitDTO getUnitById(UUID estateId, Long id) {
        verifyUnitBelongsToEstate(estateId, id);
        HousingUnit unit = findEntityById(id);
        return toEnrichedDTO(unit);
    }

    public List<HousingUnitDTO> getAllUnits(UUID estateId) {
        return housingUnitRepository.findAllByEstateIdOrdered(estateId)
                .stream()
                .map(this::toEnrichedDTO)
                .collect(Collectors.toList());
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public HousingUnitDTO createUnit(UUID estateId, CreateHousingUnitRequest request) {
        // The building must exist within the estate
        verifyBuildingBelongsToEstate(estateId, request.getBuildingId());

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
        }
        if (!Boolean.TRUE.equals(request.getHasGarden())) {
            unit.setGardenSurface(null);
            unit.setGardenOrientation(null);
        }

        HousingUnit saved = housingUnitRepository.save(unit);
        return toEnrichedDTO(saved);
    }

    @Transactional
    public HousingUnitDTO updateUnit(UUID estateId, Long id, UpdateHousingUnitRequest request) {
        verifyUnitBelongsToEstate(estateId, id);
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
            if (request.getGardenSurface() == null
                    || request.getGardenSurface().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Garden surface is required and must be greater than 0.");
            }
            unit.setGardenOrientation(normalizeOrientation(request.getGardenOrientation()));
        }

        HousingUnit updated = housingUnitRepository.save(unit);
        return toEnrichedDTO(updated);
    }

    @Transactional
    public void deleteUnit(UUID estateId, Long id) {
        verifyUnitBelongsToEstate(estateId, id);
        HousingUnit unit = findEntityById(id);
        long roomCount = roomRepository.countByHousingUnitId(id);
        if (roomCount > 0) {
            throw new HousingUnitHasDataException(id, roomCount);
        }
        housingUnitRepository.delete(unit);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Verifies that the building with the given id belongs to the given estate.
     */
    private void verifyBuildingBelongsToEstate(UUID estateId, Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new BuildingNotFoundException(buildingId);
        }
        if (!buildingRepository.existsByEstateIdAndId(estateId, buildingId)) {
            throw new EstateAccessDeniedException();
        }
    }

    /**
     * Verifies that the housing unit with the given id belongs to a building
     * in the given estate.
     */
    private void verifyUnitBelongsToEstate(UUID estateId, Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
        if (!housingUnitRepository.existsByBuilding_Estate_IdAndId(estateId, unitId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private HousingUnit findEntityById(Long id) {
        return housingUnitRepository.findById(id)
                .orElseThrow(() -> new HousingUnitNotFoundException(id));
    }

    private Person resolveOwner(Long ownerId) {
        if (ownerId == null) return null;
        return personRepository.findById(ownerId)
                .orElseThrow(() -> new PersonNotFoundException(ownerId));
    }

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

        // Active lease status badge
        leaseRepository
                .findFirstByHousingUnitIdAndStatus(unit.getId(), LeaseStatus.ACTIVE)
                .ifPresentOrElse(
                        l -> dto.setActiveLeaseStatus("ACTIVE"),
                        () -> leaseRepository
                                .findFirstByHousingUnitIdAndStatus(unit.getId(), LeaseStatus.DRAFT)
                                .ifPresent(l -> dto.setActiveLeaseStatus("DRAFT")));

        return dto;
    }

    private String resolveOwnerName(Person person) {
        if (person == null) return null;
        return (person.getFirstName() + " " + person.getLastName()).trim();
    }
}
