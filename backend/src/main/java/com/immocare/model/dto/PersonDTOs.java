package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * DTOs for person management, grouped in the project's holder-record pattern.
 */
public final class PersonDTOs {

    private PersonDTOs() {
    }

    public record PersonDTO(
            Long id,
            String lastName,
            String firstName,
            LocalDate birthDate,
            String birthPlace,
            String nationalId,
            String gsm,
            String email,
            String streetAddress,
            String postalCode,
            String city,
            String country,
            boolean isOwner,
            boolean isTenant,
            List<OwnedBuildingDTO> ownedBuildings,
            List<OwnedUnitDTO> ownedUnits,
            List<TenantLeaseDTO> leases,
            List<PersonBankAccountDTO> bankAccounts,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record PersonSummaryDTO(
            Long id,
            String lastName,
            String firstName,
            String city,
            String nationalId,
            boolean isOwner,
            boolean isTenant) {

        public String getDisplayLabel() {
            StringBuilder sb = new StringBuilder(lastName).append(" ").append(firstName);
            if (city != null && !city.isBlank()) {
                sb.append(" — ").append(city);
            }
            if (nationalId != null && !nationalId.isBlank()) {
                sb.append(" (").append(nationalId).append(")");
            }
            return sb.toString();
        }
    }

    public record OwnedBuildingDTO(Long id, String name, String city) {
    }

    public record OwnedUnitDTO(Long unitId, String unitNumber, Long buildingId, String buildingName) {
    }

    public record TenantLeaseDTO(Long leaseId, Long unitId, String unitNumber, String buildingName, String status) {
    }

    public record CreatePersonRequest(
            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must not exceed 100 characters")
            String lastName,

            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must not exceed 100 characters")
            String firstName,

            @PastOrPresent(message = "Birth date cannot be in the future")
            LocalDate birthDate,

            @Size(max = 100, message = "Birth place must not exceed 100 characters")
            String birthPlace,

            @Size(max = 20, message = "National ID must not exceed 20 characters")
            String nationalId,

            @Size(max = 20, message = "GSM must not exceed 20 characters")
            String gsm,

            @Email(message = "Email must be a valid email address")
            @Size(max = 100, message = "Email must not exceed 100 characters")
            String email,

            @Size(max = 200, message = "Street address must not exceed 200 characters")
            String streetAddress,

            @Size(max = 20, message = "Postal code must not exceed 20 characters")
            String postalCode,

            @Size(max = 100, message = "City must not exceed 100 characters")
            String city,

            @Size(max = 100, message = "Country must not exceed 100 characters")
            String country) {
    }

    public record UpdatePersonRequest(
            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must not exceed 100 characters")
            String lastName,

            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must not exceed 100 characters")
            String firstName,

            @PastOrPresent(message = "Birth date cannot be in the future")
            LocalDate birthDate,

            @Size(max = 100, message = "Birth place must not exceed 100 characters")
            String birthPlace,

            @Size(max = 20, message = "National ID must not exceed 20 characters")
            String nationalId,

            @Size(max = 20, message = "GSM must not exceed 20 characters")
            String gsm,

            @Email(message = "Email must be a valid email address")
            @Size(max = 100, message = "Email must not exceed 100 characters")
            String email,

            @Size(max = 200, message = "Street address must not exceed 200 characters")
            String streetAddress,

            @Size(max = 20, message = "Postal code must not exceed 20 characters")
            String postalCode,

            @Size(max = 100, message = "City must not exceed 100 characters")
            String city,

            @Size(max = 100, message = "Country must not exceed 100 characters")
            String country) {
    }
}
