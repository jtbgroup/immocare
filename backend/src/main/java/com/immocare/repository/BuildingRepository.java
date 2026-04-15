package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.Building;

/**
 * Repository interface for Building entity.
 * UC016 Phase 2: all queries are now scoped to an estate.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    // ─── Estate-scoped queries (Phase 2) ─────────────────────────────────────

    /**
     * Returns all buildings belonging to an estate, ordered by name.
     */
    Page<Building> findByEstateIdOrderByNameAsc(UUID estateId, Pageable pageable);

    /**
     * Searches buildings within an estate by name, street address or city (case-insensitive).
     */
    @Query("""
            SELECT b FROM Building b
            WHERE b.estate.id = :estateId
            AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(b.city) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Building> searchByEstate(
            @Param("estateId") UUID estateId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Searches buildings within an estate filtered by city.
     */
    @Query("""
            SELECT b FROM Building b
            WHERE b.estate.id = :estateId
            AND b.city = :city
            AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Building> searchByEstateAndCity(
            @Param("estateId") UUID estateId,
            @Param("city") String city,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Returns all buildings of an estate filtered by city.
     */
    Page<Building> findByEstateIdAndCity(UUID estateId, String city, Pageable pageable);

    /**
     * Returns all distinct cities from buildings of a given estate.
     */
    @Query("SELECT DISTINCT b.city FROM Building b WHERE b.estate.id = :estateId ORDER BY b.city")
    List<String> findDistinctCitiesByEstateId(@Param("estateId") UUID estateId);

    /**
     * Checks that a building exists and belongs to the given estate.
     * Used for ownership verification before any operation.
     */
    boolean existsByEstateIdAndId(UUID estateId, Long buildingId);

    /**
     * Counts all buildings in a given estate.
     * Used by EstateService.getDashboard() (Phase 2).
     */
    long countByEstateId(UUID estateId);

    // ─── Legacy queries kept for non-estate-scoped internal use ──────────────

    /**
     * True if any building references this person as owner.
     * UC009 — used by PersonService.
     */
    boolean existsByOwnerId(Long ownerId);

    /**
     * List all buildings owned by a given person.
     * UC009 — used by PersonService.
     */
    List<Building> findByOwnerId(Long ownerId);
}
