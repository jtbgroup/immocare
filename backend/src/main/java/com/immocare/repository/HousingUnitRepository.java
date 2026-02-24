package com.immocare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.HousingUnit;

/**
 * Repository for HousingUnit entity.
 */
@Repository
public interface HousingUnitRepository extends JpaRepository<HousingUnit, Long> {

  /**
   * Find all units belonging to a building, ordered by floor then unit number.
   */
  List<HousingUnit> findByBuildingIdOrderByFloorAscUnitNumberAsc(Long buildingId);

  /**
   * Count units in a building — used by BuildingService to guard against
   * deletion.
   */
  long countByBuildingId(Long buildingId);

  /**
   * Check for duplicate unit number within a building, excluding a given unit ID.
   * Used when editing a unit (id != excludeId) or creating (excludeId = null via
   * separate method).
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

  /**
   * Count rooms associated with a unit (placeholder until Room entity exists).
   * Returns 0 until UC003 is implemented.
   */
  @Query("SELECT 0L")
  Long countRoomsByUnitId(@Param("unitId") Long unitId);

  // Existing methods (keep as-is) ...

  // ---- NEW: owner_id queries for UC009 ----

  /** True if any housing unit references this person as direct owner. */
  boolean existsByOwnerId(Long ownerId);

  /** List all housing units owned by a given person (direct, not inherited). */
  List<HousingUnit> findByOwnerId(Long ownerId);

  /** Find all units for a given building. */
  List<HousingUnit> findByBuildingId(Long buildingId);

  /** True if a building has any housing units (used for delete check). */
  boolean existsByBuildingId(Long buildingId);
}
