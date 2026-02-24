package com.immocare.model.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lease_indexation_history")
public class LeaseIndexationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lease_id") private Lease lease;
    @Column(name = "application_date", nullable = false) private LocalDate applicationDate;
    @Column(name = "old_rent", nullable = false, precision = 10, scale = 2) private BigDecimal oldRent;
    @Column(name = "new_index_value", nullable = false, precision = 8, scale = 4) private BigDecimal newIndexValue;
    @Column(name = "new_index_month", nullable = false) private LocalDate newIndexMonth;
    @Column(name = "applied_rent", nullable = false, precision = 10, scale = 2) private BigDecimal appliedRent;
    @Column(name = "notification_date") private LocalDate notificationDate;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Lease getLease() { return lease; } public void setLease(Lease lease) { this.lease = lease; }
    public LocalDate getApplicationDate() { return applicationDate; } public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    public BigDecimal getOldRent() { return oldRent; } public void setOldRent(BigDecimal oldRent) { this.oldRent = oldRent; }
    public BigDecimal getNewIndexValue() { return newIndexValue; } public void setNewIndexValue(BigDecimal newIndexValue) { this.newIndexValue = newIndexValue; }
    public LocalDate getNewIndexMonth() { return newIndexMonth; } public void setNewIndexMonth(LocalDate newIndexMonth) { this.newIndexMonth = newIndexMonth; }
    public BigDecimal getAppliedRent() { return appliedRent; } public void setAppliedRent(BigDecimal appliedRent) { this.appliedRent = appliedRent; }
    public LocalDate getNotificationDate() { return notificationDate; } public void setNotificationDate(LocalDate notificationDate) { this.notificationDate = notificationDate; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
