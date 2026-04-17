package com.immocare.repository.spec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.immocare.model.dto.LeaseFilterParams;
import com.immocare.model.entity.Lease;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;

import jakarta.persistence.criteria.Predicate;

/**
 * JPA Specifications for Lease queries.
 * UC016 Phase 3: {@link #hasEstate(UUID)} added for estate-scoped filtering.
 */
public class LeaseSpecification {

    private LeaseSpecification() {}

    // ── Individual specs ──────────────────────────────────────────────────────

    public static Specification<Lease> hasStatusIn(List<LeaseStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Lease> hasLeaseType(LeaseType type) {
        return (root, query, cb) -> cb.equal(root.get("leaseType"), type);
    }

    public static Specification<Lease> hasBuilding(Long buildingId) {
        return (root, query, cb) ->
                cb.equal(root.get("housingUnit").get("building").get("id"), buildingId);
    }

    public static Specification<Lease> hasHousingUnit(Long housingUnitId) {
        return (root, query, cb) ->
                cb.equal(root.get("housingUnit").get("id"), housingUnitId);
    }

    /**
     * Restricts results to leases whose housing unit belongs to the given estate.
     * UC016 Phase 3 — used by LeaseService.getAll() to enforce estate scope.
     */
    public static Specification<Lease> hasEstate(UUID estateId) {
        return (root, query, cb) ->
                cb.equal(root.get("housingUnit").get("building").get("estate").get("id"), estateId);
    }

    public static Specification<Lease> startDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Lease> startDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    public static Specification<Lease> endDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), from);
    }

    public static Specification<Lease> endDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }

    public static Specification<Lease> rentMin(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("monthlyRent"), min);
    }

    public static Specification<Lease> rentMax(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("monthlyRent"), max);
    }

    // ── Composite builder ─────────────────────────────────────────────────────

    /**
     * Builds a combined Specification from a {@link LeaseFilterParams} object.
     * Only non-null/non-empty params produce predicates.
     * Note: estate scope is NOT added here — call {@code .and(hasEstate(estateId))}
     * explicitly in the service when needed.
     */
    public static Specification<Lease> of(LeaseFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.getStatuses() != null && !params.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(params.getStatuses()));
            }
            if (params.getLeaseType() != null) {
                predicates.add(cb.equal(root.get("leaseType"), params.getLeaseType()));
            }
            if (params.getBuildingId() != null) {
                predicates.add(cb.equal(
                        root.get("housingUnit").get("building").get("id"),
                        params.getBuildingId()));
            }
            if (params.getHousingUnitId() != null) {
                predicates.add(cb.equal(
                        root.get("housingUnit").get("id"),
                        params.getHousingUnitId()));
            }
            if (params.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("startDate"), params.getStartDateFrom()));
            }
            if (params.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("startDate"), params.getStartDateTo()));
            }
            if (params.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("endDate"), params.getEndDateFrom()));
            }
            if (params.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("endDate"), params.getEndDateTo()));
            }
            if (params.getRentMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("monthlyRent"), params.getRentMin()));
            }
            if (params.getRentMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("monthlyRent"), params.getRentMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
