package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.FireExtinguisher;

/**
 * Repository for FireExtinguisher entity.
 * UC016 Phase 6: findByBuildingEstateId added for dashboard alert computation.
 */
public interface FireExtinguisherRepository extends JpaRepository<FireExtinguisher, Long> {

    List<FireExtinguisher> findByBuildingIdOrderByIdentificationNumberAsc(Long buildingId);

    boolean existsByBuildingIdAndIdentificationNumberIgnoreCase(Long buildingId, String identificationNumber);

    @Query("SELECT COUNT(e) > 0 FROM FireExtinguisher e " +
           "WHERE e.building.id = :buildingId " +
           "AND LOWER(e.identificationNumber) = LOWER(:num) " +
           "AND e.id <> :excludeId")
    boolean existsByBuildingIdAndNumberIgnoreCaseExcluding(
        @Param("buildingId") Long buildingId,
        @Param("num") String num,
        @Param("excludeId") Long excludeId);

    /**
     * All fire extinguishers in buildings belonging to the given estate,
     * with revisions eagerly fetched (needed for alert computation without N+1).
     * Used by EstateService.getDashboard() — UC016 Phase 6.
     */
    @Query("""
            SELECT DISTINCT e FROM FireExtinguisher e
            LEFT JOIN FETCH e.revisions
            WHERE e.building.estate.id = :estateId
            """)
    List<FireExtinguisher> findByBuildingEstateId(@Param("estateId") UUID estateId);
}
