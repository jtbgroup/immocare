package com.immocare.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link PlatformConfig}.
 * UC004_ESTATE_PLACEHOLDER Phase 5: PK changed from configKey alone to (estate_id, config_key).
 */
public class PlatformConfigId implements Serializable {

    private UUID estate;
    private String configKey;

    public PlatformConfigId() {}

    public PlatformConfigId(UUID estate, String configKey) {
        this.estate = estate;
        this.configKey = configKey;
    }

    public UUID getEstate() { return estate; }
    public void setEstate(UUID estate) { this.estate = estate; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlatformConfigId that)) return false;
        return Objects.equals(estate, that.estate) && Objects.equals(configKey, that.configKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(estate, configKey);
    }
}
