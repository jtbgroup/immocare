package com.immocare.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.immocare.model.entity.FinancialTransaction;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByExternalReferenceAndTransactionDateAndAmount(
            String externalReference, LocalDate transactionDate, BigDecimal amount);

    /** Duplicate detection via SHA-256 fingerprint stored at import time. */
    boolean existsByImportFingerprint(String importFingerprint);

    /**
     * Returns the next unique sequence value from a dedicated PostgreSQL sequence.
     */
    @Query(value = "SELECT NEXTVAL('financial_transaction_ref_seq')", nativeQuery = true)
    long nextRefSequence();

    Page<FinancialTransaction> findByImportBatchId(Long batchId, Pageable pageable);
}
