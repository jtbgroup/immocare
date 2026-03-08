package com.immocare.repository;

import com.immocare.model.entity.LeaseTenant;
import com.immocare.model.entity.LeaseTenantId;
import com.immocare.model.enums.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaseTenantRepository extends JpaRepository<LeaseTenant, LeaseTenantId> {

    List<LeaseTenant> findByLeaseId(Long leaseId);

    /** All lease memberships for a given person, with lease and unit eagerly loaded. */
    @Query("""
        SELECT lt FROM LeaseTenant lt
        JOIN FETCH lt.lease l
        JOIN FETCH l.housingUnit u
        JOIN FETCH u.building
        WHERE lt.person.id = :personId
        """)
    List<LeaseTenant> findByPersonId(@Param("personId") Long personId);

    boolean existsByPersonId(Long personId);

    long countByLeaseIdAndRole(Long leaseId, TenantRole role);

    boolean existsByLeaseIdAndPersonId(Long leaseId, Long personId);
}
