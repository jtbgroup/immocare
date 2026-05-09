package com.immocare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.PlatformConfig;

/**
 * Repository for UC004_ESTATE_PLACEHOLDER Phase 5 — per-estate Platform
 * Configuration.
 * All queries are scoped to an estate via the composite PK (estate_id,
 * config_key).
 */
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig> {

    Optional<PlatformConfig> findByEstateIdAndConfigKey(UUID estateId, String configKey);

    List<PlatformConfig> findByEstateIdOrderByConfigKeyAsc(UUID estateId);

    boolean existsByEstateIdAndConfigKey(UUID estateId, String configKey);
}
