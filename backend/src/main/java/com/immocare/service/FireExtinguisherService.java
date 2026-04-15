package com.immocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.FireExtinguisherDuplicateNumberException;
import com.immocare.exception.FireExtinguisherNotFoundException;
import com.immocare.exception.FireExtinguisherRevisionNotFoundException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.model.dto.AddRevisionRequest;
import com.immocare.model.dto.FireExtinguisherDTO.FireExtinguisherResponse;
import com.immocare.model.dto.FireExtinguisherDTO.FireExtinguisherRevisionResponse;
import com.immocare.model.dto.SaveFireExtinguisherRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.FireExtinguisher;
import com.immocare.model.entity.FireExtinguisherRevision;
import com.immocare.model.entity.HousingUnit;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.FireExtinguisherRevisionRepository;
import com.immocare.repository.HousingUnitRepository;

/**
 * Service for Fire Extinguisher management.
 * UC016 Phase 2: estate ownership verification added.
 */
@Service
@Transactional(readOnly = true)
public class FireExtinguisherService {

    private final FireExtinguisherRepository extinguisherRepository;
    private final FireExtinguisherRevisionRepository revisionRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;

    public FireExtinguisherService(
            FireExtinguisherRepository extinguisherRepository,
            FireExtinguisherRevisionRepository revisionRepository,
            BuildingRepository buildingRepository,
            HousingUnitRepository housingUnitRepository) {
        this.extinguisherRepository = extinguisherRepository;
        this.revisionRepository = revisionRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<FireExtinguisherResponse> getByBuilding(UUID estateId, Long buildingId) {
        verifyBuildingBelongsToEstate(estateId, buildingId);
        return extinguisherRepository
                .findByBuildingIdOrderByIdentificationNumberAsc(buildingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public FireExtinguisherResponse getById(UUID estateId, Long id) {
        FireExtinguisher ext = findById(id);
        verifyExtinguisherBelongsToEstate(estateId, ext);
        return toDTO(ext);
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public FireExtinguisherResponse create(UUID estateId, Long buildingId, SaveFireExtinguisherRequest req) {
        verifyBuildingBelongsToEstate(estateId, buildingId);
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException(buildingId));

        if (extinguisherRepository.existsByBuildingIdAndIdentificationNumberIgnoreCase(
                buildingId, req.identificationNumber())) {
            throw new FireExtinguisherDuplicateNumberException();
        }

        FireExtinguisher entity = new FireExtinguisher();
        entity.setBuilding(building);
        entity.setIdentificationNumber(req.identificationNumber());
        entity.setNotes(req.notes());
        entity.setUnit(resolveUnit(req.unitId(), buildingId));

        return toDTO(extinguisherRepository.save(entity));
    }

    @Transactional
    public FireExtinguisherResponse update(UUID estateId, Long id, SaveFireExtinguisherRequest req) {
        FireExtinguisher entity = findById(id);
        verifyExtinguisherBelongsToEstate(estateId, entity);

        if (extinguisherRepository.existsByBuildingIdAndNumberIgnoreCaseExcluding(
                entity.getBuilding().getId(), req.identificationNumber(), id)) {
            throw new FireExtinguisherDuplicateNumberException();
        }

        entity.setIdentificationNumber(req.identificationNumber());
        entity.setNotes(req.notes());
        entity.setUnit(resolveUnit(req.unitId(), entity.getBuilding().getId()));

        return toDTO(extinguisherRepository.save(entity));
    }

    @Transactional
    public void delete(UUID estateId, Long id) {
        FireExtinguisher entity = findById(id);
        verifyExtinguisherBelongsToEstate(estateId, entity);
        extinguisherRepository.delete(entity);
    }

    @Transactional
    public FireExtinguisherResponse addRevision(UUID estateId, Long extinguisherId, AddRevisionRequest req) {
        FireExtinguisher entity = findById(extinguisherId);
        verifyExtinguisherBelongsToEstate(estateId, entity);

        if (req.revisionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Revision date cannot be in the future");
        }

        FireExtinguisherRevision revision = new FireExtinguisherRevision();
        revision.setFireExtinguisher(entity);
        revision.setRevisionDate(req.revisionDate());
        revision.setNotes(req.notes());

        entity.getRevisions().add(revision);
        return toDTO(extinguisherRepository.save(entity));
    }

    @Transactional
    public void deleteRevision(UUID estateId, Long extinguisherId, Long revisionId) {
        FireExtinguisher entity = findById(extinguisherId);
        verifyExtinguisherBelongsToEstate(estateId, entity);

        FireExtinguisherRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new FireExtinguisherRevisionNotFoundException(revisionId));

        entity.getRevisions().remove(revision);
        revisionRepository.delete(revision);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Verifies the building belongs to the given estate.
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
     * Verifies the fire extinguisher's building belongs to the given estate.
     */
    private void verifyExtinguisherBelongsToEstate(UUID estateId, FireExtinguisher ext) {
        if (!buildingRepository.existsByEstateIdAndId(estateId, ext.getBuilding().getId())) {
            throw new EstateAccessDeniedException();
        }
    }

    private FireExtinguisher findById(Long id) {
        return extinguisherRepository.findById(id)
                .orElseThrow(() -> new FireExtinguisherNotFoundException(id));
    }

    private HousingUnit resolveUnit(Long unitId, Long buildingId) {
        if (unitId == null) {
            return null;
        }
        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Housing unit not found: " + unitId));
        if (!unit.getBuilding().getId().equals(buildingId)) {
            throw new IllegalArgumentException("The specified unit does not belong to this building");
        }
        return unit;
    }

    private FireExtinguisherResponse toDTO(FireExtinguisher entity) {
        List<FireExtinguisherRevisionResponse> revisionDTOs = entity.getRevisions().stream()
                .map(r -> new FireExtinguisherRevisionResponse(
                        r.getId(),
                        entity.getId(),
                        r.getRevisionDate(),
                        r.getNotes(),
                        r.getCreatedAt()))
                .toList();

        return new FireExtinguisherResponse(
                entity.getId(),
                entity.getBuilding().getId(),
                entity.getUnit() != null ? entity.getUnit().getId() : null,
                entity.getUnit() != null ? entity.getUnit().getUnitNumber() : null,
                entity.getIdentificationNumber(),
                entity.getNotes(),
                revisionDTOs,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
