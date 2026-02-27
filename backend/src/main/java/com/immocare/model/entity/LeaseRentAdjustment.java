package com.immocare.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lease_rent_adjustment")
public class LeaseRentAdjustment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    /** "RENT" or "CHARGES" */
    @Column(nullable = false, length = 10)
    private String field;

    @Column(name = "old_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldValue;

    @Column(name = "new_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal newValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Lease getLease() { return lease; } public void setLease(Lease v) { this.lease = v; }
    public String getField() { return field; } public void setField(String v) { this.field = v; }
    public BigDecimal getOldValue() { return oldValue; } public void setOldValue(BigDecimal v) { this.oldValue = v; }
    public BigDecimal getNewValue() { return newValue; } public void setNewValue(BigDecimal v) { this.newValue = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
    public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate v) { this.effectiveDate = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
