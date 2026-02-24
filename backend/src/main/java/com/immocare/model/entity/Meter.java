package com.immocare.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Append-only utility meter record.
 * <p>
 * Records are never modified. Closing a meter sets {@code endDate}.
 * An active meter has {@code endDate == null}.
 * Multiple active meters of the same type are allowed per owner.
 */
@Entity
@Table(name = "meter")
@Getter
@Setter
@NoArgsConstructor
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WATER | GAS | ELECTRICITY */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "meter_number", nullable = false, length = 50)
    private String meterNumber;

    /** Required for GAS and ELECTRICITY */
    @Column(name = "ean_code", length = 18)
    private String eanCode;

    /** Required for WATER */
    @Column(name = "installation_number", length = 50)
    private String installationNumber;

    /** Required for WATER on BUILDING */
    @Column(name = "customer_number", length = 50)
    private String customerNumber;

    /** HOUSING_UNIT | BUILDING */
    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    /** FK to housing_unit.id or building.id — polymorphic, no DB-level FK constraint */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** Activation date — cannot be in the future (BR-01) */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Closure date — null = active (BR-02: endDate >= startDate) */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
