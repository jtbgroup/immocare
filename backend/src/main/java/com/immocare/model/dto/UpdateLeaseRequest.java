package com.immocare.model.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
public class UpdateLeaseRequest {
    @NotNull private LocalDate signatureDate;
    @NotNull private LocalDate startDate;
    @NotNull private String leaseType;
    @Positive private int durationMonths;
    @Positive private int noticePeriodMonths;
    private int indexationNoticeDays = 30;
    private Integer indexationAnniversaryMonth;
    @NotNull @Positive private BigDecimal monthlyRent;
    private BigDecimal monthlyCharges = BigDecimal.ZERO;
    private String chargesType = "FORFAIT";
    private String chargesDescription;
    private BigDecimal baseIndexValue; private LocalDate baseIndexMonth;
    private String registrationSpf; private String registrationRegion;
    private BigDecimal depositAmount; private String depositType; private String depositReference;
    private boolean tenantInsuranceConfirmed; private String tenantInsuranceReference; private LocalDate tenantInsuranceExpiry;
    public LocalDate getSignatureDate() { return signatureDate; } public void setSignatureDate(LocalDate v) { this.signatureDate = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { this.startDate = v; }
    public String getLeaseType() { return leaseType; } public void setLeaseType(String v) { this.leaseType = v; }
    public int getDurationMonths() { return durationMonths; } public void setDurationMonths(int v) { this.durationMonths = v; }
    public int getNoticePeriodMonths() { return noticePeriodMonths; } public void setNoticePeriodMonths(int v) { this.noticePeriodMonths = v; }
    public int getIndexationNoticeDays() { return indexationNoticeDays; } public void setIndexationNoticeDays(int v) { this.indexationNoticeDays = v; }
    public Integer getIndexationAnniversaryMonth() { return indexationAnniversaryMonth; } public void setIndexationAnniversaryMonth(Integer v) { this.indexationAnniversaryMonth = v; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }
    public String getChargesType() { return chargesType; } public void setChargesType(String v) { this.chargesType = v; }
    public String getChargesDescription() { return chargesDescription; } public void setChargesDescription(String v) { this.chargesDescription = v; }
    public BigDecimal getBaseIndexValue() { return baseIndexValue; } public void setBaseIndexValue(BigDecimal v) { this.baseIndexValue = v; }
    public LocalDate getBaseIndexMonth() { return baseIndexMonth; } public void setBaseIndexMonth(LocalDate v) { this.baseIndexMonth = v; }
    public String getRegistrationSpf() { return registrationSpf; } public void setRegistrationSpf(String v) { this.registrationSpf = v; }
    public String getRegistrationRegion() { return registrationRegion; } public void setRegistrationRegion(String v) { this.registrationRegion = v; }
    public BigDecimal getDepositAmount() { return depositAmount; } public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }
    public String getDepositType() { return depositType; } public void setDepositType(String v) { this.depositType = v; }
    public String getDepositReference() { return depositReference; } public void setDepositReference(String v) { this.depositReference = v; }
    public boolean isTenantInsuranceConfirmed() { return tenantInsuranceConfirmed; } public void setTenantInsuranceConfirmed(boolean v) { this.tenantInsuranceConfirmed = v; }
    public String getTenantInsuranceReference() { return tenantInsuranceReference; } public void setTenantInsuranceReference(String v) { this.tenantInsuranceReference = v; }
    public LocalDate getTenantInsuranceExpiry() { return tenantInsuranceExpiry; } public void setTenantInsuranceExpiry(LocalDate v) { this.tenantInsuranceExpiry = v; }
}
