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

    /**
     * Returns the next unique sequence value from a dedicated PostgreSQL sequence.
     * Safe within a single Hibernate session because nextval() is evaluated
     * immediately in the DB, independently of any pending flushes.
     */
    @Query(value = "SELECT NEXTVAL('financial_transaction_ref_seq')", nativeQuery = true)
    long nextRefSequence();

    Page<FinancialTransaction> findByImportBatchId(Long batchId, Pageable pageable);
}