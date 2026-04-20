package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.HousingUnit;

/**
 * Repository for HousingUnit entity.
 * UC016 Phase 2+: all queries scoped to an estate.
 * UC016 Phase 6: countByBuilding_Estate_Id used by dashboard.
 */
public interface HousingUnitRepository extends JpaRepository<HousingUnit, Long> {

    long countByBuildingId(Long buildingId);

    List<HousingUnit> findByBuildingId(Long buildingId);

    boolean existsByBuilding_Estate_IdAndId(UUID estateId, Long unitId);

    /**
     * Counts all housing units across all buildings in the given estate.
     * Used by EstateService.getDashboard() — UC016 Phase 6.
     */
    long countByBuilding_Estate_Id(UUID estateId);

    @Query("""
            SELECT COUNT(u) FROM HousingUnit u
            WHERE u.building.estate.id = :estateId
            """)
    long countUnitsForEstate(@Param("estateId") UUID estateId);

    @Query("""
            SELECT h FROM HousingUnit h
            WHERE h.building.estate.id = :estateId AND h.building.id = :buildingId
            """)
    List<HousingUnit> findByEstateIdAndBuildingId(@Param("estateId") UUID estateId,
            @Param("buildingId") Long buildingId);

    @Query("""
            SELECT h FROM HousingUnit h
            WHERE h.building.estate.id = :estateId
            ORDER BY h.unitNumber
            """)
    List<HousingUnit> findAllByEstateIdOrdered(@Param("estateId") UUID estateId);

    @Query("""
            SELECT COUNT(h) > 0 FROM HousingUnit h
            WHERE h.building.id = :buildingId AND LOWER(h.unitNumber) = LOWER(:unitNumber)
            """)
    boolean existsByBuildingIdAndUnitNumberIgnoreCase(@Param("buildingId") Long buildingId,
            @Param("unitNumber") String unitNumber);

    @Query("""
            SELECT COUNT(h) > 0 FROM HousingUnit h
            WHERE h.building.id = :buildingId AND LOWER(h.unitNumber) = LOWER(:unitNumber) AND h.id != :excludeId
            """)
    boolean existsByBuildingIdAndUnitNumberIgnoreCaseExcluding(@Param("buildingId") Long buildingId,
            @Param("unitNumber") String unitNumber, @Param("excludeId") Long excludeId);

    // ─── Owner queries ───────────────────────────────────────────────────────

    List<HousingUnit> findByOwner_Id(Long ownerId);

    long countByOwner_Id(Long ownerId);
}
