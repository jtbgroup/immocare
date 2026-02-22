package com.immocare.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for batch-creating multiple rooms at once.
 * UC003 - US015 Quick Add Multiple Rooms.
 */
public class BatchCreateRoomsRequest {

  @NotEmpty(message = "At least one room entry is required")
  @Size(max = 20, message = "Cannot batch-create more than 20 rooms at once")
  @Valid
  private List<RoomEntry> rooms;

  // ─── Inner class ─────────────────────────────────────────────────────────────

  public static class RoomEntry {

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

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public BigDecimal getApproximateSurface() { return approximateSurface; }
    public void setApproximateSurface(BigDecimal approximateSurface) {
      this.approximateSurface = approximateSurface;
    }
  }

  // ─── Getters / Setters ───────────────────────────────────────────────────────

  public List<RoomEntry> getRooms() { return rooms; }
  public void setRooms(List<RoomEntry> rooms) { this.rooms = rooms; }
}
