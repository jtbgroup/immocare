package com.immocare.model.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "housing_unit")
public class HousingUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "landing_number", length = 10)
    private String landingNumber;

    @Column(name = "total_surface", precision = 7, scale = 2)
    private BigDecimal totalSurface;

    @Column(name = "has_terrace", nullable = false)
    private Boolean hasTerrace = false;

    @Column(name = "terrace_surface", precision = 7, scale = 2)
    private BigDecimal terraceSurface;

    @Column(name = "terrace_orientation", length = 2)
    private String terraceOrientation;

    @Column(name = "has_garden", nullable = false)
    private Boolean hasGarden = false;

    @Column(name = "garden_surface", precision = 7, scale = 2)
    private BigDecimal gardenSurface;

    @Column(name = "garden_orientation", length = 2)
    private String gardenOrientation;

    /** Unit-specific owner; overrides building owner when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Person owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getLandingNumber() {
        return landingNumber;
    }

    public void setLandingNumber(String landingNumber) {
        this.landingNumber = landingNumber;
    }

    public BigDecimal getTotalSurface() {
        return totalSurface;
    }

    public void setTotalSurface(BigDecimal totalSurface) {
        this.totalSurface = totalSurface;
    }

    public Boolean getHasTerrace() {
        return hasTerrace;
    }

    public void setHasTerrace(Boolean hasTerrace) {
        this.hasTerrace = hasTerrace;
    }

    public BigDecimal getTerraceSurface() {
        return terraceSurface;
    }

    public void setTerraceSurface(BigDecimal terraceSurface) {
        this.terraceSurface = terraceSurface;
    }

    public String getTerraceOrientation() {
        return terraceOrientation;
    }

    public void setTerraceOrientation(String terraceOrientation) {
        this.terraceOrientation = terraceOrientation;
    }

    public Boolean getHasGarden() {
        return hasGarden;
    }

    public void setHasGarden(Boolean hasGarden) {
        this.hasGarden = hasGarden;
    }

    public BigDecimal getGardenSurface() {
        return gardenSurface;
    }

    public void setGardenSurface(BigDecimal gardenSurface) {
        this.gardenSurface = gardenSurface;
    }

    public String getGardenOrientation() {
        return gardenOrientation;
    }

    public void setGardenOrientation(String gardenOrientation) {
        this.gardenOrientation = gardenOrientation;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}