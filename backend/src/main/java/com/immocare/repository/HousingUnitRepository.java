package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.HousingUnit;

/**
 * Repository for HousingUnit entity.
 * UC016 Phase 2: estate-scoped queries added via building join.
 */
@Repository
public interface HousingUnitRepository extends JpaRepository<HousingUnit, Long> {

    // ─── Estate-scoped queries (Phase 2) ─────────────────────────────────────

    /**
     * Find all units belonging to a specific building within a given estate,
     * ordered by floor then unit number.
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
     * Checks that a housing unit exists and belongs to a building in the given estate.
     */
    boolean existsByBuilding_Estate_IdAndId(UUID estateId, Long unitId);

    /**
     * Counts all housing units in a given estate (via building join).
     * Used by EstateService.getDashboard() (Phase 2).
     */
    long countByBuilding_Estate_Id(UUID estateId);

    /**
     * Find all units across all buildings of a given estate,
     * ordered by building id, floor, and unit number.
     */
    @Query("""
            SELECT u FROM HousingUnit u
            WHERE u.building.estate.id = :estateId
            ORDER BY u.building.id ASC, u.floor ASC, u.unitNumber ASC
            """)
    List<HousingUnit> findAllByEstateIdOrdered(@Param("estateId") UUID estateId);

    // ─── Existing queries (non-estate-scoped, kept for internal use) ──────────

    /**
     * Find all units belonging to a building, ordered by floor then unit number.
     */
    List<HousingUnit> findByBuildingIdOrderByFloorAscUnitNumberAsc(Long buildingId);

    /**
     * Count units in a building — used by BuildingService to guard against deletion.
     */
    long countByBuildingId(Long buildingId);

    /**
     * Check for duplicate unit number within a building, excluding a given unit ID.
     */
    @Query("""
            SELECT COUNT(u) > 0
            FROM HousingUnit u
            WHERE u.building.id = :buildingId
              AND LOWER(u.unitNumber) = LOWER(:unitNumber)
              AND u.id <> :excludeId
            """)
    boolean existsByBuildingIdAndUnitNumberIgnoreCaseExcluding(
            @Param("buildingId") Long buildingId,
            @Param("unitNumber") String unitNumber,
            @Param("excludeId") Long excludeId);

    /**
     * Check for duplicate unit number within a building (used during creation).
     */
    boolean existsByBuildingIdAndUnitNumberIgnoreCase(Long buildingId, String unitNumber);

    /** True if any housing unit references this person as direct owner. */
    boolean existsByOwnerId(Long ownerId);

    /** List all housing units owned by a given person (direct, not inherited). */
    List<HousingUnit> findByOwnerId(Long ownerId);

    /** Find all units for a given building. */
    List<HousingUnit> findByBuildingId(Long buildingId);

    /** True if a building has any housing units (used for delete check). */
    boolean existsByBuildingId(Long buildingId);

    List<HousingUnit> findAllByOrderByBuildingIdAscFloorAscUnitNumberAsc();
}
