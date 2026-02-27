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
    @Column(name = "start_date",     nullable = false) private LocalDate startDate;
    @Column(name = "end_date",       nullable = false) private LocalDate endDate;

    @Enumerated(EnumType.STRING) @Column(name = "lease_type", nullable = false, length = 30)
    private LeaseType leaseType;

    @Column(name = "duration_months",      nullable = false) private int durationMonths;
    @Column(name = "notice_period_months", nullable = false) private int noticePeriodMonths;

    @Column(name = "monthly_rent",    nullable = false, precision = 10, scale = 2) private BigDecimal monthlyRent;
    @Column(name = "monthly_charges", nullable = false, precision = 10, scale = 2) private BigDecimal monthlyCharges = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "charges_type", nullable = false, length = 20)
    private ChargesType chargesType = ChargesType.FORFAIT;
    @Column(name = "charges_description", columnDefinition = "TEXT") private String chargesDescription;

    // ── Registration — lease deed ─────────────────────────────────────────────
    @Column(name = "registration_spf",    length = 50) private String registrationSpf;
    @Column(name = "registration_region", length = 50) private String registrationRegion;

    // ── Registration — inventory (état des lieux) ─────────────────────────────
    @Column(name = "registration_inventory_spf",    length = 100) private String registrationInventorySpf;
    @Column(name = "registration_inventory_region", length = 100) private String registrationInventoryRegion;

    @Column(name = "deposit_amount", precision = 10, scale = 2)          private BigDecimal depositAmount;
    @Enumerated(EnumType.STRING) @Column(name = "deposit_type", length = 30) private DepositType depositType;
    @Column(name = "deposit_reference", length = 100)                    private String depositReference;

    @Column(name = "tenant_insurance_confirmed", nullable = false)       private boolean tenantInsuranceConfirmed = false;
    @Column(name = "tenant_insurance_reference", length = 100)           private String tenantInsuranceReference;
    @Column(name = "tenant_insurance_expiry")                            private LocalDate tenantInsuranceExpiry;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaseTenant> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("effectiveDate DESC, createdAt DESC")
    private List<LeaseRentAdjustment> rentAdjustments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Transient public boolean isEditable() { return status == LeaseStatus.DRAFT || status == LeaseStatus.ACTIVE; }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public HousingUnit getHousingUnit() { return housingUnit; }
    public void setHousingUnit(HousingUnit v) { this.housingUnit = v; }

    public LeaseStatus getStatus() { return status; }
    public void setStatus(LeaseStatus v) { this.status = v; }

    public LocalDate getSignatureDate() { return signatureDate; }
    public void setSignatureDate(LocalDate v) { this.signatureDate = v; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }

    public LeaseType getLeaseType() { return leaseType; }
    public void setLeaseType(LeaseType v) { this.leaseType = v; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int v) { this.durationMonths = v; }

    public int getNoticePeriodMonths() { return noticePeriodMonths; }
    public void setNoticePeriodMonths(int v) { this.noticePeriodMonths = v; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }

    public BigDecimal getMonthlyCharges() { return monthlyCharges; }
    public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }

    public ChargesType getChargesType() { return chargesType; }
    public void setChargesType(ChargesType v) { this.chargesType = v; }

    public String getChargesDescription() { return chargesDescription; }
    public void setChargesDescription(String v) { this.chargesDescription = v; }

    public String getRegistrationSpf() { return registrationSpf; }
    public void setRegistrationSpf(String v) { this.registrationSpf = v; }

    public String getRegistrationRegion() { return registrationRegion; }
    public void setRegistrationRegion(String v) { this.registrationRegion = v; }

    public String getRegistrationInventorySpf() { return registrationInventorySpf; }
    public void setRegistrationInventorySpf(String v) { this.registrationInventorySpf = v; }

    public String getRegistrationInventoryRegion() { return registrationInventoryRegion; }
    public void setRegistrationInventoryRegion(String v) { this.registrationInventoryRegion = v; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }

    public DepositType getDepositType() { return depositType; }
    public void setDepositType(DepositType v) { this.depositType = v; }

    public String getDepositReference() { return depositReference; }
    public void setDepositReference(String v) { this.depositReference = v; }

    public boolean isTenantInsuranceConfirmed() { return tenantInsuranceConfirmed; }
    public void setTenantInsuranceConfirmed(boolean v) { this.tenantInsuranceConfirmed = v; }

    public String getTenantInsuranceReference() { return tenantInsuranceReference; }
    public void setTenantInsuranceReference(String v) { this.tenantInsuranceReference = v; }

    public LocalDate getTenantInsuranceExpiry() { return tenantInsuranceExpiry; }
    public void setTenantInsuranceExpiry(LocalDate v) { this.tenantInsuranceExpiry = v; }

    public List<LeaseTenant> getTenants() { return tenants; }
    public void setTenants(List<LeaseTenant> v) { this.tenants = v; }

    public List<LeaseRentAdjustment> getRentAdjustments() { return rentAdjustments; }
    public void setRentAdjustments(List<LeaseRentAdjustment> v) { this.rentAdjustments = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
