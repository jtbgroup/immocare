package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.FireExtinguisher;

/**
 * Repository for FireExtinguisher entity.
 * UC004_ESTATE_PLACEHOLDER Phase 6: findByBuildingEstateId added for dashboard alert computation.
 */
public interface FireExtinguisherRepository extends JpaRepository<FireExtinguisher, Long> {

    List<FireExtinguisher> findByBuildingIdOrderByIdentificationNumberAsc(Long buildingId);

    List<FireExtinguisher> findByUnitIdOrderByIdentificationNumberAsc(Long unitId);

    long countByUnitId(Long unitId);

    @Query("""
            SELECT e FROM FireExtinguisher e
            LEFT JOIN e.building b
            LEFT JOIN e.unit u
            WHERE b.id = :buildingId OR u.building.id = :buildingId
            ORDER BY e.identificationNumber
            """)
    List<FireExtinguisher> findByBuildingContextIdOrderByIdentificationNumberAsc(
            @Param("buildingId") Long buildingId);

    boolean existsByBuildingIdAndIdentificationNumberIgnoreCase(Long buildingId, String identificationNumber);

    boolean existsByUnitIdAndIdentificationNumberIgnoreCase(Long unitId, String identificationNumber);

    @Query("SELECT COUNT(e) > 0 FROM FireExtinguisher e " +
           "WHERE e.building.id = :buildingId " +
           "AND LOWER(e.identificationNumber) = LOWER(:num) " +
           "AND e.id <> :excludeId")
    boolean existsByBuildingIdAndNumberIgnoreCaseExcluding(
        @Param("buildingId") Long buildingId,
        @Param("num") String num,
        @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(e) > 0 FROM FireExtinguisher e
            WHERE e.unit.id = :unitId
              AND LOWER(e.identificationNumber) = LOWER(:num)
              AND e.id <> :excludeId
            """)
    boolean existsByUnitIdAndNumberIgnoreCaseExcluding(
            @Param("unitId") Long unitId,
            @Param("num") String num,
            @Param("excludeId") Long excludeId);

    /**
     * All fire extinguishers in buildings belonging to the given estate,
     * with revisions eagerly fetched (needed for alert computation without N+1).
     * Used by EstateService.getDashboard() — UC004_ESTATE_PLACEHOLDER Phase 6.
     */
    @Query("""
            SELECT DISTINCT e FROM FireExtinguisher e
            LEFT JOIN FETCH e.revisions
            LEFT JOIN e.building b
            LEFT JOIN e.unit u
            WHERE b.estate.id = :estateId
               OR u.building.estate.id = :estateId
            """)
    List<FireExtinguisher> findByBuildingEstateId(@Param("estateId") UUID estateId);
}
