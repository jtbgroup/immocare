package com.immocare.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.PlatformConfig;

/**
 * Repository for UC012 — Platform Configuration.
 */
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, String> {
}
