package com.immocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.BoilerServiceRecord;

/** Repository for boiler maintenance records — UC011 (US064/US065). */
public interface BoilerServiceRecordRepository extends JpaRepository<BoilerServiceRecord, Long> {

    /** All records for a boiler, newest first (US065). */
    List<BoilerServiceRecord> findByBoilerIdOrderByServiceDateDesc(Long boilerId);

    /** Latest record for validity badge computation (US066). */
    Optional<BoilerServiceRecord> findTopByBoilerIdOrderByServiceDateDesc(Long boilerId);
}
