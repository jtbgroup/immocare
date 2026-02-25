package com.immocare.model.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing housing unit.
 * buildingId is intentionally excluded — units cannot be moved between
 * buildings.
 * ownerId references a Person entity; null clears the unit-level owner.
 */
public class UpdateHousingUnitRequest {

  @NotBlank(message = "Unit number is required")
  @Size(max = 20, message = "Unit number must be 20 characters or less")
  private String unitNumber;

  @NotNull(message = "Floor is required")
  @Min(value = -10, message = "Floor must be between -10 and 100")
  @Max(value = 100, message = "Floor must be between -10 and 100")
  private Integer floor;

  @Size(max = 10, message = "Landing number must be 10 characters or less")
  private String landingNumber;

  @DecimalMin(value = "0.01", message = "Total surface must be greater than 0")
  private BigDecimal totalSurface;

  /** Optional owner: references a Person by ID. Null = clear unit-level owner. */
  private Long ownerId;

  private Boolean hasTerrace = false;

  @DecimalMin(value = "0.01", message = "Terrace surface must be greater than 0")
  private BigDecimal terraceSurface;

  @Pattern(regexp = "^(N|S|E|W|NE|NW|SE|SW)$", message = "Terrace orientation must be N, S, E, W, NE, NW, SE or SW")
  private String terraceOrientation;

  private Boolean hasGarden = false;

  @DecimalMin(value = "0.01", message = "Garden surface must be greater than 0")
  private BigDecimal gardenSurface;

  @Pattern(regexp = "^(N|S|E|W|NE|NW|SE|SW)$", message = "Garden orientation must be N, S, E, W, NE, NW, SE or SW")
  private String gardenOrientation;

  // Getters and Setters
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

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long ownerId) {
    this.ownerId = ownerId;
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
}