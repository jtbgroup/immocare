package com.immocare.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BoilerNotFoundException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.EstateConfigDTOs;
import com.immocare.model.dto.SaveBoilerRequest;
import com.immocare.model.entity.Boiler;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for UC012 — Manage Boilers.
 * UC004_ESTATE_PLACEHOLDER Phase 5: alert threshold is now read from
 * estate-scoped platform config.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoilerService {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING = "BUILDING";
    private static final int DEFAULT_WARNING_MONTHS = 3;

    private final BoilerRepository boilerRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final EstateConfigService platformConfigService;

    // ─── READ ────────────────────────────────────────────────────────────────

    public List<BoilerDTO> getBoilers(String ownerType, Long ownerId) {
        validateOwnerExists(ownerType, ownerId);
        UUID estateId = resolveEstateId(ownerType, ownerId);
        int warningMonths = getWarningMonths(estateId);
        return boilerRepository
                .findByOwnerTypeAndOwnerIdOrderByInstallationDateDesc(ownerType, ownerId)
                .stream()
                .map(b -> toDTO(b, warningMonths))
                .toList();
    }

    public BoilerDTO getById(Long id) {
        Boiler boiler = boilerRepository.findById(id)
                .orElseThrow(() -> new BoilerNotFoundException(id));
        UUID estateId = resolveEstateIdFromBoiler(boiler);
        int warningMonths = getWarningMonths(estateId);
        return toDTO(boiler, warningMonths);
    }

    /**
     * Returns all boilers with service due within the configured warning window
     * for the given estate.
     * UC004_ESTATE_PLACEHOLDER Phase 5: estate-scoped threshold replaces global
     * config.
     */
    public List<BoilerDTO> getServiceAlerts(UUID estateId) {
        int warningMonths = getWarningMonths(estateId);
        LocalDate threshold = LocalDate.now().plusMonths(warningMonths);
        return boilerRepository.findBoilersWithServiceDueBefore(threshold)
                .stream()
                // Filter to estate if provided
                .filter(b -> estateId == null || estateId.equals(resolveEstateIdFromBoiler(b)))
                .map(b -> toDTO(b, warningMonths))
                .toList();
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public BoilerDTO create(String ownerType, Long ownerId, SaveBoilerRequest req) {
        validateOwnerExists(ownerType, ownerId);
        validateFuelType(req.fuelType());
        validateDates(req.installationDate(), req.lastServiceDate(), req.nextServiceDate());

        Boiler boiler = new Boiler();
        boiler.setOwnerType(ownerType);
        boiler.setOwnerId(ownerId);
        applyRequest(boiler, req);

        UUID estateId = resolveEstateId(ownerType, ownerId);
        int warningMonths = getWarningMonths(estateId);
        return toDTO(boilerRepository.save(boiler), warningMonths);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public BoilerDTO update(Long id, SaveBoilerRequest req) {
        Boiler boiler = boilerRepository.findById(id)
                .orElseThrow(() -> new BoilerNotFoundException(id));
        validateFuelType(req.fuelType());
        validateDates(req.installationDate(), req.lastServiceDate(), req.nextServiceDate());
        applyRequest(boiler, req);

        UUID estateId = resolveEstateIdFromBoiler(boiler);
        int warningMonths = getWarningMonths(estateId);
        return toDTO(boilerRepository.save(boiler), warningMonths);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        if (!boilerRepository.existsById(id)) {
            throw new BoilerNotFoundException(id);
        }
        boilerRepository.deleteById(id);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int getWarningMonths(UUID estateId) {
        if (estateId == null)
            return DEFAULT_WARNING_MONTHS;
        return platformConfigService.getIntValue(
                estateId,
                EstateConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS,
                DEFAULT_WARNING_MONTHS);
    }

    private UUID resolveEstateId(String ownerType, Long ownerId) {
        try {
            if (HOUSING_UNIT.equals(ownerType)) {
                return housingUnitRepository.findById(ownerId)
                        .map(u -> u.getBuilding().getEstate().getId())
                        .orElse(null);
            }
            if (BUILDING.equals(ownerType)) {
                return buildingRepository.findById(ownerId)
                        .map(b -> b.getEstate().getId())
                        .orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private UUID resolveEstateIdFromBoiler(Boiler boiler) {
        return resolveEstateId(boiler.getOwnerType(), boiler.getOwnerId());
    }

    private void applyRequest(Boiler boiler, SaveBoilerRequest req) {
        boiler.setFuelType(req.fuelType());
        boiler.setInstallationDate(req.installationDate());
        boiler.setBrand(req.brand());
        boiler.setModel(req.model());
        boiler.setSerialNumber(req.serialNumber());
        boiler.setLastServiceDate(req.lastServiceDate());
        boiler.setNextServiceDate(req.nextServiceDate());
        boiler.setNotes(req.notes());
    }

    private BoilerDTO toDTO(Boiler b, int warningMonths) {
        Long daysUntilNextService = null;
        boolean serviceAlert = false;
        if (b.getNextServiceDate() != null) {
            daysUntilNextService = ChronoUnit.DAYS.between(LocalDate.now(), b.getNextServiceDate());
            serviceAlert = b.getNextServiceDate().isBefore(LocalDate.now().plusMonths(warningMonths));
        }
        return new BoilerDTO(
                b.getId(),
                b.getOwnerType(),
                b.getOwnerId(),
                b.getBrand(),
                b.getModel(),
                b.getSerialNumber(),
                b.getFuelType(),
                b.getInstallationDate(),
                b.getLastServiceDate(),
                b.getNextServiceDate(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                daysUntilNextService,
                serviceAlert);
    }

    private void validateOwnerExists(String ownerType, Long ownerId) {
        if (HOUSING_UNIT.equals(ownerType)) {
            if (!housingUnitRepository.existsById(ownerId))
                throw new HousingUnitNotFoundException(ownerId);
        } else if (BUILDING.equals(ownerType)) {
            if (!buildingRepository.existsById(ownerId))
                throw new BuildingNotFoundException("Building not found: " + ownerId);
        } else {
            throw new IllegalArgumentException("Invalid owner type: " + ownerType);
        }
    }

    private void validateFuelType(String fuelType) {
        if (!List.of("GAS", "OIL", "ELECTRIC", "HEAT_PUMP").contains(fuelType)) {
            throw new IllegalArgumentException("Invalid fuel type: " + fuelType);
        }
    }

    private void validateDates(LocalDate installationDate, LocalDate lastServiceDate, LocalDate nextServiceDate) {
        if (lastServiceDate != null && lastServiceDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Last service date cannot be in the future");
        }
        if (lastServiceDate != null && lastServiceDate.isBefore(installationDate)) {
            throw new IllegalArgumentException("Last service date cannot be before installation date");
        }
        if (nextServiceDate != null && nextServiceDate.isBefore(installationDate)) {
            throw new IllegalArgumentException("Next service date cannot be before installation date");
        }
    }
}
