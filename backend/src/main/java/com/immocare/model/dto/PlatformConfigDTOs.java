package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * DTOs for platform-wide configuration (not scoped to any estate).
 *
 * <p>
 * Platform config keys are managed by PLATFORM_ADMIN only and apply
 * globally across all estates. Estate-scoped config uses
 * {@link PlatformConfigDTOs} instead.
 *
 * <p>
 * Exposed via {@code /api/v1/platform-config/**}.
 */
public final class PlatformConfigDTOs {

        private PlatformConfigDTOs() {
        }

        // ─── Response ─────────────────────────────────────────────────────────────

        /** Response: one platform config entry. */
        public record PlatformConfigDTO(
                        String configKey,
                        String configValue,
                        String valueType,
                        String description,
                        LocalDateTime updatedAt) {
        }

        // ─── Request ──────────────────────────────────────────────────────────────

        /** Request: update a single platform config value. */
        public record UpdatePlatformConfigRequest(
                        @NotBlank(message = "Config value is required") String configValue) {
        }

        // ─── Well-known keys ──────────────────────────────────────────────────────

        /**
         * Display name of the application shown in the UI header.
         * Type: STRING. Default: "ImmoCare".
         */
        public static final String KEY_APP_NAME = "app.name";

        /**
         * Default country code (ISO 3166-1 alpha-2) used when creating new estates.
         * Type: STRING. Default: "BE".
         */
        public static final String KEY_DEFAULT_COUNTRY = "default.country";

        /**
         * Global default confidence threshold for transaction import suggestions
         * (fallback for estates).
         * Type: INTEGER. Default: 3. Can be overridden per-estate via EstateConfig.
         */
        public static final String KEY_IMPORTS_CSV_SUGGESTION_CONFIDENCE = "imports.csv.suggestion.confidence.threshold";

        public record PlatformConfigSeed(String key, String value, String valueType) {
        }

        public static final List<PlatformConfigSeed> DEFAULT_CONFIG = List.of(
                        new PlatformConfigSeed(KEY_APP_NAME, "ImmoCare", "STRING"),
                        new PlatformConfigSeed(KEY_DEFAULT_COUNTRY, "BE", "STRING"),
                        new PlatformConfigSeed(KEY_IMPORTS_CSV_SUGGESTION_CONFIDENCE, "3", "INTEGER"));
}
