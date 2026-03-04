package com.immocare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.FireExtinguisher;

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
}
