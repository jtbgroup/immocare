package com.immocare.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.BoilerServiceValidityRule;

/**
 * Repository for BoilerServiceValidityRule.
 * UC004_ESTATE_PLACEHOLDER Phase 5: all queries are now scoped to an estate.
 */
public interface BoilerServiceValidityRuleRepository extends JpaRepository<BoilerServiceValidityRule, Long> {

    /** All rules for an estate, newest valid_from first. */
    List<BoilerServiceValidityRule> findByEstateIdOrderByValidFromDesc(UUID estateId);

    /** Check uniqueness before adding a new rule within an estate. */
    boolean existsByEstateIdAndValidFrom(UUID estateId, LocalDate validFrom);

    /**
     * Finds the applicable rule for a given service date within an estate.
     * Returns the most recent rule whose valid_from is on or before the given date.
     */
    Optional<BoilerServiceValidityRule> findTopByEstateIdAndValidFromLessThanEqualOrderByValidFromDesc(
            UUID estateId, LocalDate date);
}
