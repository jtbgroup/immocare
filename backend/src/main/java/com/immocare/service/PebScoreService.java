package com.immocare.service;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.InvalidPebScoreDateException;
import com.immocare.exception.InvalidValidityPeriodException;
import com.immocare.mapper.PebScoreMapper;
import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.model.dto.PebScoreStepDTO;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.PebScore;
import com.immocare.model.entity.PebScoreHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PebScoreRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for PEB score management.
 * Implements business logic for UC004 - Manage PEB Scores (US017-US020).
 */
@Service
@Transactional(readOnly = true)
public class PebScoreService {

    private static final int EXPIRING_SOON_MONTHS = 3;

    private final PebScoreRepository pebScoreRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final PebScoreMapper pebScoreMapper;

    public PebScoreService(PebScoreRepository pebScoreRepository,
                           HousingUnitRepository housingUnitRepository,
                           PebScoreMapper pebScoreMapper) {
        this.pebScoreRepository = pebScoreRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.pebScoreMapper = pebScoreMapper;
    }

    // ─── Commands ────────────────────────────────────────────────────────────

    /**
     * Add a new PEB score record for a housing unit.
     * Implements US017 - Add PEB Score.
     * Enforces BR-UC004-01 through BR-UC004-07.
     */
    @Transactional
    public PebScoreDTO addScore(Long unitId, CreatePebScoreRequest request) {
        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new HousingUnitNotFoundException(unitId));

        // BR-UC004-03: score_date cannot be in the future (also enforced by @PastOrPresent)
        if (request.getScoreDate().isAfter(LocalDate.now())) {
            throw new InvalidPebScoreDateException("Score date cannot be in the future");
        }

        // BR-UC004-04: valid_until must be after score_date if provided
        if (request.getValidUntil() != null && !request.getValidUntil().isAfter(request.getScoreDate())) {
            throw new InvalidValidityPeriodException("Valid until must be after score date");
        }

        PebScoreHistory entity = new PebScoreHistory();
        entity.setHousingUnit(unit);
        entity.setPebScore(request.getPebScore());
        entity.setScoreDate(request.getScoreDate());
        entity.setCertificateNumber(request.getCertificateNumber());
        entity.setValidUntil(request.getValidUntil());

        PebScoreHistory saved = pebScoreRepository.save(entity);
        return enrichDTO(pebScoreMapper.toDTO(saved), saved, true);
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    /**
     * Get full history for a unit, newest first.
     * Implements US018 - View PEB Score History.
     */
    public List<PebScoreDTO> getHistory(Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
        List<PebScoreHistory> records = pebScoreRepository
                .findByHousingUnitIdOrderByScoreDateDesc(unitId);

        Long currentId = records.isEmpty() ? null : records.get(0).getId();

        return records.stream()
                .map(r -> enrichDTO(pebScoreMapper.toDTO(r), r, r.getId().equals(currentId)))
                .collect(Collectors.toList());
    }

    /**
     * Get the current (most recent) score for a unit.
     * Implements US018 - current score badge.
     */
    public Optional<PebScoreDTO> getCurrentScore(Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
        return pebScoreRepository.findFirstByHousingUnitIdOrderByScoreDateDesc(unitId)
                .map(r -> enrichDTO(pebScoreMapper.toDTO(r), r, true));
    }

    /**
     * Get improvement summary for a unit.
     * Implements US020 - Track PEB Score Improvements.
     */
    public Optional<PebImprovementDTO> getImprovementSummary(Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
        List<PebScoreHistory> records = pebScoreRepository
                .findByHousingUnitIdOrderByScoreDateDesc(unitId);

        if (records.size() < 1) {
            return Optional.empty();
        }

        PebScoreHistory current = records.get(0);
        PebScoreHistory first = records.get(records.size() - 1);

        PebImprovementDTO dto = new PebImprovementDTO();
        dto.setFirstScore(first.getPebScore());
        dto.setFirstScoreDate(first.getScoreDate());
        dto.setCurrentScore(current.getPebScore());
        dto.setCurrentScoreDate(current.getScoreDate());
        dto.setGradesImproved(first.getPebScore().gradesTo(current.getPebScore()));
        dto.setYearsCovered((int) ChronoUnit.YEARS.between(first.getScoreDate(), current.getScoreDate()));

        // Build step history (chronological order, oldest → newest)
        List<PebScoreStepDTO> steps = new ArrayList<>();
        for (int i = records.size() - 1; i > 0; i--) {
            PebScoreHistory from = records.get(i);
            PebScoreHistory to = records.get(i - 1);
            int diff = from.getPebScore().gradesTo(to.getPebScore());
            String direction = diff > 0 ? "IMPROVED" : (diff < 0 ? "DEGRADED" : "UNCHANGED");
            steps.add(new PebScoreStepDTO(from.getPebScore(), to.getPebScore(), direction, to.getScoreDate()));
        }
        dto.setHistory(steps);

        return Optional.of(dto);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private PebScoreDTO enrichDTO(PebScoreDTO dto, PebScoreHistory entity, boolean isCurrent) {
        LocalDate today = LocalDate.now();
        LocalDate validUntil = entity.getValidUntil();

        // Compute status
        if (validUntil != null && validUntil.isBefore(today)) {
            dto.setStatus("EXPIRED");
        } else if (isCurrent) {
            dto.setStatus("CURRENT");
        } else {
            dto.setStatus("HISTORICAL");
        }

        // Compute expiry warning (US019)
        if (validUntil == null) {
            dto.setExpiryWarning("NO_DATE");
        } else if (validUntil.isBefore(today)) {
            dto.setExpiryWarning("EXPIRED");
        } else if (validUntil.isBefore(today.plusMonths(EXPIRING_SOON_MONTHS))) {
            dto.setExpiryWarning("EXPIRING_SOON");
        } else {
            dto.setExpiryWarning("VALID");
        }

        return dto;
    }
}
