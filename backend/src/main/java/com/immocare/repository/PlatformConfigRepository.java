package com.immocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.PlatformConfig;

/**
 * Repository for platform-wide configuration entries.
 *
 * <p>All entries are global (no estate scope). Only PLATFORM_ADMIN
 * may mutate these values via {@link com.immocare.controller.PlatformConfigController}.
 */
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, String> {

    /**
     * Returns all platform config entries sorted alphabetically by key.
     */
    List<PlatformConfig> findAllByOrderByConfigKeyAsc();

    /**
     * Looks up a single entry by its key.
     */
    Optional<PlatformConfig> findByConfigKey(String configKey);

    /**
     * Returns {@code true} if an entry exists for the given key.
     */
    boolean existsByConfigKey(String configKey);
}
