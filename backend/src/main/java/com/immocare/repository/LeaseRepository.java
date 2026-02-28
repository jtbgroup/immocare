package com.immocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.immocare.model.entity.Lease;
import com.immocare.model.enums.LeaseStatus;

public interface LeaseRepository extends JpaRepository<Lease, Long>, JpaSpecificationExecutor<Lease> {

    List<Lease> findByHousingUnitIdOrderByStartDateDesc(Long housingUnitId);

    boolean existsByHousingUnitIdAndStatusIn(Long housingUnitId, List<LeaseStatus> statuses);

    boolean existsByHousingUnitIdAndStatusInAndIdNot(Long housingUnitId, List<LeaseStatus> statuses, Long excludeId);

    Optional<Lease> findFirstByHousingUnitIdAndStatus(Long housingUnitId, LeaseStatus status);

    List<Lease> findByStatusIn(List<LeaseStatus> statuses);

    @Query("SELECT l FROM Lease l JOIN FETCH l.tenants t JOIN FETCH t.person WHERE l.status = 'ACTIVE'")
    List<Lease> findAllActiveWithTenants();

    /**
     * Paginated global list — delegates to JpaSpecificationExecutor.findAll(spec,
     * pageable).
     * Declared here for documentation purposes; the actual method is inherited.
     *
     * Usage: leaseRepository.findAll(LeaseSpecification.of(params), pageable)
     */
    Page<Lease> findAll(org.springframework.data.jpa.domain.Specification<Lease> spec, Pageable pageable);
}