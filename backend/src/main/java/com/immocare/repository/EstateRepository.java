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
 * UC016 — Manage Estates (Phase 1).
 */
@Repository
public interface EstateRepository extends JpaRepository<Estate, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Page<Estate> findAllByOrderByNameAsc(Pageable pageable);

    @Query("SELECT e FROM Estate e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY e.name ASC")
    Page<Estate> searchByName(@Param("search") String search, Pageable pageable);
}
