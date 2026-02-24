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
     * Used for history view (US042).
     */
    List<Meter> findByOwnerTypeAndOwnerIdOrderByStartDateDesc(String ownerType, Long ownerId);

    /**
     * Active meters only (endDate IS NULL).
     * Used for the active meters section (US036, US037).
     */
    List<Meter> findByOwnerTypeAndOwnerIdAndEndDateIsNull(String ownerType, Long ownerId);

    /**
     * Single active meter by id.
     * Used for replace (US040) and remove (US041) operations.
     */
    Optional<Meter> findByIdAndEndDateIsNull(Long id);
}
