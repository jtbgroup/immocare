package com.immocare.model.entity;

import java.time.LocalDateTime;

import com.immocare.model.enums.EstateRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Join table entity between {@link Estate} and {@link AppUser}.
 * Each row grants a user a specific role (MANAGER or VIEWER) within an estate.
 * UC016 — Manage Estates (Phase 1).
 */
@Entity
@Table(name = "estate_member")
@IdClass(EstateMemberId.class)
public class EstateMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstateRole role = EstateRole.VIEWER;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Estate getEstate() {
        return estate;
    }

    public void setEstate(Estate estate) {
        this.estate = estate;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public EstateRole getRole() {
        return role;
    }

    public void setRole(EstateRole role) {
        this.role = role;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }
}
