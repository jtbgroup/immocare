package com.immocare.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.Estate;

/**
 * Repository for {@link Estate}.
 * UC016 — Manage Estates.
 */
@Repository
public interface EstateRepository extends JpaRepository<Estate, UUID> {

    /** BR-UC016-01: case-insensitive name uniqueness check on create. */
    boolean existsByNameIgnoreCase(String name);

    /** BR-UC016-01: case-insensitive name uniqueness check on update (excludes self). */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** All estates ordered by name — used by getAllEstates() and getMyEstates() for PLATFORM_ADMIN. */
    Page<Estate> findAllByOrderByNameAsc(Pageable pageable);

    /**
     * Case-insensitive partial name search — used by admin estate list (US095).
     * Uses ILIKE for PostgreSQL; falls back to LOWER + LIKE for portability.
     */
    @Query("""
            SELECT e FROM Estate e
            WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY e.name ASC
            """)
    Page<Estate> searchByName(@Param("term") String term, Pageable pageable);
}
