package com.immocare.repository;
import com.immocare.model.entity.LeaseTenant;
import com.immocare.model.entity.LeaseTenantId;
import com.immocare.model.enums.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LeaseTenantRepository extends JpaRepository<LeaseTenant, LeaseTenantId> {
    List<LeaseTenant> findByLeaseId(Long leaseId);
    boolean existsByPersonId(Long personId);
    long countByLeaseIdAndRole(Long leaseId, TenantRole role);
    boolean existsByLeaseIdAndPersonId(Long leaseId, Long personId);
}
