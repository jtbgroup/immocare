package com.immocare.repository.spec;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.TransactionAssetLink;
import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionStatus;

import jakarta.persistence.criteria.Subquery;

public class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<FinancialTransaction> withDirection(TransactionDirection d) {
        return (root, query, cb) -> cb.equal(root.get("direction"), d);
    }

    public static Specification<FinancialTransaction> withDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), from);
    }

    public static Specification<FinancialTransaction> withDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), to);
    }

    public static Specification<FinancialTransaction> withAccountingFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("accountingMonth"), from);
    }

    public static Specification<FinancialTransaction> withAccountingTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("accountingMonth"), to);
    }

    public static Specification<FinancialTransaction> withCategoryId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("subcategory").get("category").get("id"), id);
    }

    public static Specification<FinancialTransaction> withSubcategoryId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("subcategory").get("id"), id);
    }

    public static Specification<FinancialTransaction> withBankAccountId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("bankAccount").get("id"), id);
    }

    public static Specification<FinancialTransaction> withBuildingId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("building").get("id"), id);
    }

    public static Specification<FinancialTransaction> withUnitId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("housingUnit").get("id"), id);
    }

    public static Specification<FinancialTransaction> withStatus(TransactionStatus s) {
        return (root, query, cb) -> cb.equal(root.get("status"), s);
    }

    public static Specification<FinancialTransaction> withImportBatchId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("importBatch").get("id"), id);
    }

    public static Specification<FinancialTransaction> withAssetLink(AssetType type, Long assetId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var linkRoot = sub.from(TransactionAssetLink.class);
            sub.select(linkRoot.get("transaction").get("id"))
                    .where(
                            cb.equal(linkRoot.get("assetType"), type),
                            cb.equal(linkRoot.get("assetId"), assetId));
            return root.get("id").in(sub);
        };
    }

    public static Specification<FinancialTransaction> withSearch(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("reference")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("counterpartyAccount")), pattern));
        };
    }

    public static Specification<FinancialTransaction> confirmedOrReconciled() {
        return (root, query, cb) -> root.get("status").in(
                TransactionStatus.CONFIRMED, TransactionStatus.RECONCILED);
    }
}
