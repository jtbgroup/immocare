package com.immocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.BoilerServiceRecord;

/** Repository for boiler maintenance records — UC012 (UC011.005/UC011.006). */
public interface BoilerServiceRecordRepository extends JpaRepository<BoilerServiceRecord, Long> {

    /** All records for a boiler, newest first (UC011.006). */
    List<BoilerServiceRecord> findByBoilerIdOrderByServiceDateDesc(Long boilerId);

    /** Latest record for validity badge computation (UC011.007). */
    Optional<BoilerServiceRecord> findTopByBoilerIdOrderByServiceDateDesc(Long boilerId);
}
