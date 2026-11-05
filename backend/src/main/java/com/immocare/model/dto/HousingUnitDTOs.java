package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.immocare.model.entity.PebScore;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs for housing units, grouped in the project's holder-record pattern.
 */
public final class HousingUnitDTOs {

    private HousingUnitDTOs() {
    }

    public record HousingUnitDTO(
            Long id,
            Long buildingId,
            String buildingName,
            String unitNumber,
            Integer floor,
            String landingNumber,
            BigDecimal totalSurface,
            Boolean hasTerrace,
            BigDecimal terraceSurface,
            String terraceOrientation,
            Boolean hasGarden,
            BigDecimal gardenSurface,
            String gardenOrientation,
            BigDecimal currentMonthlyRent,
            PebScore currentPebScore,
            String activeLeaseStatus,
            Long ownerId,
            String ownerName,
            String effectiveOwnerName,
            Long roomCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateHousingUnitRequest(
            @NotNull(message = "Building ID is required") Long buildingId,
            @NotBlank(message = "Unit number is required")
            @Size(max = 20, message = "Unit number must be 20 characters or less")
            String unitNumber,
            @NotNull(message = "Floor is required")
            @Min(value = -10, message = "Floor must be between -10 and 100")
            @Max(value = 100, message = "Floor must be between -10 and 100")
            Integer floor,
            @Size(max = 10, message = "Landing number must be 10 characters or less")
            String landingNumber,
            @DecimalMin(value = "0.01", message = "Total surface must be greater than 0")
            BigDecimal totalSurface,
            Long ownerId,
            Boolean hasTerrace,
            @DecimalMin(value = "0.01", message = "Terrace surface must be greater than 0")
            BigDecimal terraceSurface,
            @Pattern(regexp = "^$|^(N|S|E|W|NE|NW|SE|SW)$", message = "Terrace orientation must be N, S, E, W, NE, NW, SE or SW")
            String terraceOrientation,
            Boolean hasGarden,
            @DecimalMin(value = "0.01", message = "Garden surface must be greater than 0")
            BigDecimal gardenSurface,
            @Pattern(regexp = "^$|^(N|S|E|W|NE|NW|SE|SW)$", message = "Garden orientation must be N, S, E, W, NE, NW, SE or SW")
            String gardenOrientation) {

        public CreateHousingUnitRequest {
            if (hasTerrace == null) {
                hasTerrace = false;
            }
            if (hasGarden == null) {
                hasGarden = false;
            }
        }
    }

    public record UpdateHousingUnitRequest(
            @NotBlank(message = "Unit number is required")
            @Size(max = 20, message = "Unit number must be 20 characters or less")
            String unitNumber,
            @NotNull(message = "Floor is required")
            @Min(value = -10, message = "Floor must be between -10 and 100")
            @Max(value = 100, message = "Floor must be between -10 and 100")
            Integer floor,
            @Size(max = 10, message = "Landing number must be 10 characters or less")
            String landingNumber,
            @DecimalMin(value = "0.01", message = "Total surface must be greater than 0")
            BigDecimal totalSurface,
            Long ownerId,
            Boolean hasTerrace,
            @DecimalMin(value = "0.01", message = "Terrace surface must be greater than 0")
            BigDecimal terraceSurface,
            @Pattern(regexp = "^$|^(N|S|E|W|NE|NW|SE|SW)$", message = "Terrace orientation must be N, S, E, W, NE, NW, SE or SW")
            String terraceOrientation,
            Boolean hasGarden,
            @DecimalMin(value = "0.01", message = "Garden surface must be greater than 0")
            BigDecimal gardenSurface,
            @Pattern(regexp = "^$|^(N|S|E|W|NE|NW|SE|SW)$", message = "Garden orientation must be N, S, E, W, NE, NW, SE or SW")
            String gardenOrientation) {

        public UpdateHousingUnitRequest {
            if (hasTerrace == null) {
                hasTerrace = false;
            }
            if (hasGarden == null) {
                hasGarden = false;
            }
        }
    }
}
