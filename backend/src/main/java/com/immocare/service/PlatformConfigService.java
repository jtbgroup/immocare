package com.immocare.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.PlatformConfigNotFoundException;
import com.immocare.model.dto.PlatformConfigDTOs.BulkUpdateConfigRequest;
import com.immocare.model.dto.PlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.PlatformConfigDTOs.UpdateConfigRequest;
import com.immocare.model.entity.PlatformConfig;
import com.immocare.repository.PlatformConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for UC012 — Platform Configuration.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository configRepository;

    // ─── READ ────────────────────────────────────────────────────────────────

    public List<PlatformConfigDTO> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::toDTO)
                .sorted((a, b) -> a.configKey().compareTo(b.configKey()))
                .toList();
    }

    public PlatformConfigDTO getConfig(String key) {
        return configRepository.findById(key)
                .map(this::toDTO)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
    }

    /**
     * Returns the integer value for a config key, or the default if not found
     * or not parseable.
     */
    public int getInt(String key, int defaultValue) {
        return configRepository.findById(key)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /** Returns the string value for a config key, or the default if not found. */
    public String getString(String key, String defaultValue) {
        return configRepository.findById(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public PlatformConfigDTO updateConfig(String key, UpdateConfigRequest req) {
        PlatformConfig config = configRepository.findById(key)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
        config.setConfigValue(req.configValue().trim());
        return toDTO(configRepository.save(config));
    }

    @Transactional
    public List<PlatformConfigDTO> bulkUpdate(BulkUpdateConfigRequest req) {
        return req.entries().stream().map(entry -> {
            PlatformConfig config = configRepository.findById(entry.configKey())
                    .orElseThrow(() -> new PlatformConfigNotFoundException(entry.configKey()));
            config.setConfigValue(entry.configValue().trim());
            return toDTO(configRepository.save(config));
        }).toList();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private PlatformConfigDTO toDTO(PlatformConfig c) {
        return new PlatformConfigDTO(c.getConfigKey(), c.getConfigValue(), c.getDescription(), c.getUpdatedAt());
    }
}
