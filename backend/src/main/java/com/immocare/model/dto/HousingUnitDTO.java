package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.immocare.model.entity.PebScore;

public class HousingUnitDTO {

  private Long id;
  private Long buildingId;
  private String buildingName;
  private String unitNumber;
  private Integer floor;
  private String landingNumber;
  private BigDecimal totalSurface;
  private Boolean hasTerrace;
  private BigDecimal terraceSurface;
  private String terraceOrientation;
  private Boolean hasGarden;
  private BigDecimal gardenSurface;
  private String gardenOrientation;
  private BigDecimal currentMonthlyRent;
  private PebScore currentPebScore;

  /** ACTIVE, DRAFT, or null if no current lease */
  private String activeLeaseStatus;

  private Long ownerId;
  private String ownerName;
  private String effectiveOwnerName;
  private Long roomCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public HousingUnitDTO() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBuildingId() {
    return buildingId;
  }

  public void setBuildingId(Long buildingId) {
    this.buildingId = buildingId;
  }

  public String getBuildingName() {
    return buildingName;
  }

  public void setBuildingName(String buildingName) {
    this.buildingName = buildingName;
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

  public BigDecimal getCurrentMonthlyRent() {
    return currentMonthlyRent;
  }

  public void setCurrentMonthlyRent(BigDecimal currentMonthlyRent) {
    this.currentMonthlyRent = currentMonthlyRent;
  }

  public PebScore getCurrentPebScore() {
    return currentPebScore;
  }

  public void setCurrentPebScore(PebScore currentPebScore) {
    this.currentPebScore = currentPebScore;
  }

  public String getActiveLeaseStatus() {
    return activeLeaseStatus;
  }

  public void setActiveLeaseStatus(String activeLeaseStatus) {
    this.activeLeaseStatus = activeLeaseStatus;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getEffectiveOwnerName() {
    return effectiveOwnerName;
  }

  public void setEffectiveOwnerName(String effectiveOwnerName) {
    this.effectiveOwnerName = effectiveOwnerName;
  }

  public Long getRoomCount() {
    return roomCount;
  }

  public void setRoomCount(Long roomCount) {
    this.roomCount = roomCount;
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