package com.immocare.model.entity;

import com.immocare.model.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lease")
public class Lease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "housing_unit_id", nullable = false)
    private HousingUnit housingUnit;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LeaseStatus status = LeaseStatus.DRAFT;

    @Column(name = "signature_date", nullable = false) private LocalDate signatureDate;
    @Column(name = "start_date", nullable = false)     private LocalDate startDate;
    @Column(name = "end_date", nullable = false)       private LocalDate endDate;

    @Enumerated(EnumType.STRING) @Column(name = "lease_type", nullable = false, length = 30)
    private LeaseType leaseType;

    @Column(name = "duration_months", nullable = false)     private int durationMonths;
    @Column(name = "notice_period_months", nullable = false) private int noticePeriodMonths;
    @Column(name = "indexation_notice_days", nullable = false) private int indexationNoticeDays = 30;
    @Column(name = "indexation_anniversary_month")          private Integer indexationAnniversaryMonth;

    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)   private BigDecimal monthlyRent;
    @Column(name = "monthly_charges", nullable = false, precision = 10, scale = 2) private BigDecimal monthlyCharges = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(name = "charges_type", nullable = false, length = 20)
    private ChargesType chargesType = ChargesType.FORFAIT;

    @Column(name = "charges_description", columnDefinition = "TEXT") private String chargesDescription;
    @Column(name = "base_index_value", precision = 8, scale = 4)     private BigDecimal baseIndexValue;
    @Column(name = "base_index_month")                                private LocalDate baseIndexMonth;
    @Column(name = "registration_spf", length = 50)                  private String registrationSpf;
    @Column(name = "registration_region", length = 50)               private String registrationRegion;
    @Column(name = "deposit_amount", precision = 10, scale = 2)      private BigDecimal depositAmount;
    @Enumerated(EnumType.STRING) @Column(name = "deposit_type", length = 30) private DepositType depositType;
    @Column(name = "deposit_reference", length = 100)                private String depositReference;
    @Column(name = "tenant_insurance_confirmed", nullable = false)   private boolean tenantInsuranceConfirmed = false;
    @Column(name = "tenant_insurance_reference", length = 100)       private String tenantInsuranceReference;
    @Column(name = "tenant_insurance_expiry")                        private LocalDate tenantInsuranceExpiry;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaseTenant> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "lease") @OrderBy("applicationDate DESC")
    private List<LeaseIndexationHistory> indexations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Transient public boolean isEditable() { return status == LeaseStatus.DRAFT || status == LeaseStatus.ACTIVE; }

    // Getters & Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public HousingUnit getHousingUnit() { return housingUnit; } public void setHousingUnit(HousingUnit h) { this.housingUnit = h; }
    public LeaseStatus getStatus() { return status; } public void setStatus(LeaseStatus s) { this.status = s; }
    public LocalDate getSignatureDate() { return signatureDate; } public void setSignatureDate(LocalDate d) { this.signatureDate = d; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate d) { this.endDate = d; }
    public LeaseType getLeaseType() { return leaseType; } public void setLeaseType(LeaseType t) { this.leaseType = t; }
    public int getDurationMonths() { return durationMonths; } public void setDurationMonths(int d) { this.durationMonths = d; }
    public int getNoticePeriodMonths() { return noticePeriodMonths; } public void setNoticePeriodMonths(int n) { this.noticePeriodMonths = n; }
    public int getIndexationNoticeDays() { return indexationNoticeDays; } public void setIndexationNoticeDays(int d) { this.indexationNoticeDays = d; }
    public Integer getIndexationAnniversaryMonth() { return indexationAnniversaryMonth; } public void setIndexationAnniversaryMonth(Integer m) { this.indexationAnniversaryMonth = m; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal r) { this.monthlyRent = r; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal c) { this.monthlyCharges = c; }
    public ChargesType getChargesType() { return chargesType; } public void setChargesType(ChargesType t) { this.chargesType = t; }
    public String getChargesDescription() { return chargesDescription; } public void setChargesDescription(String d) { this.chargesDescription = d; }
    public BigDecimal getBaseIndexValue() { return baseIndexValue; } public void setBaseIndexValue(BigDecimal v) { this.baseIndexValue = v; }
    public LocalDate getBaseIndexMonth() { return baseIndexMonth; } public void setBaseIndexMonth(LocalDate m) { this.baseIndexMonth = m; }
    public String getRegistrationSpf() { return registrationSpf; } public void setRegistrationSpf(String s) { this.registrationSpf = s; }
    public String getRegistrationRegion() { return registrationRegion; } public void setRegistrationRegion(String s) { this.registrationRegion = s; }
    public BigDecimal getDepositAmount() { return depositAmount; } public void setDepositAmount(BigDecimal a) { this.depositAmount = a; }
    public DepositType getDepositType() { return depositType; } public void setDepositType(DepositType t) { this.depositType = t; }
    public String getDepositReference() { return depositReference; } public void setDepositReference(String r) { this.depositReference = r; }
    public boolean isTenantInsuranceConfirmed() { return tenantInsuranceConfirmed; } public void setTenantInsuranceConfirmed(boolean b) { this.tenantInsuranceConfirmed = b; }
    public String getTenantInsuranceReference() { return tenantInsuranceReference; } public void setTenantInsuranceReference(String r) { this.tenantInsuranceReference = r; }
    public LocalDate getTenantInsuranceExpiry() { return tenantInsuranceExpiry; } public void setTenantInsuranceExpiry(LocalDate d) { this.tenantInsuranceExpiry = d; }
    public List<LeaseTenant> getTenants() { return tenants; } public void setTenants(List<LeaseTenant> t) { this.tenants = t; }
    public List<LeaseIndexationHistory> getIndexations() { return indexations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
