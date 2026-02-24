package com.immocare.model.dto;
import java.time.LocalDate;
import java.util.List;
public class LeaseAlertDTO {
    private Long leaseId;
    private Long housingUnitId;
    private String housingUnitNumber;
    private String buildingName;
    private String alertType; // INDEXATION or END_NOTICE
    private LocalDate deadline;
    private List<String> tenantNames;
    public Long getLeaseId() { return leaseId; } public void setLeaseId(Long leaseId) { this.leaseId = leaseId; }
    public Long getHousingUnitId() { return housingUnitId; } public void setHousingUnitId(Long housingUnitId) { this.housingUnitId = housingUnitId; }
    public String getHousingUnitNumber() { return housingUnitNumber; } public void setHousingUnitNumber(String housingUnitNumber) { this.housingUnitNumber = housingUnitNumber; }
    public String getBuildingName() { return buildingName; } public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getAlertType() { return alertType; } public void setAlertType(String alertType) { this.alertType = alertType; }
    public LocalDate getDeadline() { return deadline; } public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public List<String> getTenantNames() { return tenantNames; } public void setTenantNames(List<String> tenantNames) { this.tenantNames = tenantNames; }
}
