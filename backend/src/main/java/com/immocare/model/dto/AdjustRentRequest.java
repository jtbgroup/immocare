package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AdjustRentRequest {

    /** "RENT" or "CHARGES" */
    @NotBlank
    private String field;

    /** New absolute value (not a delta) */
    @NotNull @Positive
    private BigDecimal newValue;

    @NotBlank
    private String reason;

    @NotNull
    private LocalDate effectiveDate;

    public String getField() { return field; } public void setField(String v) { this.field = v; }
    public BigDecimal getNewValue() { return newValue; } public void setNewValue(BigDecimal v) { this.newValue = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
    public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate v) { this.effectiveDate = v; }
}
