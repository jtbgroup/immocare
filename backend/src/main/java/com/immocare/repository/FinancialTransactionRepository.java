package com.immocare.repository;

import com.immocare.model.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FinancialTransactionRepository
    extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByExternalReferenceAndTransactionDateAndAmount(
        String externalReference, LocalDate transactionDate, BigDecimal amount);

    @Query(value = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(reference FROM 10) AS INTEGER)), 0) + 1
        FROM financial_transaction
        WHERE reference LIKE :prefix
        """, nativeQuery = true)
    int nextSequenceForYear(@Param("prefix") String prefix);

    Page<FinancialTransaction> findByImportBatchId(Long batchId, Pageable pageable);
}
