package com.immocare.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.PlatformConfigDTOs.BulkUpdateConfigRequest;
import com.immocare.model.dto.PlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.PlatformConfigDTOs.UpdateConfigRequest;
import com.immocare.service.PlatformConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for UC012 — Platform Configuration.
 */
@RestController
@RequestMapping("/api/v1/platform-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PlatformConfigController {

    private final PlatformConfigService configService;

    /** GET /api/v1/platform-config → all settings */
    @GetMapping
    public ResponseEntity<List<PlatformConfigDTO>> getAll() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    /** GET /api/v1/platform-config/{key} */
    @GetMapping("/{key}")
    public ResponseEntity<PlatformConfigDTO> getOne(@PathVariable String key) {
        return ResponseEntity.ok(configService.getConfig(key));
    }

    /** PATCH /api/v1/platform-config/{key} — update a single value */
    @PatchMapping("/{key}")
    public ResponseEntity<PlatformConfigDTO> updateOne(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest req) {
        return ResponseEntity.ok(configService.updateConfig(key, req));
    }

    /** PUT /api/v1/platform-config — bulk update */
    @PutMapping
    public ResponseEntity<List<PlatformConfigDTO>> bulkUpdate(
            @Valid @RequestBody BulkUpdateConfigRequest req) {
        return ResponseEntity.ok(configService.bulkUpdate(req));
    }
}
