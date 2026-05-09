package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.EstateConfigDTOs.AddBoilerServiceValidityRuleRequest;
import com.immocare.model.dto.EstateConfigDTOs.AssetTypeMappingDTO;
import com.immocare.model.dto.EstateConfigDTOs.BoilerServiceValidityRuleDTO;
import com.immocare.model.dto.EstateConfigDTOs.EstateConfigDTO;
import com.immocare.model.dto.EstateConfigDTOs.UpdateAssetTypeMappingRequest;
import com.immocare.model.dto.EstateConfigDTOs.UpdateEstateConfigRequest;
import com.immocare.service.BoilerServiceValidityRuleService;
import com.immocare.service.EstateConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for per-estate platform configuration and boiler validity
 * rules.
 * UC004_ESTATE_PLACEHOLDER Phase 5 — replaces the global
 * PlatformConfigController for all estate-scoped routes.
 *
 * All endpoints are under /api/v1/estates/{estateId}/config/.
 * VIEWER access on GET endpoints; MANAGER required for mutations.
 *
 * Endpoints:
 * GET /api/v1/estates/{estateId}/config/settings → UC013.001
 * PUT /api/v1/estates/{estateId}/config/settings/{key} → UC013.004
 * GET /api/v1/estates/{estateId}/config/boiler-validity-rules → UC013.003
 * POST /api/v1/estates/{estateId}/config/boiler-validity-rules → UC013.002
 * GET /api/v1/estates/{estateId}/config/asset-type-mappings → UC012.001
 * PUT /api/v1/estates/{estateId}/config/asset-type-mappings/{assetType} →
 * UC012.001
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/config")
@RequiredArgsConstructor
public class EstateConfigController {

    private final EstateConfigService configService;
    private final BoilerServiceValidityRuleService validityRuleService;

    // ─── Platform Settings ────────────────────────────────────────────────────

    /**
     * GET /api/v1/estates/{estateId}/config/settings
     * Returns all config entries for the estate.
     */
    @GetMapping("/settings")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<EstateConfigDTO>> getAllSettings(
            @PathVariable UUID estateId) {
        return ResponseEntity.ok(configService.getAllConfigs(estateId));
    }

    /**
     * GET /api/v1/estates/{estateId}/config/settings/{key}
     * Returns a single config entry.
     */
    @GetMapping("/settings/{key}")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<EstateConfigDTO> getSetting(
            @PathVariable UUID estateId,
            @PathVariable String key) {
        return ResponseEntity.ok(configService.getConfig(estateId, key));
    }

    /**
     * PUT /api/v1/estates/{estateId}/config/settings/{key}
     * Updates a single config value.
     */
    @PutMapping("/settings/{key}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<EstateConfigDTO> updateSetting(
            @PathVariable UUID estateId,
            @PathVariable String key,
            @Valid @RequestBody UpdateEstateConfigRequest req) {
        return ResponseEntity.ok(configService.updateConfig(estateId, key, req));
    }

    // ─── Boiler Service Validity Rules ────────────────────────────────────────

    /**
     * GET /api/v1/estates/{estateId}/config/boiler-validity-rules
     * Returns all validity rules for the estate, newest first.
     */
    @GetMapping("/boiler-validity-rules")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<BoilerServiceValidityRuleDTO>> getValidityRules(
            @PathVariable UUID estateId) {
        return ResponseEntity.ok(validityRuleService.getAllRules(estateId));
    }

    /**
     * POST /api/v1/estates/{estateId}/config/boiler-validity-rules
     * Adds a new validity rule for the estate.
     * Rules are append-only (never modified or deleted).
     */
    @PostMapping("/boiler-validity-rules")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<BoilerServiceValidityRuleDTO> addValidityRule(
            @PathVariable UUID estateId,
            @Valid @RequestBody AddBoilerServiceValidityRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(validityRuleService.addRule(estateId, req));
    }

    // ─── Asset Type Mappings ──────────────────────────────────────────────────

    /**
     * GET /api/v1/estates/{estateId}/config/asset-type-mappings
     * Returns all asset-type → subcategory mappings for the estate.
     */
    @GetMapping("/asset-type-mappings")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<AssetTypeMappingDTO>> getAssetTypeMappings(
            @PathVariable UUID estateId) {
        return ResponseEntity.ok(configService.getAssetTypeMappings(estateId));
    }

    /**
     * PUT /api/v1/estates/{estateId}/config/asset-type-mappings/{assetType}
     * Updates the subcategory mapping for the given asset type.
     * Pass null subcategoryId to clear the mapping.
     */
    @PutMapping("/asset-type-mappings/{assetType}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<AssetTypeMappingDTO> updateAssetTypeMapping(
            @PathVariable UUID estateId,
            @PathVariable String assetType,
            @RequestBody UpdateAssetTypeMappingRequest req) {
        return ResponseEntity.ok(
                configService.updateAssetTypeMapping(estateId, assetType, req));
    }
}
