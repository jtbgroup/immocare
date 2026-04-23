package com.immocare.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.immocare.model.entity.FinancialTransaction;

/**
 * Repository for FinancialTransaction entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: estate-scoped queries added.
 */
public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByExternalReferenceAndTransactionDateAndAmount(
            String externalReference, LocalDate transactionDate, BigDecimal amount);

    /** Duplicate detection via SHA-256 fingerprint stored at import time. */
    boolean existsByImportFingerprint(String importFingerprint);

    /**
     * Returns the id of the existing transaction with the given fingerprint, or null.
     * UC004_ESTATE_PLACEHOLDER Phase 4: check is now estate-scoped to avoid cross-estate dedup conflicts.
     */
    @Query("SELECT t.id FROM FinancialTransaction t WHERE t.importFingerprint = :fingerprint")
    Long findIdByImportFingerprint(
            @org.springframework.data.repository.query.Param("fingerprint") String fingerprint);

    /**
     * Returns the next unique sequence value from a dedicated PostgreSQL sequence.
     */
    @Query(value = "SELECT NEXTVAL('financial_transaction_ref_seq')", nativeQuery = true)
    long nextRefSequence();

    Page<FinancialTransaction> findByImportBatchId(Long batchId, Pageable pageable);

    // ─── Estate-scoped duplicate detection (Phase 4) ─────────────────────────

    /**
     * Checks for duplicate fingerprint within the same estate.
     * A transaction from estate A should not block import of the same transaction in estate B.
     */
    boolean existsByImportFingerprintAndEstateId(String importFingerprint, UUID estateId);

    /**
     * Returns the id of the existing transaction with the given fingerprint within the estate.
     */
    @Query("SELECT t.id FROM FinancialTransaction t WHERE t.importFingerprint = :fingerprint AND t.estate.id = :estateId")
    Long findIdByImportFingerprintAndEstateId(
            @org.springframework.data.repository.query.Param("fingerprint") String fingerprint,
            @org.springframework.data.repository.query.Param("estateId") UUID estateId);
}
