package com.immocare.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BuildingHasUnitsException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.mapper.BuildingMapper;
import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PersonRepository;

/**
 * Service layer for Building management.
 * UC016 Phase 2: all operations are now scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final PersonRepository personRepository;
    private final EstateRepository estateRepository;
    private final BuildingMapper buildingMapper;

    public BuildingService(
            BuildingRepository buildingRepository,
            HousingUnitRepository housingUnitRepository,
            PersonRepository personRepository,
            EstateRepository estateRepository,
            BuildingMapper buildingMapper) {
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.personRepository = personRepository;
        this.estateRepository = estateRepository;
        this.buildingMapper = buildingMapper;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public Page<BuildingDTO> getAllBuildings(UUID estateId, String city, String search, Pageable pageable) {
        Page<Building> buildingsPage;

        if (city != null && !city.isBlank() && search != null && !search.isBlank()) {
            buildingsPage = buildingRepository.searchByEstateAndCity(estateId, city, search, pageable);
        } else if (search != null && !search.isBlank()) {
            buildingsPage = buildingRepository.searchByEstate(estateId, search, pageable);
        } else if (city != null && !city.isBlank()) {
            buildingsPage = buildingRepository.findByEstateIdAndCity(estateId, city, pageable);
        } else {
            buildingsPage = buildingRepository.findByEstateIdOrderByNameAsc(estateId, pageable);
        }

        return buildingsPage.map(building -> {
            long unitCount = housingUnitRepository.countByBuildingId(building.getId());
            return buildingMapper.toDTOWithUnitCount(building, unitCount);
        });
    }

    public BuildingDTO getBuildingById(UUID estateId, Long id) {
        verifyBuildingBelongsToEstate(estateId, id);
        Building building = findBuildingEntityById(id);
        long unitCount = housingUnitRepository.countByBuildingId(id);
        return buildingMapper.toDTOWithUnitCount(building, unitCount);
    }

    public List<String> getAllCities(UUID estateId) {
        return buildingRepository.findDistinctCitiesByEstateId(estateId);
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public BuildingDTO createBuilding(UUID estateId, CreateBuildingRequest request) {
        Estate estate = findEstateOrThrow(estateId);
        Building building = buildingMapper.toEntity(request);
        building.setEstate(estate);
        building.setOwner(resolveOwner(request.ownerId()));
        Building savedBuilding = buildingRepository.save(building);
        return buildingMapper.toDTOWithUnitCount(savedBuilding, 0L);
    }

    @Transactional
    public BuildingDTO updateBuilding(UUID estateId, Long id, UpdateBuildingRequest request) {
        verifyBuildingBelongsToEstate(estateId, id);
        Building building = findBuildingEntityById(id);
        buildingMapper.updateEntityFromRequest(request, building);
        building.setOwner(resolveOwner(request.ownerId()));
        Building updatedBuilding = buildingRepository.save(building);
        long unitCount = housingUnitRepository.countByBuildingId(id);
        return buildingMapper.toDTOWithUnitCount(updatedBuilding, unitCount);
    }

    @Transactional
    public void deleteBuilding(UUID estateId, Long id) {
        verifyBuildingBelongsToEstate(estateId, id);
        Building building = findBuildingEntityById(id);
        long unitCount = housingUnitRepository.countByBuildingId(id);
        if (unitCount > 0) {
            throw new BuildingHasUnitsException(id, unitCount);
        }
        buildingRepository.delete(building);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Verifies that the building with the given id belongs to the given estate.
     * Throws {@link EstateAccessDeniedException} if the building does not exist
     * in the estate, or {@link BuildingNotFoundException} if the building does not exist at all.
     */
    private void verifyBuildingBelongsToEstate(UUID estateId, Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new BuildingNotFoundException(buildingId);
        }
        if (!buildingRepository.existsByEstateIdAndId(estateId, buildingId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Building findBuildingEntityById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException(id));
    }

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }

    /**
     * Resolves a Person entity from an optional ownerId.
     * Returns null if ownerId is null (owner cleared).
     */
    private Person resolveOwner(Long ownerId) {
        if (ownerId == null) return null;
        return personRepository.findById(ownerId)
                .orElseThrow(() -> new PersonNotFoundException(ownerId));
    }
}
