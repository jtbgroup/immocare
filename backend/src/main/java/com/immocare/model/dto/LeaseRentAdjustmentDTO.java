package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaseRentAdjustmentDTO {
    private Long id;
    private String field;          // "RENT" or "CHARGES"
    private BigDecimal oldValue;
    private BigDecimal newValue;
    private String reason;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getField() { return field; } public void setField(String v) { this.field = v; }
    public BigDecimal getOldValue() { return oldValue; } public void setOldValue(BigDecimal v) { this.oldValue = v; }
    public BigDecimal getNewValue() { return newValue; } public void setNewValue(BigDecimal v) { this.newValue = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
    public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate v) { this.effectiveDate = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
