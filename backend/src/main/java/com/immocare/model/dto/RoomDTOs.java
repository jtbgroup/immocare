package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs for room management, grouped in the project's holder-record pattern.
 */
public final class RoomDTOs {

    private RoomDTOs() {
    }

    public record RoomDTO(
            Long id,
            Long housingUnitId,
            String roomType,
            BigDecimal approximateSurface,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateRoomRequest(
            @NotBlank(message = "Room type is required")
            @Pattern(
                    regexp = "LIVING_ROOM|BEDROOM|KITCHEN|BATHROOM|TOILET|HALLWAY|STORAGE|OFFICE|DINING_ROOM|OTHER",
                    message = "Invalid room type")
            String roomType,
            @NotNull(message = "Surface is required")
            @DecimalMin(value = "0.01", message = "Surface must be greater than 0")
            @DecimalMax(value = "999.99", message = "Surface must be less than 1000 m²")
            BigDecimal approximateSurface) {
    }

    public record UpdateRoomRequest(
            @NotBlank(message = "Room type is required")
            @Pattern(
                    regexp = "LIVING_ROOM|BEDROOM|KITCHEN|BATHROOM|TOILET|HALLWAY|STORAGE|OFFICE|DINING_ROOM|OTHER",
                    message = "Invalid room type")
            String roomType,
            @NotNull(message = "Surface is required")
            @DecimalMin(value = "0.01", message = "Surface must be greater than 0")
            @DecimalMax(value = "999.99", message = "Surface must be less than 1000 m²")
            BigDecimal approximateSurface) {
    }

    public record BatchCreateRoomsRequest(
            @NotEmpty(message = "At least one room entry is required")
            @Size(max = 20, message = "Cannot batch-create more than 20 rooms at once")
            @Valid List<RoomEntry> rooms) {
    }

    public record RoomEntry(
            @NotBlank(message = "Room type is required")
            @Pattern(
                    regexp = "LIVING_ROOM|BEDROOM|KITCHEN|BATHROOM|TOILET|HALLWAY|STORAGE|OFFICE|DINING_ROOM|OTHER",
                    message = "Invalid room type")
            String roomType,
            @NotNull(message = "Surface is required")
            @DecimalMin(value = "0.01", message = "Surface must be greater than 0")
            @DecimalMax(value = "999.99", message = "Surface must be less than 1000 m²")
            BigDecimal approximateSurface) {
    }

    public record RoomListResponse(List<RoomDTO> rooms, BigDecimal totalSurface) {
    }
}
