package com.immocare.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Request DTO for removing the active water meter without replacement.
 * UC006 - US029 Remove Water Meter.
 */
public class RemoveMeterRequest {

    @NotNull(message = "Removal date is required")
    @PastOrPresent(message = "Removal date cannot be in the future")
    private LocalDate removalDate;

    // Getters / Setters

    public LocalDate getRemovalDate() { return removalDate; }
    public void setRemovalDate(LocalDate removalDate) { this.removalDate = removalDate; }
}
