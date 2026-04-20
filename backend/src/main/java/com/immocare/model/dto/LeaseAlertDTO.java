package com.immocare.model.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Alert DTO for lease-level alerts (end notice and indexation).
 * UC016 Phase 6: added boolean flag accessors needed by EstateService dashboard computation.
 */
public class LeaseAlertDTO {
    private Long leaseId;
    private Long housingUnitId;
    private String housingUnitNumber;
    private String buildingName;
    private String alertType; // INDEXATION or END_NOTICE
    private LocalDate deadline;
    private List<String> tenantNames;

    // Phase 6: computed flags set by LeaseService for dashboard aggregation
    private boolean endNoticeAlertActive;
    private boolean indexationAlertActive;

    public Long getLeaseId() { return leaseId; }
    public void setLeaseId(Long leaseId) { this.leaseId = leaseId; }

    public Long getHousingUnitId() { return housingUnitId; }
    public void setHousingUnitId(Long housingUnitId) { this.housingUnitId = housingUnitId; }

    public String getHousingUnitNumber() { return housingUnitNumber; }
    public void setHousingUnitNumber(String housingUnitNumber) { this.housingUnitNumber = housingUnitNumber; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) {
        this.alertType = alertType;
        this.endNoticeAlertActive   = "END_NOTICE".equals(alertType);
        this.indexationAlertActive  = "INDEXATION".equals(alertType);
    }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public List<String> getTenantNames() { return tenantNames; }
    public void setTenantNames(List<String> tenantNames) { this.tenantNames = tenantNames; }

    /** True when this alert represents a lease-end notice deadline. */
    public boolean isEndNoticeAlertActive() { return endNoticeAlertActive; }

    /** True when this alert represents an upcoming rent indexation. */
    public boolean isIndexationAlertActive() { return indexationAlertActive; }
}
