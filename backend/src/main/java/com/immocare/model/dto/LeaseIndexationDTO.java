package com.immocare.model.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class LeaseIndexationDTO {
    private Long id;
    private LocalDate applicationDate;
    private BigDecimal oldRent;
    private BigDecimal newIndexValue;
    private LocalDate newIndexMonth;
    private BigDecimal appliedRent;
    private LocalDate notificationDate;
    private String notes;
    private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDate getApplicationDate() { return applicationDate; } public void setApplicationDate(LocalDate d) { this.applicationDate = d; }
    public BigDecimal getOldRent() { return oldRent; } public void setOldRent(BigDecimal v) { this.oldRent = v; }
    public BigDecimal getNewIndexValue() { return newIndexValue; } public void setNewIndexValue(BigDecimal v) { this.newIndexValue = v; }
    public LocalDate getNewIndexMonth() { return newIndexMonth; } public void setNewIndexMonth(LocalDate d) { this.newIndexMonth = d; }
    public BigDecimal getAppliedRent() { return appliedRent; } public void setAppliedRent(BigDecimal v) { this.appliedRent = v; }
    public LocalDate getNotificationDate() { return notificationDate; } public void setNotificationDate(LocalDate d) { this.notificationDate = d; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
