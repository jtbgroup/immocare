package com.immocare.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.PlatformConfigNotFoundException;
import com.immocare.model.dto.PlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.PlatformConfigDTOs.UpdatePlatformConfigRequest;
import com.immocare.model.entity.PlatformConfig;
import com.immocare.repository.PlatformConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for platform-wide configuration.
 *
 * <p>Platform config is global — no estate scope. Mutations are restricted to
 * PLATFORM_ADMIN at the controller level.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository configRepository;

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns all platform config entries, sorted by key.
     */
    public List<PlatformConfigDTO> getAll() {
        return configRepository.findAllByOrderByConfigKeyAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns a single platform config entry by key.
     *
     * @throws PlatformConfigNotFoundException if the key does not exist
     */
    public PlatformConfigDTO getOne(String key) {
        return configRepository.findByConfigKey(key)
                .map(this::toDTO)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
    }

    /**
     * Returns the string value for a platform config key.
     * Falls back to {@code defaultValue} if the key is missing.
     */
    public String getStringValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Returns the integer value for a platform config key.
     * Falls back to {@code defaultValue} if the key is missing or not parseable.
     */
    public int getIntValue(String key, int defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates a single platform config value.
     *
     * @throws PlatformConfigNotFoundException if the key does not exist
     */
    @Transactional
    public PlatformConfigDTO updateOne(String key, UpdatePlatformConfigRequest req) {
        PlatformConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
        config.setConfigValue(req.configValue().trim());
        return toDTO(configRepository.save(config));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PlatformConfigDTO toDTO(PlatformConfig c) {
        return new PlatformConfigDTO(
                c.getConfigKey(),
                c.getConfigValue(),
                c.getValueType(),
                c.getDescription(),
                c.getUpdatedAt());
    }
}
