package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request DTO for replacing an existing water meter.
 * UC006 - US027 Replace Water Meter.
 *
 * <p>The old meter's removal_date is automatically set to newInstallationDate.
 * Cross-field validation (newInstallationDate >= current meter date) is enforced in the service.
 */
public class ReplaceMeterRequest {

    @NotBlank(message = "New meter number is required")
    @Size(max = 50, message = "Meter number must be 50 characters or less")
    @Pattern(
        regexp = "^[A-Za-z0-9\\-_]+$",
        message = "Invalid meter number format: only letters, digits, hyphens and underscores are allowed"
    )
    private String newMeterNumber;

    @Size(max = 100, message = "Meter location must be 100 characters or less")
    private String newMeterLocation;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate newInstallationDate;

    /** One of: BROKEN, END_OF_LIFE, UPGRADE, CALIBRATION_ISSUE, OTHER */
    private String reason;

    // Getters / Setters

    public String getNewMeterNumber() { return newMeterNumber; }
    public void setNewMeterNumber(String newMeterNumber) { this.newMeterNumber = newMeterNumber; }

    public String getNewMeterLocation() { return newMeterLocation; }
    public void setNewMeterLocation(String newMeterLocation) { this.newMeterLocation = newMeterLocation; }

    public LocalDate getNewInstallationDate() { return newInstallationDate; }
    public void setNewInstallationDate(LocalDate newInstallationDate) { this.newInstallationDate = newInstallationDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
