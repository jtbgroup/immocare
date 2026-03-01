package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs and request types for UC012 — Platform Configuration.
 */
public final class PlatformConfigDTOs {

    private PlatformConfigDTOs() {}

    // ─── Response ────────────────────────────────────────────────────────────

    public record PlatformConfigDTO(
            String configKey,
            String configValue,
            String description,
            LocalDateTime updatedAt
    ) {}

    // ─── Requests ────────────────────────────────────────────────────────────

    /** Update a single config entry. */
    public record UpdateConfigRequest(
            @NotBlank(message = "Value is required")
            String configValue
    ) {}

    /** Bulk update: list of key/value pairs. */
    public record BulkUpdateConfigRequest(
            @NotNull
            List<ConfigEntry> entries
    ) {
        public record ConfigEntry(
                @NotBlank String configKey,
                @NotBlank String configValue
        ) {}
    }

    // ─── Well-known keys ─────────────────────────────────────────────────────

    public static final String KEY_PEB_EXPIRY_WARNING_DAYS       = "peb_expiry_warning_days";
    public static final String KEY_BOILER_SERVICE_WARNING_DAYS   = "boiler_service_warning_days";
    public static final String KEY_LEASE_END_NOTICE_WARNING_DAYS = "lease_end_notice_warning_days";
    public static final String KEY_INDEXATION_NOTICE_DAYS        = "indexation_notice_days";
    public static final String KEY_APP_NAME                      = "app_name";
    public static final String KEY_DEFAULT_COUNTRY               = "default_country";
}
