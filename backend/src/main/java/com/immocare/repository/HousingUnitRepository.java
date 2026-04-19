package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.HousingUnit;

/**
 * Repository for HousingUnit entity.
 */
public interface HousingUnitRepository extends JpaRepository<HousingUnit, Long> {

        // ─── Building-scoped queries ──────────────────────────────────────────────

        List<HousingUnit> findByBuildingIdOrderByFloorAscUnitNumberAsc(Long buildingId);

        long countByBuildingId(Long buildingId);

        /**
         * Check if a unit number already exists in a building (case-insensitive).
         * Used by HousingUnitService.createUnit() to enforce unique unit numbers.
         */
        @Query("""
                        SELECT COUNT(u) > 0 FROM HousingUnit u
                        WHERE u.building.id = :buildingId
                        AND LOWER(u.unitNumber) = LOWER(:unitNumber)
                        """)
        boolean existsByBuildingIdAndUnitNumberIgnoreCase(
                        @Param("buildingId") Long buildingId,
                        @Param("unitNumber") String unitNumber);

        /**
         * Check if a unit number exists in a building, excluding a specific unit
         * (case-insensitive).
         * Used by HousingUnitService.updateUnit() to enforce unique unit numbers during
         * updates.
         */
        @Query("""
                        SELECT COUNT(u) > 0 FROM HousingUnit u
                        WHERE u.building.id = :buildingId
                        AND LOWER(u.unitNumber) = LOWER(:unitNumber)
                        AND u.id != :excludeUnitId
                        """)
        boolean existsByBuildingIdAndUnitNumberIgnoreCaseExcluding(
                        @Param("buildingId") Long buildingId,
                        @Param("unitNumber") String unitNumber,
                        @Param("excludeUnitId") Long excludeUnitId);

        // ─── Estate-scoped queries (Phase 2+) ────────────────────────────────────

        /**
         * All units within a building that belongs to the given estate.
         * Used by BuildingController, HousingUnitController.
         */
        @Query("""
                        SELECT u FROM HousingUnit u
                        WHERE u.building.estate.id = :estateId
                        AND u.building.id = :buildingId
                        ORDER BY u.floor ASC, u.unitNumber ASC
                        """)
        List<HousingUnit> findByEstateIdAndBuildingId(
                        @Param("estateId") UUID estateId,
                        @Param("buildingId") Long buildingId);

        /**
         * Verifies that a unit belongs to the given estate (via building → estate
         * chain).
         */
        boolean existsByBuilding_Estate_IdAndId(UUID estateId, Long unitId);

        /**
         * All units within the given estate, ordered by building, floor, and unit
         * number.
         * Used by HousingUnitService.getAllUnits().
         */
        @Query("""
                        SELECT u FROM HousingUnit u
                        WHERE u.building.estate.id = :estateId
                        ORDER BY u.building.id ASC, u.floor ASC, u.unitNumber ASC
                        """)
        List<HousingUnit> findAllByEstateIdOrdered(@Param("estateId") UUID estateId);

        // ─── Owner-based queries — used by PersonService ──────────────────────────

        /**
         * All housing units owned by the given person.
         * Used by PersonService to check referential integrity before deletion
         * and to populate PersonDTO.ownedUnits.
         */
        List<HousingUnit> findByOwnerId(Long ownerId);

        /**
         * Count units for a given owner — used in PersonDTO summary.
         */
        long countByOwnerId(Long ownerId);

        // ─── PEB / rent / delete guard ────────────────────────────────────────────

        boolean existsByBuildingId(Long buildingId);
}