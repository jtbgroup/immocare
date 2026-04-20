package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.Building;

/**
 * Repository for Building entity.
 * UC016 Phase 2+: all queries scoped to an estate.
 * UC016 Phase 6: countByEstateId used by dashboard.
 */
public interface BuildingRepository extends JpaRepository<Building, Long> {

        // ─── Estate-scoped queries ────────────────────────────────────────────────

        Page<Building> findByEstateIdOrderByNameAsc(UUID estateId, Pageable pageable);

        Page<Building> findByEstateIdAndCity(UUID estateId, String city, Pageable pageable);

        @Query("""
                        SELECT b FROM Building b
                        WHERE b.estate.id = :estateId
                          AND (LOWER(b.name)          LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(b.city)          LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Building> searchByEstate(
                        @Param("estateId") UUID estateId,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("""
                        SELECT b FROM Building b
                        WHERE b.estate.id = :estateId
                          AND LOWER(b.city) = LOWER(:city)
                          AND (LOWER(b.name)          LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :search, '%')))
                        """)
        Page<Building> searchByEstateAndCity(
                        @Param("estateId") UUID estateId,
                        @Param("city") String city,
                        @Param("search") String search,
                        Pageable pageable);

        // ─── Owner queries ───────────────────────────────────────────────────────

        List<Building> findByOwner_Id(Long ownerId);

        @Query("SELECT DISTINCT b.city FROM Building b WHERE b.estate.id = :estateId ORDER BY b.city ASC")
        List<String> findDistinctCitiesByEstateId(@Param("estateId") UUID estateId);

        boolean existsByEstateIdAndId(UUID estateId, Long buildingId);

        /** Used by EstateService.getDashboard() and deleteEstate() — Phase 6. */
        long countByEstateId(UUID estateId);
}
