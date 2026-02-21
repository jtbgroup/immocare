package com.immocare.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing an individual housing unit (apartment) within a building.
 *
 * Business Rules:
 * - Must belong to a building
 * - Unit number must be unique within a building
 * - Floor must be between -10 and 100
 * - Terrace/garden surfaces required when has_terrace/has_garden = true (enforced in service)
 * - owner_name overrides building.owner_name when set
 */
@Entity
@Table(
    name = "housing_unit",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_housing_unit_number",
        columnNames = {"building_id", "unit_number"}
    )
)
public class HousingUnit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Building is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "building_id", nullable = false)
  private Building building;

  @NotBlank(message = "Unit number is required")
  @Size(max = 20, message = "Unit number must be 20 characters or less")
  @Column(name = "unit_number", nullable = false, length = 20)
  private String unitNumber;

  @NotNull(message = "Floor is required")
  @Min(value = -10, message = "Floor must be between -10 and 100")
  @Max(value = 100, message = "Floor must be between -10 and 100")
  @Column(name = "floor", nullable = false)
  private Integer floor;

  @Size(max = 10, message = "Landing number must be 10 characters or less")
  @Column(name = "landing_number", length = 10)
  private String landingNumber;

  @DecimalMin(value = "0.01", message = "Total surface must be greater than 0")
  @Column(name = "total_surface", precision = 7, scale = 2)
  private BigDecimal totalSurface;

  @NotNull
  @Column(name = "has_terrace", nullable = false)
  private Boolean hasTerrace = false;

  @DecimalMin(value = "0.01", message = "Terrace surface must be greater than 0")
  @Column(name = "terrace_surface", precision = 7, scale = 2)
  private BigDecimal terraceSurface;

  @Pattern(regexp = "^(N|S|E|W|NE|NW|SE|SW)$", message = "Invalid orientation")
  @Column(name = "terrace_orientation", length = 2)
  private String terraceOrientation;

  @NotNull
  @Column(name = "has_garden", nullable = false)
  private Boolean hasGarden = false;

  @DecimalMin(value = "0.01", message = "Garden surface must be greater than 0")
  @Column(name = "garden_surface", precision = 7, scale = 2)
  private BigDecimal gardenSurface;

  @Pattern(regexp = "^(N|S|E|W|NE|NW|SE|SW)$", message = "Invalid orientation")
  @Column(name = "garden_orientation", length = 2)
  private String gardenOrientation;

  @Size(max = 200, message = "Owner name must be 200 characters or less")
  @Column(name = "owner_name", length = 200)
  private String ownerName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AppUser createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Constructors
  public HousingUnit() {
  }

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Building getBuilding() { return building; }
  public void setBuilding(Building building) { this.building = building; }

  public String getUnitNumber() { return unitNumber; }
  public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

  public Integer getFloor() { return floor; }
  public void setFloor(Integer floor) { this.floor = floor; }

  public String getLandingNumber() { return landingNumber; }
  public void setLandingNumber(String landingNumber) { this.landingNumber = landingNumber; }

  public BigDecimal getTotalSurface() { return totalSurface; }
  public void setTotalSurface(BigDecimal totalSurface) { this.totalSurface = totalSurface; }

  public Boolean getHasTerrace() { return hasTerrace; }
  public void setHasTerrace(Boolean hasTerrace) { this.hasTerrace = hasTerrace; }

  public BigDecimal getTerraceSurface() { return terraceSurface; }
  public void setTerraceSurface(BigDecimal terraceSurface) { this.terraceSurface = terraceSurface; }

  public String getTerraceOrientation() { return terraceOrientation; }
  public void setTerraceOrientation(String terraceOrientation) { this.terraceOrientation = terraceOrientation; }

  public Boolean getHasGarden() { return hasGarden; }
  public void setHasGarden(Boolean hasGarden) { this.hasGarden = hasGarden; }

  public BigDecimal getGardenSurface() { return gardenSurface; }
  public void setGardenSurface(BigDecimal gardenSurface) { this.gardenSurface = gardenSurface; }

  public String getGardenOrientation() { return gardenOrientation; }
  public void setGardenOrientation(String gardenOrientation) { this.gardenOrientation = gardenOrientation; }

  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

  public AppUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  @Override
  public String toString() {
    return "HousingUnit{id=" + id + ", unitNumber='" + unitNumber + "', floor=" + floor + "}";
  }
}
