package com.immocare.model.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public class CreateLeaseRequest {
    @NotNull private Long housingUnitId;
    @NotNull private LocalDate signatureDate;
    @NotNull private LocalDate startDate;
    @NotNull private String leaseType;
    @Positive private int durationMonths;
    @Positive private int noticePeriodMonths;
    private int indexationNoticeDays = 30;
    // indexationAnniversaryMonth NOT accepted from client — computed server-side from startDate
    @NotNull @Positive private BigDecimal monthlyRent;
    private BigDecimal monthlyCharges = BigDecimal.ZERO;
    private String chargesType = "FORFAIT";
    private String chargesDescription;
    private BigDecimal baseIndexValue;
    private LocalDate baseIndexMonth;
    private String registrationSpf;
    private String registrationInventorySpf;
    private String registrationRegion;
    private BigDecimal depositAmount;
    private String depositType;
    private String depositReference;
    private boolean tenantInsuranceConfirmed;
    private String tenantInsuranceReference;
    private LocalDate tenantInsuranceExpiry;
    @Valid private List<AddTenantRequest> tenants;

    public Long getHousingUnitId() { return housingUnitId; } public void setHousingUnitId(Long v) { this.housingUnitId = v; }
    public LocalDate getSignatureDate() { return signatureDate; } public void setSignatureDate(LocalDate v) { this.signatureDate = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { this.startDate = v; }
    public String getLeaseType() { return leaseType; } public void setLeaseType(String v) { this.leaseType = v; }
    public int getDurationMonths() { return durationMonths; } public void setDurationMonths(int v) { this.durationMonths = v; }
    public int getNoticePeriodMonths() { return noticePeriodMonths; } public void setNoticePeriodMonths(int v) { this.noticePeriodMonths = v; }
    public int getIndexationNoticeDays() { return indexationNoticeDays; } public void setIndexationNoticeDays(int v) { this.indexationNoticeDays = v; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }
    public String getChargesType() { return chargesType; } public void setChargesType(String v) { this.chargesType = v; }
    public String getChargesDescription() { return chargesDescription; } public void setChargesDescription(String v) { this.chargesDescription = v; }
    public BigDecimal getBaseIndexValue() { return baseIndexValue; } public void setBaseIndexValue(BigDecimal v) { this.baseIndexValue = v; }
    public LocalDate getBaseIndexMonth() { return baseIndexMonth; } public void setBaseIndexMonth(LocalDate v) { this.baseIndexMonth = v; }
    public String getRegistrationSpf() { return registrationSpf; } public void setRegistrationSpf(String v) { this.registrationSpf = v; }
    public String getRegistrationInventorySpf() { return registrationInventorySpf; } public void setRegistrationInventorySpf(String v) { this.registrationInventorySpf = v; }
    public String getRegistrationRegion() { return registrationRegion; } public void setRegistrationRegion(String v) { this.registrationRegion = v; }
    public BigDecimal getDepositAmount() { return depositAmount; } public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }
    public String getDepositType() { return depositType; } public void setDepositType(String v) { this.depositType = v; }
    public String getDepositReference() { return depositReference; } public void setDepositReference(String v) { this.depositReference = v; }
    public boolean isTenantInsuranceConfirmed() { return tenantInsuranceConfirmed; } public void setTenantInsuranceConfirmed(boolean v) { this.tenantInsuranceConfirmed = v; }
    public String getTenantInsuranceReference() { return tenantInsuranceReference; } public void setTenantInsuranceReference(String v) { this.tenantInsuranceReference = v; }
    public LocalDate getTenantInsuranceExpiry() { return tenantInsuranceExpiry; } public void setTenantInsuranceExpiry(LocalDate v) { this.tenantInsuranceExpiry = v; }
    public List<AddTenantRequest> getTenants() { return tenants; } public void setTenants(List<AddTenantRequest> v) { this.tenants = v; }
}
