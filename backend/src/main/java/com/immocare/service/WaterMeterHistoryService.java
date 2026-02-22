package com.immocare.service;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.WaterMeterHistoryMapper;
import com.immocare.model.dto.AssignMeterRequest;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.dto.WaterMeterHistoryDTO;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.WaterMeterHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.WaterMeterHistoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for UC006 — Manage Water Meters (US026 → US030).
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>BR-01: Append-only — no delete/update on existing records.</li>
 *   <li>BR-02: Active meter = removal_date IS NULL.</li>
 *   <li>BR-03: On replace, old meter removal_date = new installation_date.</li>
 *   <li>BR-04: Only one active meter per unit.</li>
 *   <li>BR-05: Installation date cannot be in the future (enforced by DTO validation).</li>
 *   <li>BR-06: removal_date >= installation_date (enforced by DB + service).</li>
 *   <li>BR-09: New installation date must be >= current meter installation date.</li>
 * </ul>
 */
@Service
@Transactional
public class WaterMeterHistoryService {

    private final WaterMeterHistoryRepository meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final WaterMeterHistoryMapper mapper;

    public WaterMeterHistoryService(WaterMeterHistoryRepository meterRepository,
                                    HousingUnitRepository housingUnitRepository,
                                    WaterMeterHistoryMapper mapper) {
        this.meterRepository = meterRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.mapper = mapper;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the current active meter for a unit, if any.
     * US026 AC1, US028 AC1 — Water Meter section on unit details.
     */
    @Transactional(readOnly = true)
    public Optional<WaterMeterHistoryDTO> getActiveMeter(Long unitId) {
        requireUnitExists(unitId);
        return meterRepository
                .findByHousingUnitIdAndRemovalDateIsNull(unitId)
                .map(mapper::toDTO);
    }

    /**
     * Returns full meter history for a unit, newest installation first.
     * US028 AC2 — sorted by installation_date DESC.
     */
    @Transactional(readOnly = true)
    public List<WaterMeterHistoryDTO> getMeterHistory(Long unitId) {
        requireUnitExists(unitId);
        return meterRepository
                .findByHousingUnitIdOrderByInstallationDateDesc(unitId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Assigns the first water meter to a unit.
     * US026 — unit must not have any active meter.
     *
     * @throws IllegalStateException if unit already has an active meter.
     */
    public WaterMeterHistoryDTO assignMeter(Long unitId, AssignMeterRequest request) {
        HousingUnit unit = requireUnitExists(unitId);

        // BR-04: only one active meter per unit
        meterRepository.findByHousingUnitIdAndRemovalDateIsNull(unitId).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Unit already has an active water meter: " + existing.getMeterNumber()
                            + ". Use the replace endpoint to change it.");
        });

        WaterMeterHistory meter = new WaterMeterHistory(
                unit,
                request.getMeterNumber().trim(),
                request.getMeterLocation() != null ? request.getMeterLocation().trim() : null,
                request.getInstallationDate()
        );
        return mapper.toDTO(meterRepository.save(meter));
    }

    /**
     * Replaces the current active meter with a new one.
     * US027 — atomically closes old meter and creates new.
     *
     * @throws IllegalStateException if no active meter exists.
     * @throws IllegalArgumentException if the new installation date is before the current meter's date.
     */
    public WaterMeterHistoryDTO replaceMeter(Long unitId, ReplaceMeterRequest request) {
        HousingUnit unit = requireUnitExists(unitId);

        WaterMeterHistory current = meterRepository
                .findByHousingUnitIdAndRemovalDateIsNull(unitId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active water meter found for unit " + unitId + ". Use assign endpoint."));

        // BR-09: new installation date must be >= current meter installation date
        if (request.getNewInstallationDate().isBefore(current.getInstallationDate())) {
            throw new IllegalArgumentException(
                    "New installation date cannot be before current meter installation date ("
                            + current.getInstallationDate() + ").");
        }

        // BR-03: close old meter — removal_date = new installation_date
        current.setRemovalDate(request.getNewInstallationDate());
        meterRepository.save(current);

        // Create new active meter
        WaterMeterHistory newMeter = new WaterMeterHistory(
                unit,
                request.getNewMeterNumber().trim(),
                request.getNewMeterLocation() != null ? request.getNewMeterLocation().trim() : null,
                request.getNewInstallationDate()
        );
        return mapper.toDTO(meterRepository.save(newMeter));
    }

    /**
     * Removes the active meter without replacement.
     * US029 — unit will have no active meter afterwards.
     *
     * @throws IllegalStateException if no active meter exists.
     * @throws IllegalArgumentException if removal_date < installation_date.
     */
    public WaterMeterHistoryDTO removeMeter(Long unitId, RemoveMeterRequest request) {
        requireUnitExists(unitId);

        WaterMeterHistory current = meterRepository
                .findByHousingUnitIdAndRemovalDateIsNull(unitId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active water meter found for unit " + unitId + "."));

        // BR-06: removal_date >= installation_date
        if (request.getRemovalDate().isBefore(current.getInstallationDate())) {
            throw new IllegalArgumentException(
                    "Removal date cannot be before installation date ("
                            + current.getInstallationDate() + ").");
        }

        current.setRemovalDate(request.getRemovalDate());
        return mapper.toDTO(meterRepository.save(current));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private HousingUnit requireUnitExists(Long unitId) {
        return housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new HousingUnitNotFoundException(unitId));
    }
}
