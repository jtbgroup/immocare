package com.immocare.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.model.dto.PlatformConfigDTOs;
import com.immocare.model.dto.PlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.PlatformConfigDTOs.UpdatePlatformConfigRequest;
import com.immocare.model.entity.PlatformConfig;
import com.immocare.repository.PlatformConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for platform-wide configuration.
 *
 * <p>
 * Platform config is global — no estate scope. Mutations are restricted to
 * PLATFORM_ADMIN at the controller level.
 *
 * <p>
 * Keys are optional: callers should always provide a sensible default via
 * {@link #getStringValue(String, String)} or {@link #getIntValue(String, int)}
 * rather than assuming a key exists.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository configRepository;

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns all platform config entries, sorted by key.
     * Includes both DB entries and default values from the seed that aren't in DB.
     */
    public List<PlatformConfigDTO> getAll() {
        List<PlatformConfigDTO> dbConfigs = configRepository.findAllByOrderByConfigKeyAsc()
                .stream()
                .map(this::toDTO)
                .toList();

        Set<String> existingKeys = dbConfigs.stream()
                .map(PlatformConfigDTO::configKey)
                .collect(Collectors.toSet());

        List<PlatformConfigDTO> allConfigs = new ArrayList<>(dbConfigs);

        // Add default values from the seed that aren't in DB
        PlatformConfigDTOs.DEFAULT_CONFIG.stream()
                .filter(seed -> !existingKeys.contains(seed.key()))
                .forEach(seed -> allConfigs.add(new PlatformConfigDTO(
                        seed.key(),
                        seed.value(),
                        seed.valueType(),
                        "default value",
                        LocalDateTime.now())));

        return allConfigs;
    }

    /**
     * Returns a single platform config entry by key, or {@code null} if absent.
     */
    public PlatformConfigDTO getOne(String key) {
        return configRepository.findByConfigKey(key)
                .map(this::toDTO) // 1. Si trouvé en DB, on transforme en DTO normalement
                .orElseGet(() -> { // 2. Si absent en DB, on entre ici
                    // On cherche la valeur par défaut dans le seed
                    String defaultValue = PlatformConfigDTOs.DEFAULT_CONFIG.stream()
                            .filter(seed -> seed.key().equals(key))
                            .findFirst()
                            .map(PlatformConfigDTOs.PlatformConfigSeed::value)
                            .orElse(null);

                    if (defaultValue != null) {
                        // On fabrique un DTO "virtuel" à la volée
                        PlatformConfigDTO defaultDto = new PlatformConfigDTO(key, defaultValue, "string",
                                "default value", LocalDateTime.now());
                        return defaultDto;
                    }

                    // Si la clé n'existe pas non plus dans l'Enum, on renvoie null (déclenchera le
                    // 404)
                    return null;
                });
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

    // ─── Upsert ───────────────────────────────────────────────────────────────

    /**
     * Creates or updates a platform config entry (upsert).
     *
     * <p>
     * If the key already exists its value is updated. If not, a new entry
     * is created. This makes PUT idempotent regardless of prior state.
     */
    @Transactional
    public PlatformConfigDTO upsert(String key, UpdatePlatformConfigRequest req) {
        PlatformConfig config = configRepository.findByConfigKey(key)
                .orElseGet(() -> {
                    PlatformConfig c = new PlatformConfig();
                    c.setConfigKey(key);
                    c.setValueType("STRING");
                    return c;
                });
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