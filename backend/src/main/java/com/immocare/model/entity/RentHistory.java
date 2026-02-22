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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Append-only rent history for a housing unit.
 *
 * <p>Business rules:
 * <ul>
 *   <li>current rent  → effective_to = NULL (only one per unit at a time)</li>
 *   <li>historical rent → effective_to is set</li>
 *   <li>Records are never updated or deleted.</li>
 * </ul>
 */
@Entity
@Table(name = "rent_history")
public class RentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "housing_unit_id", nullable = false)
    private HousingUnit housingUnit;

    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected RentHistory() {}

    public RentHistory(HousingUnit housingUnit,
                       BigDecimal monthlyRent,
                       LocalDate effectiveFrom,
                       LocalDate effectiveTo,
                       String notes) {
        this.housingUnit = housingUnit;
        this.monthlyRent = monthlyRent;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.notes = notes;
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public HousingUnit getHousingUnit() { return housingUnit; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getNotes() { return notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
