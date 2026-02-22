package com.immocare.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Append-only water meter history record for a housing unit.
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-UC006-01: Records are never deleted or updated.</li>
 *   <li>BR-UC006-02: Active meter = removal_date IS NULL.</li>
 *   <li>BR-UC006-04: Only one active meter per unit at a time.</li>
 *   <li>BR-UC006-06: removal_date >= installation_date (enforced by DB check).</li>
 * </ul>
 */
@Entity
@Table(name = "water_meter_history")
public class WaterMeterHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "housing_unit_id", nullable = false)
    private HousingUnit housingUnit;

    @Column(name = "meter_number", nullable = false, length = 50)
    private String meterNumber;

    @Column(name = "meter_location", length = 100)
    private String meterLocation;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Column(name = "removal_date")
    private LocalDate removalDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected WaterMeterHistory() {}

    public WaterMeterHistory(HousingUnit housingUnit,
                              String meterNumber,
                              String meterLocation,
                              LocalDate installationDate) {
        this.housingUnit = housingUnit;
        this.meterNumber = meterNumber;
        this.meterLocation = meterLocation;
        this.installationDate = installationDate;
        this.removalDate = null; // active by default
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public HousingUnit getHousingUnit() { return housingUnit; }

    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

    public String getMeterLocation() { return meterLocation; }
    public void setMeterLocation(String meterLocation) { this.meterLocation = meterLocation; }

    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }

    public LocalDate getRemovalDate() { return removalDate; }
    public void setRemovalDate(LocalDate removalDate) { this.removalDate = removalDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isActive() { return removalDate == null; }
}
