package com.immocare.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Append-only PEB energy certificate history for a housing unit.
 * UC004 - Manage PEB Scores.
 *
 * Business rules:
 * - No updates or deletes (BR-UC004-01)
 * - Current score = record with most recent score_date (BR-UC004-02)
 * - score_date cannot be in the future (enforced in service) (BR-UC004-03)
 * - valid_until must be after score_date if provided (enforced in service) (BR-UC004-04)
 */
@Entity
@Table(name = "peb_score_history")
public class PebScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "housing_unit_id", nullable = false)
    private HousingUnit housingUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "peb_score", nullable = false, length = 10)
    private PebScore pebScore;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "certificate_number", length = 50)
    private String certificateNumber;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }

    public HousingUnit getHousingUnit() { return housingUnit; }
    public void setHousingUnit(HousingUnit housingUnit) { this.housingUnit = housingUnit; }

    public PebScore getPebScore() { return pebScore; }
    public void setPebScore(PebScore pebScore) { this.pebScore = pebScore; }

    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
