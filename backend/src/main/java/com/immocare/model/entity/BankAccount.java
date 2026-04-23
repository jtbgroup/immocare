package com.immocare.model.entity;

import com.immocare.model.enums.BankAccountType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bank account entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: estate_id added — bank accounts are now scoped to an estate.
 */
@Entity
@Table(name = "bank_account")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String label;

    @Column(name = "account_number", nullable = false, length = 50, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BankAccountType type;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Estate this bank account belongs to.
     * UC004_ESTATE_PLACEHOLDER Phase 4 — all bank accounts are scoped to an estate.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BankAccountType getType() { return type; }
    public void setType(BankAccountType type) { this.type = type; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Estate getEstate() { return estate; }
    public void setEstate(Estate estate) { this.estate = estate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
