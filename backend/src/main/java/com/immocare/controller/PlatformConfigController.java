package com.immocare.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.PlatformConfigDTOs.PlatformConfigDTO;
import com.immocare.model.dto.PlatformConfigDTOs.UpdatePlatformConfigRequest;
import com.immocare.service.PlatformConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for platform-wide configuration.
 *
 * <p>All endpoints are restricted to PLATFORM_ADMIN. Platform config is global
 * and not scoped to any estate. Estate-scoped config is handled by
 * {@link EstateConfigController}.
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/v1/platform-config/settings        — list all entries
 *   GET /api/v1/platform-config/settings/{key}  — get one entry
 *   PUT /api/v1/platform-config/settings/{key}  — update one entry
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/platform-config")
@RequiredArgsConstructor
public class PlatformConfigController {

    private final PlatformConfigService configService;

    /**
     * GET /api/v1/platform-config/settings
     * Returns all platform config entries, sorted by key.
     */
    @GetMapping("/settings")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<PlatformConfigDTO>> getAll() {
        return ResponseEntity.ok(configService.getAll());
    }

    /**
     * GET /api/v1/platform-config/settings/{key}
     * Returns a single platform config entry.
     */
    @GetMapping("/settings/{key}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformConfigDTO> getOne(@PathVariable String key) {
        return ResponseEntity.ok(configService.getOne(key));
    }

    /**
     * PUT /api/v1/platform-config/settings/{key}
     * Updates a single platform config value.
     */
    @PutMapping("/settings/{key}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformConfigDTO> updateOne(
            @PathVariable String key,
            @Valid @RequestBody UpdatePlatformConfigRequest req) {
        return ResponseEntity.ok(configService.updateOne(key, req));
    }
}
