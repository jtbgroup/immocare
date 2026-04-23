package com.immocare.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.Setter;

/**
 * Temporal rule governing boiler service validity duration for an estate.
 * UC004_ESTATE_PLACEHOLDER Phase 5: estate_id added — rules are now per-estate.
 *
 * <p>Append-only: one rule per (estate_id, valid_from). The applicable rule
 * for a given service_date is the most recent rule whose valid_from <= service_date.
 */
@Entity
@Table(name = "boiler_service_validity_rule")
@Getter
@Setter
public class BoilerServiceValidityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Estate this validity rule belongs to.
     * UC004_ESTATE_PLACEHOLDER Phase 5 — rules are scoped to an estate.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "validity_duration_months", nullable = false)
    private int validityDurationMonths;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
