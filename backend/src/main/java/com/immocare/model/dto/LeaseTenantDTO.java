package com.immocare.model.dto;
public class LeaseTenantDTO {
    private Long personId;
    private String lastName;
    private String firstName;
    private String email;
    private String gsm;
    private String role;
    public Long getPersonId() { return personId; } public void setPersonId(Long personId) { this.personId = personId; }
    public String getLastName() { return lastName; } public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; } public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getGsm() { return gsm; } public void setGsm(String gsm) { this.gsm = gsm; }
    public String getRole() { return role; } public void setRole(String role) { this.role = role; }
}
