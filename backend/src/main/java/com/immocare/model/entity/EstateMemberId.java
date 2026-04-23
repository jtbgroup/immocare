package com.immocare.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link EstateMember}.
 * UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 1).
 */
public class EstateMemberId implements Serializable {

    private UUID estate;
    private Long user;

    public EstateMemberId() {}

    public EstateMemberId(UUID estate, Long user) {
        this.estate = estate;
        this.user = user;
    }

    public UUID getEstate() { return estate; }
    public void setEstate(UUID estate) { this.estate = estate; }

    public Long getUser() { return user; }
    public void setUser(Long user) { this.user = user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EstateMemberId that)) return false;
        return Objects.equals(estate, that.estate) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(estate, user);
    }
}
