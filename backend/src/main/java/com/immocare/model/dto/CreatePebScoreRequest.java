package com.immocare.model.dto;

import com.immocare.model.entity.PebScore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request DTO for adding a PEB score.
 * Cross-field validation (validUntil > scoreDate) is enforced in the service layer.
 * UC004 - US017 Add PEB Score.
 */
public class CreatePebScoreRequest {

    @NotNull(message = "PEB score is required")
    private PebScore pebScore;

    @NotNull(message = "Score date is required")
    @PastOrPresent(message = "Score date cannot be in the future")
    private LocalDate scoreDate;

    @Size(max = 50, message = "Certificate number must be 50 characters or less")
    @Pattern(regexp = "^[A-Za-z0-9\\-]*$", message = "Certificate number must be alphanumeric with hyphens only")
    private String certificateNumber;

    private LocalDate validUntil;

    // Getters and setters

    public PebScore getPebScore() { return pebScore; }
    public void setPebScore(PebScore pebScore) { this.pebScore = pebScore; }

    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
}
