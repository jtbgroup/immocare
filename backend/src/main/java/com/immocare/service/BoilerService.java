package com.immocare.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BoilerNotFoundException;
import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.SaveBoilerRequest;
import com.immocare.model.entity.Boiler;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for UC011 — Manage Boilers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoilerService {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING = "BUILDING";

    private final BoilerRepository boilerRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final PlatformConfigService platformConfigService;

    // ─── READ ────────────────────────────────────────────────────────────────

    public List<BoilerDTO> getBoilers(String ownerType, Long ownerId) {
        validateOwnerExists(ownerType, ownerId);
        int warningDays = platformConfigService.getInt(
                com.immocare.model.dto.PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        return boilerRepository
                .findByOwnerTypeAndOwnerIdOrderByInstallationDateDesc(ownerType, ownerId)
                .stream()
                .map(b -> toDTO(b, warningDays))
                .toList();
    }

    public BoilerDTO getById(Long id) {
        int warningDays = platformConfigService.getInt(
                com.immocare.model.dto.PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        return boilerRepository.findById(id)
                .map(b -> toDTO(b, warningDays))
                .orElseThrow(() -> new BoilerNotFoundException(id));
    }

    /**
     * Returns all boilers with service due within the configured warning window
     * (for alerts page).
     */
    public List<BoilerDTO> getServiceAlerts() {
        int warningDays = platformConfigService.getInt(
                com.immocare.model.dto.PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        LocalDate threshold = LocalDate.now().plusDays(warningDays);
        return boilerRepository.findBoilersWithServiceDueBefore(threshold)
                .stream()
                .map(b -> toDTO(b, warningDays))
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

        int warningDays = platformConfigService.getInt(
                com.immocare.model.dto.PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        return toDTO(boilerRepository.save(boiler), warningDays);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public BoilerDTO update(Long id, SaveBoilerRequest req) {
        Boiler boiler = boilerRepository.findById(id)
                .orElseThrow(() -> new BoilerNotFoundException(id));
        validateFuelType(req.fuelType());
        validateDates(req.installationDate(), req.lastServiceDate(), req.nextServiceDate());
        applyRequest(boiler, req);

        int warningDays = platformConfigService.getInt(
                com.immocare.model.dto.PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        return toDTO(boilerRepository.save(boiler), warningDays);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        if (!boilerRepository.existsById(id)) {
            throw new BoilerNotFoundException(id);
        }
        boilerRepository.deleteById(id);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

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

    private BoilerDTO toDTO(Boiler b, int warningDays) {
        Long daysUntilNextService = null;
        boolean serviceAlert = false;
        if (b.getNextServiceDate() != null) {
            daysUntilNextService = ChronoUnit.DAYS.between(LocalDate.now(), b.getNextServiceDate());
            serviceAlert = daysUntilNextService <= warningDays;
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
