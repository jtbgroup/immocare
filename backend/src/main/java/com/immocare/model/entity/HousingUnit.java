package com.immocare.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "surface_m2")
    private Double surfaceM2;

    @Column(name = "num_rooms")
    private Integer numRooms;

    @Column(name = "has_terrace")
    private Boolean hasTerrace;

    @Column(name = "terrace_surface_m2")
    private Double terraceSurfaceM2;

    @Column(name = "terrace_orientation", length = 20)
    private String terraceOrientation;

    @Column(name = "has_garden")
    private Boolean hasGarden;

    @Column(name = "garden_surface_m2")
    private Double gardenSurfaceM2;

    @Column(name = "has_parking")
    private Boolean hasParking;

    @Column(name = "parking_number", length = 20)
    private String parkingNumber;

    /** Owner now references the Person entity instead of a free-text string */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Person owner;

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

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }

    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }

    public Double getSurfaceM2() { return surfaceM2; }
    public void setSurfaceM2(Double surfaceM2) { this.surfaceM2 = surfaceM2; }

    public Integer getNumRooms() { return numRooms; }
    public void setNumRooms(Integer numRooms) { this.numRooms = numRooms; }

    public Boolean getHasTerrace() { return hasTerrace; }
    public void setHasTerrace(Boolean hasTerrace) { this.hasTerrace = hasTerrace; }

    public Double getTerraceSurfaceM2() { return terraceSurfaceM2; }
    public void setTerraceSurfaceM2(Double terraceSurfaceM2) { this.terraceSurfaceM2 = terraceSurfaceM2; }

    public String getTerraceOrientation() { return terraceOrientation; }
    public void setTerraceOrientation(String terraceOrientation) { this.terraceOrientation = terraceOrientation; }

    public Boolean getHasGarden() { return hasGarden; }
    public void setHasGarden(Boolean hasGarden) { this.hasGarden = hasGarden; }

    public Double getGardenSurfaceM2() { return gardenSurfaceM2; }
    public void setGardenSurfaceM2(Double gardenSurfaceM2) { this.gardenSurfaceM2 = gardenSurfaceM2; }

    public Boolean getHasParking() { return hasParking; }
    public void setHasParking(Boolean hasParking) { this.hasParking = hasParking; }

    public String getParkingNumber() { return parkingNumber; }
    public void setParkingNumber(String parkingNumber) { this.parkingNumber = parkingNumber; }

    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Returns the effective owner: own owner if set, otherwise inherited from building.
     */
    @Transient
    public Person getEffectiveOwner() {
        if (owner != null) return owner;
        return building != null ? building.getOwner() : null;
    }

    @Transient
    public boolean isOwnerInherited() {
        return owner == null && building != null && building.getOwner() != null;
    }
}
