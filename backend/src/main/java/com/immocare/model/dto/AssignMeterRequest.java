package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request DTO for assigning the first water meter to a unit.
 * UC006 - US026 Assign Water Meter.
 */
public class AssignMeterRequest {

    @NotBlank(message = "Meter number is required")
    @Size(max = 50, message = "Meter number must be 50 characters or less")
    @Pattern(
        regexp = "^[A-Za-z0-9\\-_]+$",
        message = "Invalid meter number format: only letters, digits, hyphens and underscores are allowed"
    )
    private String meterNumber;

    @Size(max = 100, message = "Meter location must be 100 characters or less")
    private String meterLocation;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    // Getters / Setters

    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

    public String getMeterLocation() { return meterLocation; }
    public void setMeterLocation(String meterLocation) { this.meterLocation = meterLocation; }

    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }
}
