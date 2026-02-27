package com.immocare.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LeaseSummaryDTO {
    private Long id; private String status; private String leaseType;
    private LocalDate startDate; private LocalDate endDate;
    private BigDecimal monthlyRent; private BigDecimal monthlyCharges;
    private BigDecimal totalRent;  // computed
    private String chargesType;
    private List<String> tenantNames;
    private boolean indexationAlertActive; private LocalDate indexationAlertDate;
    private boolean endNoticeAlertActive;  private LocalDate endNoticeAlertDate;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public String getLeaseType() { return leaseType; } public void setLeaseType(String v) { this.leaseType = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { this.endDate = v; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }
    public BigDecimal getTotalRent() { return totalRent; } public void setTotalRent(BigDecimal v) { this.totalRent = v; }
    public String getChargesType() { return chargesType; } public void setChargesType(String v) { this.chargesType = v; }
    public List<String> getTenantNames() { return tenantNames; } public void setTenantNames(List<String> v) { this.tenantNames = v; }
    public boolean isIndexationAlertActive() { return indexationAlertActive; } public void setIndexationAlertActive(boolean v) { this.indexationAlertActive = v; }
    public LocalDate getIndexationAlertDate() { return indexationAlertDate; } public void setIndexationAlertDate(LocalDate v) { this.indexationAlertDate = v; }
    public boolean isEndNoticeAlertActive() { return endNoticeAlertActive; } public void setEndNoticeAlertActive(boolean v) { this.endNoticeAlertActive = v; }
    public LocalDate getEndNoticeAlertDate() { return endNoticeAlertDate; } public void setEndNoticeAlertDate(LocalDate v) { this.endNoticeAlertDate = v; }
}
