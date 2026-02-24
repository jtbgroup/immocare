package com.immocare.model.dto;
import jakarta.validation.constraints.NotNull;
public class AddTenantRequest {
    @NotNull private Long personId;
    @NotNull private String role; // PRIMARY, CO_TENANT, GUARANTOR
    public Long getPersonId() { return personId; } public void setPersonId(Long personId) { this.personId = personId; }
    public String getRole() { return role; } public void setRole(String role) { this.role = role; }
}
