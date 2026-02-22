package com.immocare.model.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO wrapping the list of rooms for a housing unit,
 * including the computed total surface.
 * UC003 - US016 View Room Composition.
 */
public class RoomListResponse {

  private List<RoomDTO> rooms;
  private BigDecimal totalSurface;

  public RoomListResponse(List<RoomDTO> rooms, BigDecimal totalSurface) {
    this.rooms = rooms;
    this.totalSurface = totalSurface;
  }

  public List<RoomDTO> getRooms() { return rooms; }
  public void setRooms(List<RoomDTO> rooms) { this.rooms = rooms; }

  public BigDecimal getTotalSurface() { return totalSurface; }
  public void setTotalSurface(BigDecimal totalSurface) { this.totalSurface = totalSurface; }
}
