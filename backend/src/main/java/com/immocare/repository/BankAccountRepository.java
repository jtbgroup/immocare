package com.immocare.repository;

import com.immocare.model.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BankAccount entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all queries are now scoped to an estate.
 */
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // ─── Estate-scoped queries (Phase 4) ─────────────────────────────────────

    List<BankAccount> findByEstateIdOrderByLabelAsc(UUID estateId);

    List<BankAccount> findByEstateIdAndIsActiveTrueOrderByLabelAsc(UUID estateId);

    boolean existsByEstateIdAndLabelIgnoreCase(UUID estateId, String label);

    boolean existsByEstateIdAndLabelIgnoreCaseAndIdNot(UUID estateId, String label, Long id);

    boolean existsByEstateIdAndAccountNumber(UUID estateId, String accountNumber);

    boolean existsByEstateIdAndAccountNumberAndIdNot(UUID estateId, String accountNumber, Long id);

    Optional<BankAccount> findByEstateIdAndAccountNumber(UUID estateId, String accountNumber);

    boolean existsByEstateIdAndId(UUID estateId, Long id);

    // ─── Legacy queries kept for non-estate-scoped internal use ──────────────

    /** Used by import services when estate context is not yet available. */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findAllByOrderByLabelAsc();

    List<BankAccount> findByIsActiveTrueOrderByLabelAsc();

    boolean existsByLabelIgnoreCase(String label);

    boolean existsByLabelIgnoreCaseAndIdNot(String label, Long id);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);
}
