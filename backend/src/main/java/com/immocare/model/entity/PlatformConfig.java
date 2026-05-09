package com.immocare.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Platform-wide configuration entry scoped to an estate.
 * UC004_ESTATE_PLACEHOLDER Phase 5: PK changed from configKey alone to
 * (estate_id, config_key).
 * Each estate holds its own independent set of configuration entries.
 */
@Entity
@Table(name = "platform_config")
@IdClass(EstateConfigId.class)
@Getter
@Setter
public class PlatformConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "value_type", length = 20)
    private String valueType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
