package com.immocare.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Request DTO for creating a single room.
 * UC003 - US012 Add Room to Housing Unit.
 */
public class CreateRoomRequest {

  @NotBlank(message = "Room type is required")
  @Pattern(
      regexp = "LIVING_ROOM|BEDROOM|KITCHEN|BATHROOM|TOILET|HALLWAY|STORAGE|OFFICE|DINING_ROOM|OTHER",
      message = "Invalid room type"
  )
  private String roomType;

  @NotNull(message = "Surface is required")
  @DecimalMin(value = "0.01", message = "Surface must be greater than 0")
  @DecimalMax(value = "999.99", message = "Surface must be less than 1000 m²")
  private BigDecimal approximateSurface;

  // ─── Getters / Setters ───────────────────────────────────────────────────────

  public String getRoomType() { return roomType; }
  public void setRoomType(String roomType) { this.roomType = roomType; }

  public BigDecimal getApproximateSurface() { return approximateSurface; }
  public void setApproximateSurface(BigDecimal approximateSurface) {
    this.approximateSurface = approximateSurface;
  }
}
