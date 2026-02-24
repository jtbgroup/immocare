package com.immocare.model.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
public class RecordIndexationRequest {
    @NotNull private LocalDate applicationDate;
    @NotNull @Positive private BigDecimal newIndexValue;
    @NotNull private LocalDate newIndexMonth;
    @NotNull @Positive private BigDecimal appliedRent;
    private LocalDate notificationSentDate;
    private String notes;
    public LocalDate getApplicationDate() { return applicationDate; } public void setApplicationDate(LocalDate d) { this.applicationDate = d; }
    public BigDecimal getNewIndexValue() { return newIndexValue; } public void setNewIndexValue(BigDecimal v) { this.newIndexValue = v; }
    public LocalDate getNewIndexMonth() { return newIndexMonth; } public void setNewIndexMonth(LocalDate d) { this.newIndexMonth = d; }
    public BigDecimal getAppliedRent() { return appliedRent; } public void setAppliedRent(BigDecimal v) { this.appliedRent = v; }
    public LocalDate getNotificationSentDate() { return notificationSentDate; } public void setNotificationSentDate(LocalDate d) { this.notificationSentDate = d; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
}
