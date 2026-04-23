package com.immocare.repository;

import com.immocare.model.entity.ImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for ImportBatch entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: estate-scoped queries added.
 */
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    /** All import batches for a given estate, newest first. */
    Page<ImportBatch> findByEstateIdOrderByImportedAtDesc(UUID estateId, Pageable pageable);
}
