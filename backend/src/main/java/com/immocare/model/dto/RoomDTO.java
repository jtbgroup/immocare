package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Room data.
 * UC003 - Manage Rooms.
 */
public class RoomDTO {

  private Long id;
  private Long housingUnitId;
  private String roomType;
  private BigDecimal approximateSurface;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // ─── Getters / Setters ───────────────────────────────────────────────────────

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getHousingUnitId() { return housingUnitId; }
  public void setHousingUnitId(Long housingUnitId) { this.housingUnitId = housingUnitId; }

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
