package com.immocare.repository;

import com.immocare.model.entity.WaterMeterHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for water meter history.
 * UC006 - Manage Water Meters (US026-US030).
 */
@Repository
public interface WaterMeterHistoryRepository extends JpaRepository<WaterMeterHistory, Long> {

    /**
     * Returns the current active meter for a unit (removal_date IS NULL).
     * BR-UC006-02 / BR-UC006-04: only one active meter per unit.
     */
    Optional<WaterMeterHistory> findByHousingUnitIdAndRemovalDateIsNull(Long housingUnitId);

    /**
     * Full meter history for a unit, newest installation first.
     * US028 AC2: sorted by installation_date DESC.
     */
    List<WaterMeterHistory> findByHousingUnitIdOrderByInstallationDateDesc(Long housingUnitId);

    /**
     * Used to guard housing unit deletion.
     * UC002 BR-UC002-07: cannot delete unit with meter data.
     */
    boolean existsByHousingUnitId(Long housingUnitId);
}
