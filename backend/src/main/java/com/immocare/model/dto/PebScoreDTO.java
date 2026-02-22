package com.immocare.model.dto;

import com.immocare.model.entity.PebScore;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a PEB score history record.
 * Computed fields (status, expiryWarning) are set by the service layer.
 * UC004 - Manage PEB Scores.
 */
public class PebScoreDTO {

    private Long id;
    private Long housingUnitId;
    private PebScore pebScore;
    private LocalDate scoreDate;
    private String certificateNumber;
    private LocalDate validUntil;
    private LocalDateTime createdAt;

    /** Computed: CURRENT | HISTORICAL | EXPIRED */
    private String status;

    /** Computed: EXPIRED | EXPIRING_SOON | VALID | NO_DATE */
    private String expiryWarning;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHousingUnitId() { return housingUnitId; }
    public void setHousingUnitId(Long housingUnitId) { this.housingUnitId = housingUnitId; }

    public PebScore getPebScore() { return pebScore; }
    public void setPebScore(PebScore pebScore) { this.pebScore = pebScore; }

    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExpiryWarning() { return expiryWarning; }
    public void setExpiryWarning(String expiryWarning) { this.expiryWarning = expiryWarning; }
}
