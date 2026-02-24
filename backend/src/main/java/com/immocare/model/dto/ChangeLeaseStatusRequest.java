package com.immocare.model.dto;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
public class ChangeLeaseStatusRequest {
    @NotBlank private String targetStatus;
    private LocalDate effectiveDate;
    private String notes;
    public String getTargetStatus() { return targetStatus; } public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }
    public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
}
