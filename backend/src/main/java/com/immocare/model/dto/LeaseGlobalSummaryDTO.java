package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Summary DTO for the global lease list (across all buildings).
 * Extends LeaseSummaryDTO with building/unit context fields.
 */
public class LeaseGlobalSummaryDTO {

    private Long id;
    private String status;
    private String leaseType;

    // Unit & building context
    private Long housingUnitId;
    private String housingUnitNumber;
    private Long buildingId;
    private String buildingName;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal monthlyCharges;
    private BigDecimal totalRent;
    private String chargesType;

    private List<String> tenantNames;

    private boolean indexationAlertActive;
    private LocalDate indexationAlertDate;
    private boolean endNoticeAlertActive;
    private LocalDate endNoticeAlertDate;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLeaseType() {
        return leaseType;
    }

    public void setLeaseType(String leaseType) {
        this.leaseType = leaseType;
    }

    public Long getHousingUnitId() {
        return housingUnitId;
    }

    public void setHousingUnitId(Long housingUnitId) {
        this.housingUnitId = housingUnitId;
    }

    public String getHousingUnitNumber() {
        return housingUnitNumber;
    }

    public void setHousingUnitNumber(String housingUnitNumber) {
        this.housingUnitNumber = housingUnitNumber;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getMonthlyCharges() {
        return monthlyCharges;
    }

    public void setMonthlyCharges(BigDecimal monthlyCharges) {
        this.monthlyCharges = monthlyCharges;
    }

    public BigDecimal getTotalRent() {
        return totalRent;
    }

    public void setTotalRent(BigDecimal totalRent) {
        this.totalRent = totalRent;
    }

    public String getChargesType() {
        return chargesType;
    }

    public void setChargesType(String chargesType) {
        this.chargesType = chargesType;
    }

    public List<String> getTenantNames() {
        return tenantNames;
    }

    public void setTenantNames(List<String> tenantNames) {
        this.tenantNames = tenantNames;
    }

    public boolean isIndexationAlertActive() {
        return indexationAlertActive;
    }

    public void setIndexationAlertActive(boolean indexationAlertActive) {
        this.indexationAlertActive = indexationAlertActive;
    }

    public LocalDate getIndexationAlertDate() {
        return indexationAlertDate;
    }

    public void setIndexationAlertDate(LocalDate indexationAlertDate) {
        this.indexationAlertDate = indexationAlertDate;
    }

    public boolean isEndNoticeAlertActive() {
        return endNoticeAlertActive;
    }

    public void setEndNoticeAlertActive(boolean endNoticeAlertActive) {
        this.endNoticeAlertActive = endNoticeAlertActive;
    }

    public LocalDate getEndNoticeAlertDate() {
        return endNoticeAlertDate;
    }

    public void setEndNoticeAlertDate(LocalDate endNoticeAlertDate) {
        this.endNoticeAlertDate = endNoticeAlertDate;
    }
}