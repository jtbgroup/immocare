package com.immocare.repository;

import com.immocare.model.entity.Room;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Room entity.
 * UC003 - Manage Rooms.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

  /**
   * Find all rooms belonging to a housing unit, ordered by room type.
   */
  List<Room> findByHousingUnitIdOrderByRoomTypeAsc(Long housingUnitId);

  /**
   * Count all rooms belonging to a housing unit.
   */
  long countByHousingUnitId(Long housingUnitId);

  /**
   * Compute the sum of all room surfaces for a housing unit.
   * Returns null if there are no rooms.
   */
  @Query("SELECT COALESCE(SUM(r.approximateSurface), 0) FROM Room r WHERE r.housingUnit.id = :unitId")
  BigDecimal sumApproximateSurfaceByHousingUnitId(@Param("unitId") Long unitId);

  /**
   * Delete all rooms belonging to a housing unit.
   */
  void deleteAllByHousingUnitId(Long housingUnitId);
}
