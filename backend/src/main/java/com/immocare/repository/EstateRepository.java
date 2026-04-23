package com.immocare.repository;

import java.util.Optional;
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
 * UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 1+).
 */
@Repository
public interface EstateRepository extends JpaRepository<Estate, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @Query("""
            SELECT e FROM Estate e
            LEFT JOIN FETCH e.createdBy
            WHERE e.id = :id
            """)
    Optional<Estate> findById(@Param("id") UUID id);

    @Query("""
            SELECT e FROM Estate e
            LEFT JOIN FETCH e.createdBy
            ORDER BY e.name ASC
            """)
    Page<Estate> findAllByOrderByNameAsc(Pageable pageable);

    @Query("""
            SELECT e FROM Estate e
            LEFT JOIN FETCH e.createdBy
            WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY e.name ASC
            """)
    Page<Estate> searchByName(@Param("term") String term, Pageable pageable);
}
