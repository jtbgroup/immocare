package com.immocare.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.Boiler;

/**
 * Repository for UC011 — Manage Boilers.
 */
public interface BoilerRepository extends JpaRepository<Boiler, Long> {

    /** All boilers for a given owner (housing unit or building), newest first. */
    List<Boiler> findByOwnerTypeAndOwnerIdOrderByInstallationDateDesc(String ownerType, Long ownerId);

    /** Boilers whose next service date is on or before the given date (for alerts). */
    @Query("SELECT b FROM Boiler b WHERE b.nextServiceDate IS NOT NULL AND b.nextServiceDate <= :threshold")
    List<Boiler> findBoilersWithServiceDueBefore(@Param("threshold") LocalDate threshold);
}
