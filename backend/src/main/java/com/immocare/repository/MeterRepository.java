package com.immocare.repository;

import com.immocare.model.entity.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    /**
     * All meters for a given owner (active + closed), newest first.
     * Used for history view (UC009.007).
     */
    List<Meter> findByOwnerTypeAndOwnerIdOrderByStartDateDesc(String ownerType, Long ownerId);

    /**
     * Active meters only (endDate IS NULL).
     * Used for the active meters section (UC009.001, UC009.002).
     */
    List<Meter> findByOwnerTypeAndOwnerIdAndEndDateIsNull(String ownerType, Long ownerId);

    /**
     * Single active meter by id.
     * Used for replace (UC009.005) and remove (UC009.006) operations.
     */
    Optional<Meter> findByIdAndEndDateIsNull(Long id);
}
