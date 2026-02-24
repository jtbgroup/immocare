package com.immocare.model.entity;
import java.io.Serializable;
import java.util.Objects;

public class LeaseTenantId implements Serializable {
    private Long lease;
    private Long person;
    public LeaseTenantId() {}
    public LeaseTenantId(Long lease, Long person) { this.lease = lease; this.person = person; }
    public Long getLease() { return lease; }
    public void setLease(Long lease) { this.lease = lease; }
    public Long getPerson() { return person; }
    public void setPerson(Long person) { this.person = person; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof LeaseTenantId)) return false; LeaseTenantId that = (LeaseTenantId) o; return Objects.equals(lease, that.lease) && Objects.equals(person, that.person); }
    @Override public int hashCode() { return Objects.hash(lease, person); }
}
