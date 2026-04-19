package com.immocare.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.PlatformConfigNotFoundException;
import com.immocare.model.dto.EstatePlatformConfigDTOs.AssetTypeMappingDTO;
import com.immocare.model.dto.EstatePlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.EstatePlatformConfigDTOs.UpdateAssetTypeMappingRequest;
import com.immocare.model.dto.EstatePlatformConfigDTOs.UpdatePlatformConfigRequest;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.PlatformConfig;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.PlatformConfigRepository;
import com.immocare.repository.TagSubcategoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for UC016 Phase 5 — per-estate Platform Configuration.
 * All operations require an estateId — there are no global config reads.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private static final String ASSET_MAPPING_PREFIX = "asset.type.subcategory.mapping.";
    private static final List<String> ASSET_TYPES = List.of("BOILER", "FIRE_EXTINGUISHER", "METER");

    private final PlatformConfigRepository configRepository;
    private final EstateRepository estateRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Returns all config entries for the given estate, sorted by key.
     */
    public List<PlatformConfigDTO> getAllConfigs(UUID estateId) {
        return configRepository.findByEstateIdOrderByConfigKeyAsc(estateId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns a single config entry for the given estate and key.
     */
    public PlatformConfigDTO getConfig(UUID estateId, String key) {
        return configRepository.findByEstateIdAndConfigKey(estateId, key)
                .map(this::toDTO)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
    }

    /**
     * Returns the integer value for a config key within an estate.
     * Falls back to defaultValue if the key is missing or not parseable.
     */
    public int getIntValue(UUID estateId, String key, int defaultValue) {
        return configRepository.findByEstateIdAndConfigKey(estateId, key)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Returns the string value for a config key within an estate.
     * Falls back to defaultValue if the key is missing.
     */
    public String getStringValue(UUID estateId, String key, String defaultValue) {
        return configRepository.findByEstateIdAndConfigKey(estateId, key)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Returns all asset-type → subcategory mappings for the given estate.
     */
    public List<AssetTypeMappingDTO> getAssetTypeMappings(UUID estateId) {
        return ASSET_TYPES.stream()
                .map(assetType -> {
                    String key = ASSET_MAPPING_PREFIX + assetType;
                    String value = getStringValue(estateId, key, "");
                    Long subcategoryId = parseSubcategoryId(value);
                    String subcategoryName = resolveSubcategoryName(subcategoryId);
                    return new AssetTypeMappingDTO(assetType, subcategoryId, subcategoryName);
                })
                .toList();
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Updates a single config value for the given estate and key.
     */
    @Transactional
    public PlatformConfigDTO updateConfig(UUID estateId, String key, UpdatePlatformConfigRequest req) {
        PlatformConfig config = configRepository.findByEstateIdAndConfigKey(estateId, key)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
        config.setConfigValue(req.configValue().trim());
        return toDTO(configRepository.save(config));
    }

    /**
     * Updates the subcategory mapping for a given asset type within an estate.
     * A null subcategoryId clears the mapping (stored as empty string).
     */
    @Transactional
    public AssetTypeMappingDTO updateAssetTypeMapping(UUID estateId, String assetType,
            UpdateAssetTypeMappingRequest req) {
        if (!ASSET_TYPES.contains(assetType.toUpperCase())) {
            throw new IllegalArgumentException("Invalid asset type: " + assetType
                    + ". Allowed: " + ASSET_TYPES);
        }
        String key = ASSET_MAPPING_PREFIX + assetType.toUpperCase();
        String newValue = req.subcategoryId() != null ? String.valueOf(req.subcategoryId()) : "";

        PlatformConfig config = configRepository.findByEstateIdAndConfigKey(estateId, key)
                .orElseThrow(() -> new PlatformConfigNotFoundException(key));
        config.setConfigValue(newValue);
        configRepository.save(config);

        Long subcategoryId = parseSubcategoryId(newValue);
        String subcategoryName = resolveSubcategoryName(subcategoryId);
        return new AssetTypeMappingDTO(assetType.toUpperCase(), subcategoryId, subcategoryName);
    }

    // ─── SEED (called by EstateService at creation) ───────────────────────────

    /**
     * Seeds default config entries for a newly created estate.
     * Called within the same @Transactional as EstateService.createEstate().
     */
    @Transactional
    public void seedDefaultConfig(Estate estate,
            List<com.immocare.model.dto.EstatePlatformConfigDTOs.PlatformConfigSeed> seeds) {
        for (var seed : seeds) {
            PlatformConfig config = new PlatformConfig();
            config.setEstate(estate);
            config.setConfigKey(seed.key());
            config.setConfigValue(seed.value());
            config.setValueType(seed.valueType());
            config.setDescription(seed.description());
            configRepository.save(config);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PlatformConfigDTO toDTO(PlatformConfig c) {
        return new PlatformConfigDTO(
                c.getEstate().getId(),
                c.getConfigKey(),
                c.getConfigValue(),
                c.getValueType(),
                c.getDescription(),
                c.getUpdatedAt());
    }

    private Long parseSubcategoryId(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveSubcategoryName(Long subcategoryId) {
        if (subcategoryId == null)
            return null;
        return tagSubcategoryRepository.findById(subcategoryId)
                .map(s -> s.getName())
                .orElse(null);
    }
}
