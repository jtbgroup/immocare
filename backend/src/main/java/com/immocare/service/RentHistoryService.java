package com.immocare.service;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.RentHistoryMapper;
import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.RentHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.RentHistoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for UC005 — Manage Rents (US021 → US025).
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>BR-01: Append-only — no delete/update on existing records</li>
 *   <li>BR-02: Only one rent with effective_to = NULL per unit</li>
 *   <li>BR-03: Auto-closure: old effective_to = new effectiveFrom - 1 day</li>
 *   <li>BR-05: monthly_rent > 0 (enforced by DTO validation)</li>
 *   <li>BR-06: effectiveFrom max 1 year in future</li>
 *   <li>BR-08: new effectiveFrom >= current effectiveFrom</li>
 * </ul>
 */
@Service
@Transactional
public class RentHistoryService {

    private static final int MAX_FUTURE_YEARS = 1;

    private final RentHistoryRepository rentHistoryRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final RentHistoryMapper rentHistoryMapper;

    public RentHistoryService(RentHistoryRepository rentHistoryRepository,
                              HousingUnitRepository housingUnitRepository,
                              RentHistoryMapper rentHistoryMapper) {
        this.rentHistoryRepository = rentHistoryRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.rentHistoryMapper = rentHistoryMapper;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the current active rent for a unit, if any (US023 AC1).
     */
    @Transactional(readOnly = true)
    public Optional<RentHistoryDTO> getCurrentRent(Long unitId) {
        requireUnitExists(unitId);
        return rentHistoryRepository
                .findByHousingUnitIdAndEffectiveToIsNull(unitId)
                .map(rentHistoryMapper::toDTO);
    }

    /**
     * Returns the full rent history sorted by effectiveFrom DESC (US023 AC2).
     */
    @Transactional(readOnly = true)
    public List<RentHistoryDTO> getRentHistory(Long unitId) {
        requireUnitExists(unitId);
        return rentHistoryRepository
                .findByHousingUnitIdOrderByEffectiveFromDesc(unitId)
                .stream()
                .map(rentHistoryMapper::toDTO)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Write — unified entry point (service detects first vs update)
    // -------------------------------------------------------------------------

    /**
     * Sets or updates the rent for a unit.
     *
     * <ul>
     *   <li>If no rent exists yet → {@link #setInitialRent(Long, SetRentRequest)} (US021)</li>
     *   <li>If a current rent exists → {@link #updateRent(Long, SetRentRequest)} (US022)</li>
     * </ul>
     */
    public RentHistoryDTO setOrUpdateRent(Long unitId, SetRentRequest request) {
        if (rentHistoryRepository.existsByHousingUnitId(unitId)) {
            return updateRent(unitId, request);
        }
        return setInitialRent(unitId, request);
    }

    // -------------------------------------------------------------------------
    // Internal flows
    // -------------------------------------------------------------------------

    /**
     * US021 — Sets the first rent on a unit with no prior rent records.
     */
    private RentHistoryDTO setInitialRent(Long unitId, SetRentRequest request) {
        validateEffectiveFrom(request.effectiveFrom(), null);

        HousingUnit unit = findUnit(unitId);
        RentHistory rent = new RentHistory(
                unit,
                request.monthlyRent(),
                request.effectiveFrom(),
                null,  // current rent
                request.notes()
        );
        return rentHistoryMapper.toDTO(rentHistoryRepository.save(rent));
    }

    /**
     * US022 — Updates the rent by closing the current record and creating a new one.
     * BR-03: effectiveTo of old record = new effectiveFrom - 1 day.
     * BR-08: new effectiveFrom must be >= current effectiveFrom.
     */
    private RentHistoryDTO updateRent(Long unitId, SetRentRequest request) {
        RentHistory current = rentHistoryRepository
                .findByHousingUnitIdAndEffectiveToIsNull(unitId)
                .orElseThrow(() -> new IllegalStateException(
                        "No current rent found for unit " + unitId));

        validateEffectiveFrom(request.effectiveFrom(), current.getEffectiveFrom());

        // BR-03: close the current rent
        current.setEffectiveTo(request.effectiveFrom().minusDays(1));
        rentHistoryRepository.save(current);

        // Create the new rent record
        RentHistory newRent = new RentHistory(
                current.getHousingUnit(),
                request.monthlyRent(),
                request.effectiveFrom(),
                null,  // new current rent
                request.notes()
        );
        return rentHistoryMapper.toDTO(rentHistoryRepository.save(newRent));
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    /**
     * BR-06: effectiveFrom cannot be more than 1 year in the future.
     * BR-08: effectiveFrom must be >= currentEffectiveFrom (when updating).
     */
    private void validateEffectiveFrom(LocalDate effectiveFrom, LocalDate currentEffectiveFrom) {
        LocalDate maxFuture = LocalDate.now().plusYears(MAX_FUTURE_YEARS);
        if (effectiveFrom.isAfter(maxFuture)) {
            throw new IllegalArgumentException(
                    "Effective from date cannot be more than 1 year in the future");
        }

        if (currentEffectiveFrom != null && effectiveFrom.isBefore(currentEffectiveFrom)) {
            throw new IllegalArgumentException(
                    "Cannot backdate new rent before current period start ("
                    + currentEffectiveFrom + ")");
        }
    }

    private HousingUnit findUnit(Long unitId) {
        return housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new HousingUnitNotFoundException(unitId));
    }

    private void requireUnitExists(Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
    }
}
