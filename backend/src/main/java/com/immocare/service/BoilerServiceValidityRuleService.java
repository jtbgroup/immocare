package com.immocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BoilerValidityRuleDuplicateException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.model.dto.EstatePlatformConfigDTOs.AddBoilerServiceValidityRuleRequest;
import com.immocare.model.dto.EstatePlatformConfigDTOs.BoilerServiceValidityRuleDTO;
import com.immocare.model.entity.BoilerServiceValidityRule;
import com.immocare.model.entity.Estate;
import com.immocare.repository.BoilerServiceValidityRuleRepository;
import com.immocare.repository.EstateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for boiler service validity rules — UC016 Phase 5.
 * All operations are scoped to an estate.
 * Rules are append-only: once added, they are never modified or deleted.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoilerServiceValidityRuleService {

    private final BoilerServiceValidityRuleRepository ruleRepository;
    private final EstateRepository estateRepository;

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Returns all validity rules for the given estate, newest valid_from first.
     */
    public List<BoilerServiceValidityRuleDTO> getAllRules(UUID estateId) {
        return ruleRepository.findByEstateIdOrderByValidFromDesc(estateId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Calculates the valid_until date for a service performed on the given date,
     * using the applicable estate-scoped rule (most recent rule with valid_from <= serviceDate).
     * Falls back to 24 months if no rule is found.
     */
    public LocalDate calculateValidUntil(UUID estateId, LocalDate serviceDate) {
        int months = ruleRepository
                .findTopByEstateIdAndValidFromLessThanEqualOrderByValidFromDesc(estateId, serviceDate)
                .map(BoilerServiceValidityRule::getValidityDurationMonths)
                .orElse(24);
        return serviceDate.plusMonths(months);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Adds a new validity rule for the given estate.
     * Uniqueness is enforced per (estate_id, valid_from).
     */
    @Transactional
    public BoilerServiceValidityRuleDTO addRule(UUID estateId, AddBoilerServiceValidityRuleRequest req) {
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));

        if (ruleRepository.existsByEstateIdAndValidFrom(estateId, req.validFrom())) {
            throw new BoilerValidityRuleDuplicateException(
                    "A validity rule for this date already exists in this estate: " + req.validFrom());
        }

        BoilerServiceValidityRule rule = new BoilerServiceValidityRule();
        rule.setEstate(estate);
        rule.setValidFrom(req.validFrom());
        rule.setValidityDurationMonths(req.validityDurationMonths());
        rule.setDescription(req.description());

        return toDTO(ruleRepository.save(rule));
    }

    // ─── SEED (called by EstateService at creation) ───────────────────────────

    /**
     * Seeds the default validity rule for a newly created estate.
     * Called within the same @Transactional as EstateService.createEstate().
     */
    @Transactional
    public void seedDefaultRule(Estate estate) {
        BoilerServiceValidityRule rule = new BoilerServiceValidityRule();
        rule.setEstate(estate);
        rule.setValidFrom(LocalDate.of(1900, 1, 1));
        rule.setValidityDurationMonths(24);
        rule.setDescription("Default — 2 years (current regulation)");
        ruleRepository.save(rule);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private BoilerServiceValidityRuleDTO toDTO(BoilerServiceValidityRule r) {
        return new BoilerServiceValidityRuleDTO(
                r.getId(),
                r.getEstate().getId(),
                r.getValidFrom(),
                r.getValidityDurationMonths(),
                r.getDescription(),
                r.getCreatedAt());
    }
}
