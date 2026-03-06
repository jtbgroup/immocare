package com.immocare.repository;

import com.immocare.model.entity.AccountingMonthRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountingMonthRuleRepository extends JpaRepository<AccountingMonthRule, Long> {

    @Query("""
        SELECT r FROM AccountingMonthRule r
        WHERE r.subcategory.id = :subcategoryId
        AND (LOWER(r.counterpartyAccount) = LOWER(:counterparty) OR r.counterpartyAccount IS NULL)
        ORDER BY CASE WHEN r.counterpartyAccount IS NULL THEN 1 ELSE 0 END ASC
        """)
    List<AccountingMonthRule> findBestMatch(
        @Param("subcategoryId") Long subcategoryId,
        @Param("counterparty") String counterparty);

    Optional<AccountingMonthRule> findBySubcategoryIdAndCounterpartyAccountIgnoreCase(
        Long subcategoryId, String counterpartyAccount);

    Optional<AccountingMonthRule> findBySubcategoryIdAndCounterpartyAccountIsNull(Long subcategoryId);
}
