package com.immocare.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.Boiler;

/**
 * Repository for UC011 — Manage Boilers.
 * UC016 Phase 6: findActiveByEstateId added for dashboard alert computation.
 */
public interface BoilerRepository extends JpaRepository<Boiler, Long> {

    /** All boilers for a given owner (housing unit or building), newest first. */
    List<Boiler> findByOwnerTypeAndOwnerIdOrderByInstallationDateDesc(String ownerType, Long ownerId);

    /** Boilers whose next service date is on or before the given date (for alerts). */
    @Query("SELECT b FROM Boiler b WHERE b.nextServiceDate IS NOT NULL AND b.nextServiceDate <= :threshold")
    List<Boiler> findBoilersWithServiceDueBefore(@Param("threshold") LocalDate threshold);

    /**
     * All active (non-removed) boilers across all buildings in the given estate.
     * Used by EstateService.getDashboard() — UC016 Phase 6.
     */
    @Query("""
            SELECT b FROM Boiler b
            WHERE b.ownerType = 'HOUSING_UNIT'
              AND EXISTS (
                  SELECT 1 FROM HousingUnit u
                  WHERE u.id = b.ownerId
                    AND u.building.estate.id = :estateId
              )
            UNION
            SELECT b FROM Boiler b
            WHERE b.ownerType = 'BUILDING'
              AND EXISTS (
                  SELECT 1 FROM Building bld
                  WHERE bld.id = b.ownerId
                    AND bld.estate.id = :estateId
              )
            """)
    List<Boiler> findActiveByEstateId(@Param("estateId") UUID estateId);

    /** Count boilers whose next service date is within the warning window for a given estate. */
    @Query("""
            SELECT COUNT(b) FROM Boiler b
            WHERE b.nextServiceDate IS NOT NULL
              AND b.nextServiceDate <= :threshold
              AND (
                (b.ownerType = 'HOUSING_UNIT' AND EXISTS (
                    SELECT 1 FROM HousingUnit u
                    WHERE u.id = b.ownerId AND u.building.estate.id = :estateId
                ))
                OR
                (b.ownerType = 'BUILDING' AND EXISTS (
                    SELECT 1 FROM Building bld
                    WHERE bld.id = b.ownerId AND bld.estate.id = :estateId
                ))
              )
            """)
    long countServiceAlertsForEstate(
            @Param("estateId") UUID estateId,
            @Param("threshold") LocalDate threshold);
}
