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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a room within a housing unit.
 * UC003 - Manage Rooms.
 */
@Entity
@Table(name = "room")
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "housing_unit_id", nullable = false)
  private HousingUnit housingUnit;

  @Column(name = "room_type", nullable = false, length = 20)
  private String roomType;

  @Column(name = "approximate_surface", nullable = false, precision = 6, scale = 2)
  private BigDecimal approximateSurface;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  // ─── Getters / Setters ───────────────────────────────────────────────────────

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public HousingUnit getHousingUnit() { return housingUnit; }
  public void setHousingUnit(HousingUnit housingUnit) { this.housingUnit = housingUnit; }

  public String getRoomType() { return roomType; }
  public void setRoomType(String roomType) { this.roomType = roomType; }

  public BigDecimal getApproximateSurface() { return approximateSurface; }
  public void setApproximateSurface(BigDecimal approximateSurface) {
    this.approximateSurface = approximateSurface;
  }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
