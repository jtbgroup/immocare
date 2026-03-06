package com.immocare.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_month_rule")
public class AccountingMonthRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private TagSubcategory subcategory;

    @Column(name = "counterparty_account", length = 50)
    private String counterpartyAccount;

    @Column(name = "month_offset", nullable = false)
    private int monthOffset = 0;

    @Column(nullable = false)
    private int confidence = 1;

    @Column(name = "last_matched_at")
    private LocalDateTime lastMatchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TagSubcategory getSubcategory() { return subcategory; }
    public void setSubcategory(TagSubcategory subcategory) { this.subcategory = subcategory; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    public int getMonthOffset() { return monthOffset; }
    public void setMonthOffset(int monthOffset) { this.monthOffset = monthOffset; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public LocalDateTime getLastMatchedAt() { return lastMatchedAt; }
    public void setLastMatchedAt(LocalDateTime lastMatchedAt) { this.lastMatchedAt = lastMatchedAt; }
}
