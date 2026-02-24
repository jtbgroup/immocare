package com.immocare.service;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.MeterBusinessRuleException;
import com.immocare.exception.MeterNotFoundException;
import com.immocare.mapper.MeterMapper;
import com.immocare.model.dto.AddMeterRequest;
import com.immocare.model.dto.MeterDTO;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.entity.Meter;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for UC008 - Manage Meters.
 *
 * <h3>Append-only pattern</h3>
 * <ul>
 *   <li>Records are NEVER modified once created.</li>
 *   <li>Closing a meter sets {@code endDate}.</li>
 *   <li>Active meter = {@code endDate IS NULL}.</li>
 *   <li>Multiple active meters of the same type per owner are allowed (BR-03).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterService {

    private static final String OWNER_HOUSING_UNIT = "HOUSING_UNIT";
    private static final String OWNER_BUILDING     = "BUILDING";

    private final MeterRepository     meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository  buildingRepository;
    private final MeterMapper         meterMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns active meters for the given owner, sorted by type then startDate DESC.
     * US036, US037.
     */
    public List<MeterDTO> getActiveMeters(String ownerType, Long ownerId) {
        validateOwnerExists(ownerType, ownerId);
        return meterRepository
                .findByOwnerTypeAndOwnerIdAndEndDateIsNull(ownerType, ownerId)
                .stream()
                .sorted((a, b) -> {
                    int typeCmp = a.getType().compareTo(b.getType());
                    return typeCmp != 0 ? typeCmp : b.getStartDate().compareTo(a.getStartDate());
                })
                .map(meterMapper::toDTO)
                .toList();
    }

    /**
     * Returns full meter history (active + closed), sorted by startDate DESC.
     * US042.
     */
    public List<MeterDTO> getMeterHistory(String ownerType, Long ownerId) {
        validateOwnerExists(ownerType, ownerId);
        return meterRepository
                .findByOwnerTypeAndOwnerIdOrderByStartDateDesc(ownerType, ownerId)
                .stream()
                .map(meterMapper::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new meter for the given owner.
     * US038, US039.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>BR-01: startDate not in future</li>
     *   <li>BR-05: eanCode required for GAS and ELECTRICITY</li>
     *   <li>BR-06: installationNumber required for WATER</li>
     *   <li>BR-07: customerNumber required for WATER on BUILDING</li>
     * </ul>
     */
    @Transactional
    public MeterDTO addMeter(String ownerType, Long ownerId, AddMeterRequest request) {
        validateOwnerExists(ownerType, ownerId);
        validateMeterType(request.type());
        validateStartDateNotFuture(request.startDate());
        validateConditionalFields(request.type(), ownerType,
                request.eanCode(), request.installationNumber(), request.customerNumber());

        Meter meter = new Meter();
        meter.setType(request.type());
        meter.setMeterNumber(request.meterNumber());
        meter.setEanCode(request.eanCode());
        meter.setInstallationNumber(request.installationNumber());
        meter.setCustomerNumber(request.customerNumber());
        meter.setOwnerType(ownerType);
        meter.setOwnerId(ownerId);
        meter.setStartDate(request.startDate());
        meter.setEndDate(null); // active

        return meterMapper.toDTO(meterRepository.save(meter));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPLACE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically closes the current meter and creates a new one.
     * US040.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>BR-01: newStartDate not in future</li>
     *   <li>BR-09: newStartDate >= current meter's startDate</li>
     *   <li>BR-05/06/07: conditional fields per type and owner</li>
     *   <li>BR-08: atomic transaction</li>
     * </ul>
     */
    @Transactional
    public MeterDTO replaceMeter(String ownerType, Long ownerId, Long meterId, ReplaceMeterRequest request) {
        validateOwnerExists(ownerType, ownerId);

        Meter current = meterRepository.findByIdAndEndDateIsNull(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        validateStartDateNotFuture(request.newStartDate());

        // BR-09: newStartDate must be >= current meter's startDate
        if (request.newStartDate().isBefore(current.getStartDate())) {
            throw new MeterBusinessRuleException(
                    "Start date must be ≥ current meter start date (" + current.getStartDate() + ")");
        }

        validateConditionalFields(current.getType(), ownerType,
                request.newEanCode(), request.newInstallationNumber(), request.newCustomerNumber());

        // Close the current meter
        current.setEndDate(request.newStartDate());
        meterRepository.save(current);

        // Create the new meter
        Meter newMeter = new Meter();
        newMeter.setType(current.getType());
        newMeter.setMeterNumber(request.newMeterNumber());
        newMeter.setEanCode(request.newEanCode());
        newMeter.setInstallationNumber(request.newInstallationNumber());
        newMeter.setCustomerNumber(request.newCustomerNumber());
        newMeter.setOwnerType(ownerType);
        newMeter.setOwnerId(ownerId);
        newMeter.setStartDate(request.newStartDate());
        newMeter.setEndDate(null); // active

        return meterMapper.toDTO(meterRepository.save(newMeter));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMOVE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Closes an active meter without creating a replacement.
     * US041.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>BR-01: endDate not in future</li>
     *   <li>BR-02: endDate >= meter's startDate</li>
     * </ul>
     */
    @Transactional
    public void removeMeter(String ownerType, Long ownerId, Long meterId, LocalDate endDate) {
        validateOwnerExists(ownerType, ownerId);

        Meter meter = meterRepository.findByIdAndEndDateIsNull(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        // BR-01: endDate not in future
        if (endDate.isAfter(LocalDate.now())) {
            throw new MeterBusinessRuleException("End date cannot be in the future");
        }

        // BR-02: endDate >= startDate
        if (endDate.isBefore(meter.getStartDate())) {
            throw new MeterBusinessRuleException(
                    "End date must be ≥ start date (" + meter.getStartDate() + ")");
        }

        meter.setEndDate(endDate);
        meterRepository.save(meter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void validateOwnerExists(String ownerType, Long ownerId) {
        switch (ownerType) {
            case OWNER_HOUSING_UNIT -> {
                if (!housingUnitRepository.existsById(ownerId)) {
                    throw new HousingUnitNotFoundException(ownerId);
                }
            }
            case OWNER_BUILDING -> {
                if (!buildingRepository.existsById(ownerId)) {
                    throw new BuildingNotFoundException(ownerId);
                }
            }
            default -> throw new MeterBusinessRuleException("Invalid owner type: " + ownerType);
        }
    }

    private void validateMeterType(String type) {
        if (!"WATER".equals(type) && !"GAS".equals(type) && !"ELECTRICITY".equals(type)) {
            throw new MeterBusinessRuleException("Invalid meter type: " + type
                    + ". Allowed values: WATER, GAS, ELECTRICITY");
        }
    }

    /** BR-01: startDate cannot be in the future. */
    private void validateStartDateNotFuture(LocalDate startDate) {
        if (startDate.isAfter(LocalDate.now())) {
            throw new MeterBusinessRuleException("Start date cannot be in the future");
        }
    }

    /**
     * BR-05: eanCode required for GAS and ELECTRICITY.
     * BR-06: installationNumber required for WATER.
     * BR-07: customerNumber required for WATER on BUILDING.
     */
    private void validateConditionalFields(String type, String ownerType,
                                           String eanCode, String installationNumber, String customerNumber) {
        switch (type) {
            case "GAS", "ELECTRICITY" -> {
                if (eanCode == null || eanCode.isBlank()) {
                    throw new MeterBusinessRuleException(
                            "EAN code is required for " + type.toLowerCase() + " meters");
                }
            }
            case "WATER" -> {
                if (installationNumber == null || installationNumber.isBlank()) {
                    throw new MeterBusinessRuleException("Installation number is required for water meters");
                }
                if (OWNER_BUILDING.equals(ownerType) && (customerNumber == null || customerNumber.isBlank())) {
                    throw new MeterBusinessRuleException(
                            "Customer number is required for water meters on a building");
                }
            }
        }
    }
}
