package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PersonDTO {

    private Long id;
    private String lastName;
    private String firstName;
    private LocalDate birthDate;
    private String birthPlace;
    private String nationalId;
    private String gsm;
    private String email;
    private String streetAddress;
    private String postalCode;
    private String city;
    private String country;

    // Derived flags (computed by service, not stored)
    private boolean isOwner;
    private boolean isTenant;

    // Related data (loaded by service)
    private List<OwnedBuildingDTO> ownedBuildings;
    private List<OwnedUnitDTO> ownedUnits;
    private List<TenantLeaseDTO> leases;

    /** IBANs registered for this person, used for transaction reconciliation. */
    private List<PersonBankAccountDTO> bankAccounts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Nested lightweight DTOs ───────────────────────────────────────────────

    public static class OwnedBuildingDTO {
        private Long id;
        private String name;
        private String city;

        public OwnedBuildingDTO() {}
        public OwnedBuildingDTO(Long id, String name, String city) {
            this.id = id; this.name = name; this.city = city;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    public static class OwnedUnitDTO {
        private Long unitId;
        private String unitNumber;
        private Long buildingId;
        private String buildingName;

        public OwnedUnitDTO() {}
        public OwnedUnitDTO(Long unitId, String unitNumber, Long buildingId, String buildingName) {
            this.unitId = unitId; this.unitNumber = unitNumber;
            this.buildingId = buildingId; this.buildingName = buildingName;
        }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public String getUnitNumber() { return unitNumber; }
        public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
        public Long getBuildingId() { return buildingId; }
        public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    }

    public static class TenantLeaseDTO {
        private Long leaseId;
        private Long unitId;
        private String unitNumber;
        private String buildingName;
        private String status;

        public TenantLeaseDTO() {}
        public TenantLeaseDTO(Long leaseId, Long unitId, String unitNumber, String buildingName, String status) {
            this.leaseId = leaseId; this.unitId = unitId; this.unitNumber = unitNumber;
            this.buildingName = buildingName; this.status = status;
        }
        public Long getLeaseId() { return leaseId; }
        public void setLeaseId(Long leaseId) { this.leaseId = leaseId; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public String getUnitNumber() { return unitNumber; }
        public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // ── Main getters & setters ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getGsm() { return gsm; }
    public void setGsm(String gsm) { this.gsm = gsm; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public boolean isTenant() { return isTenant; }
    public void setTenant(boolean tenant) { isTenant = tenant; }

    public List<OwnedBuildingDTO> getOwnedBuildings() { return ownedBuildings; }
    public void setOwnedBuildings(List<OwnedBuildingDTO> ownedBuildings) { this.ownedBuildings = ownedBuildings; }

    public List<OwnedUnitDTO> getOwnedUnits() { return ownedUnits; }
    public void setOwnedUnits(List<OwnedUnitDTO> ownedUnits) { this.ownedUnits = ownedUnits; }

    public List<TenantLeaseDTO> getLeases() { return leases; }
    public void setLeases(List<TenantLeaseDTO> leases) { this.leases = leases; }

    public List<PersonBankAccountDTO> getBankAccounts() { return bankAccounts; }
    public void setBankAccounts(List<PersonBankAccountDTO> bankAccounts) { this.bankAccounts = bankAccounts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
