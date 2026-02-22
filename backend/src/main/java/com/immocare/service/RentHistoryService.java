package com.immocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.RentHistoryMapper;
import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.RentHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.RentHistoryRepository;

/**
 * Business logic for UC005 — Manage Rents (US021 → US025).
 * Full CRUD with automatic recalculation of adjacent effectiveTo dates.
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

    @Transactional(readOnly = true)
    public Optional<RentHistoryDTO> getCurrentRent(Long unitId) {
        requireUnitExists(unitId);
        return rentHistoryRepository
                .findByHousingUnitIdAndEffectiveToIsNull(unitId)
                .map(rentHistoryMapper::toDTO);
    }

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
    // Create — add a new rent record
    // -------------------------------------------------------------------------

    /**
     * Adds a new rent record.
     * - If it becomes the most recent, sets effectiveTo = null and closes the
     * previous.
     * - If inserted in the middle, sets effectiveTo = effectiveFrom of the next
     * record - 1 day,
     * and recalculates the effectiveTo of the previous record.
     */
    public RentHistoryDTO addRent(Long unitId, SetRentRequest request) {
        requireUnitExists(unitId);
        validateEffectiveFrom(request.effectiveFrom());

        HousingUnit unit = findUnit(unitId);

        // All records sorted DESC
        List<RentHistory> all = rentHistoryRepository
                .findByHousingUnitIdOrderByEffectiveFromDesc(unitId);

        // Find the next record (more recent than the new one)
        RentHistory next = all.stream()
                .filter(r -> r.getEffectiveFrom().isAfter(request.effectiveFrom()))
                .reduce((a, b) -> a.getEffectiveFrom().isBefore(b.getEffectiveFrom()) ? a : b)
                .orElse(null);

        // Find the previous record (older than the new one)
        RentHistory prev = all.stream()
                .filter(r -> !r.getEffectiveFrom().isAfter(request.effectiveFrom()))
                .findFirst() // list is DESC so first = most recent before new date
                .orElse(null);

        // Compute effectiveTo for the new record
        LocalDate newEffectiveTo = (next != null)
                ? next.getEffectiveFrom().minusDays(1)
                : null; // new record becomes the current one

        // If new record has no next → it is the most recent → close the previous
        if (next == null && prev != null) {
            prev.setEffectiveTo(request.effectiveFrom().minusDays(1));
            rentHistoryRepository.save(prev);
        }

        RentHistory newRecord = new RentHistory(
                unit,
                request.monthlyRent(),
                request.effectiveFrom(),
                newEffectiveTo,
                request.notes());
        return rentHistoryMapper.toDTO(rentHistoryRepository.save(newRecord));
    }

    // -------------------------------------------------------------------------
    // Update — edit an existing record
    // -------------------------------------------------------------------------

    /**
     * Updates an existing rent record.
     * Recalculates effectiveTo of the previous record and effectiveTo of this
     * record.
     */
    public RentHistoryDTO updateRent(Long unitId, Long rentId, SetRentRequest request) {
        requireUnitExists(unitId);
        validateEffectiveFrom(request.effectiveFrom());

        RentHistory record = rentHistoryRepository.findById(rentId)
                .filter(r -> r.getHousingUnit().getId().equals(unitId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rent record " + rentId + " not found for unit " + unitId));

        // Reload sorted list excluding this record to find neighbours
        List<RentHistory> others = rentHistoryRepository
                .findByHousingUnitIdOrderByEffectiveFromDesc(unitId)
                .stream()
                .filter(r -> !r.getId().equals(rentId))
                .toList();

        // Next record after the new effectiveFrom
        RentHistory next = others.stream()
                .filter(r -> r.getEffectiveFrom().isAfter(request.effectiveFrom()))
                .reduce((a, b) -> a.getEffectiveFrom().isBefore(b.getEffectiveFrom()) ? a : b)
                .orElse(null);

        // Previous record before the new effectiveFrom
        RentHistory prev = others.stream()
                .filter(r -> !r.getEffectiveFrom().isAfter(request.effectiveFrom()))
                .findFirst()
                .orElse(null);

        // Recalculate effectiveTo of the previous record
        if (prev != null) {
            prev.setEffectiveTo(request.effectiveFrom().minusDays(1));
            rentHistoryRepository.save(prev);
        }

        // Recalculate effectiveTo of this record
        LocalDate newEffectiveTo = (next != null)
                ? next.getEffectiveFrom().minusDays(1)
                : null;

        record.setMonthlyRent(request.monthlyRent());
        record.setEffectiveFrom(request.effectiveFrom());
        record.setEffectiveTo(newEffectiveTo);
        record.setNotes(request.notes());

        return rentHistoryMapper.toDTO(rentHistoryRepository.save(record));
    }

    // -------------------------------------------------------------------------
    // Delete — remove a record and recalculate adjacent
    // -------------------------------------------------------------------------

    /**
     * Deletes a rent record.
     * The previous record (older) inherits the effectiveTo of the deleted one,
     * or null if the deleted record was the most recent.
     */
    public void deleteRent(Long unitId, Long rentId) {
        requireUnitExists(unitId);

        RentHistory record = rentHistoryRepository.findById(rentId)
                .filter(r -> r.getHousingUnit().getId().equals(unitId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rent record " + rentId + " not found for unit " + unitId));

        List<RentHistory> all = rentHistoryRepository
                .findByHousingUnitIdOrderByEffectiveFromDesc(unitId);

        // Find the previous record (older than the deleted one)
        RentHistory prev = all.stream()
                .filter(r -> r.getEffectiveFrom().isBefore(record.getEffectiveFrom()))
                .findFirst() // list is DESC so first = most recent before deleted
                .orElse(null);

        // Previous record inherits the effectiveTo of the deleted record
        // (null if deleted was the most recent → prev becomes current)
        if (prev != null) {
            prev.setEffectiveTo(record.getEffectiveTo());
            rentHistoryRepository.save(prev);
        }

        rentHistoryRepository.delete(record);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateEffectiveFrom(LocalDate effectiveFrom) {
        LocalDate maxFuture = LocalDate.now().plusYears(MAX_FUTURE_YEARS);
        if (effectiveFrom.isAfter(maxFuture)) {
            throw new IllegalArgumentException(
                    "Effective from date cannot be more than 1 year in the future");
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