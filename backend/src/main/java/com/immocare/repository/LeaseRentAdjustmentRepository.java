package com.immocare.repository;

import com.immocare.model.entity.LeaseRentAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LeaseRentAdjustmentRepository extends JpaRepository<LeaseRentAdjustment, Long> {

    List<LeaseRentAdjustment> findByLeaseIdOrderByEffectiveDateDescCreatedAtDesc(Long leaseId);

    /** Used for indexation alert: check if a RENT adjustment already exists this year */
    @Query("SELECT COUNT(a) > 0 FROM LeaseRentAdjustment a " +
           "WHERE a.lease.id = :leaseId AND a.field = 'RENT' " +
           "AND YEAR(a.effectiveDate) = :year")
    boolean existsRentAdjustmentForYear(@Param("leaseId") Long leaseId, @Param("year") int year);
}
