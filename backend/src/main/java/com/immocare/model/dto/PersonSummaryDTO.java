package com.immocare.model.dto;

public class PersonSummaryDTO {

    private Long id;
    private String lastName;
    private String firstName;
    private String city;
    private String nationalId;
    private boolean isOwner;
    private boolean isTenant;

    public PersonSummaryDTO() {}

    public PersonSummaryDTO(Long id, String lastName, String firstName,
                             String city, String nationalId,
                             boolean isOwner, boolean isTenant) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.city = city;
        this.nationalId = nationalId;
        this.isOwner = isOwner;
        this.isTenant = isTenant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public boolean isTenant() { return isTenant; }
    public void setTenant(boolean tenant) { isTenant = tenant; }

    /** Convenience for display: "Dupont Jean — Brussels (nationalId)" */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder(lastName).append(" ").append(firstName);
        if (city != null && !city.isBlank()) sb.append(" — ").append(city);
        if (nationalId != null && !nationalId.isBlank()) sb.append(" (").append(nationalId).append(")");
        return sb.toString();
    }
}
