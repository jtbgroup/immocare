package com.immocare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.EstateConfig;
import com.immocare.model.entity.EstateConfigId;

/**
 * Repository for UC004_ESTATE_PLACEHOLDER Phase 5 — per-estate Platform
 * Configuration.
 * All queries are scoped to an estate via the composite PK (estate_id,
 * config_key).
 */
public interface EstateConfigRepository extends JpaRepository<EstateConfig, EstateConfigId> {

    Optional<EstateConfig> findByEstateIdAndConfigKey(UUID estateId, String configKey);

    List<EstateConfig> findByEstateIdOrderByConfigKeyAsc(UUID estateId);

    boolean existsByEstateIdAndConfigKey(UUID estateId, String configKey);
}
