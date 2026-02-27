package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LeaseDTO {
    private Long id; private Long housingUnitId; private String housingUnitNumber;
    private Long buildingId; private String buildingName;
    private String status;
    private LocalDate signatureDate; private LocalDate startDate; private LocalDate endDate;
    private String leaseType; private int durationMonths; private int noticePeriodMonths;
    private BigDecimal monthlyRent; private BigDecimal monthlyCharges;
    private BigDecimal totalRent;
    private String chargesType; private String chargesDescription;
    // Lease deed registration
    private String registrationSpf; private String registrationRegion;
    // Inventory (état des lieux) registration
    private String registrationInventorySpf; private String registrationInventoryRegion;
    private BigDecimal depositAmount; private String depositType; private String depositReference;
    private boolean tenantInsuranceConfirmed; private String tenantInsuranceReference; private LocalDate tenantInsuranceExpiry;
    private List<LeaseTenantDTO> tenants;
    private List<LeaseRentAdjustmentDTO> rentAdjustments;
    private boolean indexationAlertActive; private LocalDate indexationAlertDate;
    private boolean endNoticeAlertActive;  private LocalDate endNoticeAlertDate;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getHousingUnitId() { return housingUnitId; } public void setHousingUnitId(Long v) { this.housingUnitId = v; }
    public String getHousingUnitNumber() { return housingUnitNumber; } public void setHousingUnitNumber(String v) { this.housingUnitNumber = v; }
    public Long getBuildingId() { return buildingId; } public void setBuildingId(Long v) { this.buildingId = v; }
    public String getBuildingName() { return buildingName; } public void setBuildingName(String v) { this.buildingName = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public LocalDate getSignatureDate() { return signatureDate; } public void setSignatureDate(LocalDate v) { this.signatureDate = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { this.endDate = v; }
    public String getLeaseType() { return leaseType; } public void setLeaseType(String v) { this.leaseType = v; }
    public int getDurationMonths() { return durationMonths; } public void setDurationMonths(int v) { this.durationMonths = v; }
    public int getNoticePeriodMonths() { return noticePeriodMonths; } public void setNoticePeriodMonths(int v) { this.noticePeriodMonths = v; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }
    public BigDecimal getTotalRent() { return totalRent; } public void setTotalRent(BigDecimal v) { this.totalRent = v; }
    public String getChargesType() { return chargesType; } public void setChargesType(String v) { this.chargesType = v; }
    public String getChargesDescription() { return chargesDescription; } public void setChargesDescription(String v) { this.chargesDescription = v; }
    public String getRegistrationSpf() { return registrationSpf; } public void setRegistrationSpf(String v) { this.registrationSpf = v; }
    public String getRegistrationRegion() { return registrationRegion; } public void setRegistrationRegion(String v) { this.registrationRegion = v; }
    public String getRegistrationInventorySpf() { return registrationInventorySpf; } public void setRegistrationInventorySpf(String v) { this.registrationInventorySpf = v; }
    public String getRegistrationInventoryRegion() { return registrationInventoryRegion; } public void setRegistrationInventoryRegion(String v) { this.registrationInventoryRegion = v; }
    public BigDecimal getDepositAmount() { return depositAmount; } public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }
    public String getDepositType() { return depositType; } public void setDepositType(String v) { this.depositType = v; }
    public String getDepositReference() { return depositReference; } public void setDepositReference(String v) { this.depositReference = v; }
    public boolean isTenantInsuranceConfirmed() { return tenantInsuranceConfirmed; } public void setTenantInsuranceConfirmed(boolean v) { this.tenantInsuranceConfirmed = v; }
    public String getTenantInsuranceReference() { return tenantInsuranceReference; } public void setTenantInsuranceReference(String v) { this.tenantInsuranceReference = v; }
    public LocalDate getTenantInsuranceExpiry() { return tenantInsuranceExpiry; } public void setTenantInsuranceExpiry(LocalDate v) { this.tenantInsuranceExpiry = v; }
    public List<LeaseTenantDTO> getTenants() { return tenants; } public void setTenants(List<LeaseTenantDTO> v) { this.tenants = v; }
    public List<LeaseRentAdjustmentDTO> getRentAdjustments() { return rentAdjustments; } public void setRentAdjustments(List<LeaseRentAdjustmentDTO> v) { this.rentAdjustments = v; }
    public boolean isIndexationAlertActive() { return indexationAlertActive; } public void setIndexationAlertActive(boolean v) { this.indexationAlertActive = v; }
    public LocalDate getIndexationAlertDate() { return indexationAlertDate; } public void setIndexationAlertDate(LocalDate v) { this.indexationAlertDate = v; }
    public boolean isEndNoticeAlertActive() { return endNoticeAlertActive; } public void setEndNoticeAlertActive(boolean v) { this.endNoticeAlertActive = v; }
    public LocalDate getEndNoticeAlertDate() { return endNoticeAlertDate; } public void setEndNoticeAlertDate(LocalDate v) { this.endNoticeAlertDate = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
