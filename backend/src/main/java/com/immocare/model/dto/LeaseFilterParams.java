package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;

/**
 * Encapsulates all filter criteria for the global lease list.
 * Each field is optional (null = no filter applied).
 * Add new fields here as filtering needs evolve.
 */
public class LeaseFilterParams {

    /** Filter by one or more statuses. Null or empty = all statuses. */
    private List<LeaseStatus> statuses;

    /** Filter by lease type. */
    private LeaseType leaseType;

    /** Filter by building ID. */
    private Long buildingId;

    /** Filter by housing unit ID. */
    private Long housingUnitId;

    /** Filter leases whose start date is on or after this date. */
    private LocalDate startDateFrom;

    /** Filter leases whose start date is on or before this date. */
    private LocalDate startDateTo;

    /** Filter leases whose end date is on or after this date. */
    private LocalDate endDateFrom;

    /** Filter leases whose end date is on or before this date. */
    private LocalDate endDateTo;

    /** Filter leases with monthly rent >= this value. */
    private BigDecimal rentMin;

    /** Filter leases with monthly rent <= this value. */
    private BigDecimal rentMax;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public List<LeaseStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<LeaseStatus> statuses) {
        this.statuses = statuses;
    }

    public LeaseType getLeaseType() {
        return leaseType;
    }

    public void setLeaseType(LeaseType leaseType) {
        this.leaseType = leaseType;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Long getHousingUnitId() {
        return housingUnitId;
    }

    public void setHousingUnitId(Long housingUnitId) {
        this.housingUnitId = housingUnitId;
    }

    public LocalDate getStartDateFrom() {
        return startDateFrom;
    }

    public void setStartDateFrom(LocalDate startDateFrom) {
        this.startDateFrom = startDateFrom;
    }

    public LocalDate getStartDateTo() {
        return startDateTo;
    }

    public void setStartDateTo(LocalDate startDateTo) {
        this.startDateTo = startDateTo;
    }

    public LocalDate getEndDateFrom() {
        return endDateFrom;
    }

    public void setEndDateFrom(LocalDate endDateFrom) {
        this.endDateFrom = endDateFrom;
    }

    public LocalDate getEndDateTo() {
        return endDateTo;
    }

    public void setEndDateTo(LocalDate endDateTo) {
        this.endDateTo = endDateTo;
    }

    public BigDecimal getRentMin() {
        return rentMin;
    }

    public void setRentMin(BigDecimal rentMin) {
        this.rentMin = rentMin;
    }

    public BigDecimal getRentMax() {
        return rentMax;
    }

    public void setRentMax(BigDecimal rentMax) {
        this.rentMax = rentMax;
    }
}