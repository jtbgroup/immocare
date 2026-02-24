package com.immocare.model.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public class LeaseSummaryDTO {
    private Long id; private String status; private String leaseType;
    private LocalDate startDate; private LocalDate endDate;
    private BigDecimal monthlyRent; private BigDecimal monthlyCharges; private String chargesType;
    private List<String> tenantNames;
    private boolean indexationAlertActive; private LocalDate indexationAlertDate;
    private boolean endNoticeAlertActive; private LocalDate endNoticeAlertDate;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getLeaseType() { return leaseType; } public void setLeaseType(String s) { this.leaseType = s; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate d) { this.endDate = d; }
    public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { this.monthlyRent = v; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; } public void setMonthlyCharges(BigDecimal v) { this.monthlyCharges = v; }
    public String getChargesType() { return chargesType; } public void setChargesType(String s) { this.chargesType = s; }
    public List<String> getTenantNames() { return tenantNames; } public void setTenantNames(List<String> l) { this.tenantNames = l; }
    public boolean isIndexationAlertActive() { return indexationAlertActive; } public void setIndexationAlertActive(boolean b) { this.indexationAlertActive = b; }
    public LocalDate getIndexationAlertDate() { return indexationAlertDate; } public void setIndexationAlertDate(LocalDate d) { this.indexationAlertDate = d; }
    public boolean isEndNoticeAlertActive() { return endNoticeAlertActive; } public void setEndNoticeAlertActive(boolean b) { this.endNoticeAlertActive = b; }
    public LocalDate getEndNoticeAlertDate() { return endNoticeAlertDate; } public void setEndNoticeAlertDate(LocalDate d) { this.endNoticeAlertDate = d; }
}
