package com.immocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.immocare.model.entity.Lease;
import com.immocare.model.enums.LeaseStatus;

public interface LeaseRepository extends JpaRepository<Lease, Long> {
    List<Lease> findByHousingUnitIdOrderByStartDateDesc(Long housingUnitId);

    boolean existsByHousingUnitIdAndStatusIn(Long housingUnitId, List<LeaseStatus> statuses);

    boolean existsByHousingUnitIdAndStatusInAndIdNot(Long housingUnitId, List<LeaseStatus> statuses, Long excludeId);

    Optional<Lease> findFirstByHousingUnitIdAndStatus(Long housingUnitId, LeaseStatus status);

    List<Lease> findByStatusIn(List<LeaseStatus> statuses);

    @Query("SELECT l FROM Lease l JOIN FETCH l.tenants t JOIN FETCH t.person WHERE l.status = 'ACTIVE'")
    List<Lease> findAllActiveWithTenants();
}