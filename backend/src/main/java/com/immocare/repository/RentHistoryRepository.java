package com.immocare.repository;

import com.immocare.model.entity.RentHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link RentHistory}.
 * All queries are append-safe — no delete or update methods are exposed.
 */
public interface RentHistoryRepository extends JpaRepository<RentHistory, Long> {

    /**
     * Returns the current active rent for a unit (effectiveTo = NULL).
     */
    Optional<RentHistory> findByHousingUnitIdAndEffectiveToIsNull(Long housingUnitId);

    /**
     * Returns the full rent history for a unit, newest first.
     */
    List<RentHistory> findByHousingUnitIdOrderByEffectiveFromDesc(Long housingUnitId);

    /**
     * Checks whether any rent record exists for a unit.
     */
    boolean existsByHousingUnitId(Long housingUnitId);
}
