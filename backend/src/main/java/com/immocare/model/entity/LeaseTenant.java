package com.immocare.model.entity;
import com.immocare.model.enums.TenantRole;
import jakarta.persistence.*;

@Entity
@Table(name = "lease_tenant")
@IdClass(LeaseTenantId.class)
public class LeaseTenant {
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lease_id") private Lease lease;
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "person_id") private Person person;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TenantRole role = TenantRole.PRIMARY;
    public LeaseTenant() {}
    public LeaseTenant(Lease lease, Person person, TenantRole role) { this.lease = lease; this.person = person; this.role = role; }
    public Lease getLease() { return lease; } public void setLease(Lease lease) { this.lease = lease; }
    public Person getPerson() { return person; } public void setPerson(Person person) { this.person = person; }
    public TenantRole getRole() { return role; } public void setRole(TenantRole role) { this.role = role; }
}
