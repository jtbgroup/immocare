package com.immocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

/**
 * DTOs for UC004_ESTATE_PLACEHOLDER Phase 5 — per-estate Platform Configuration
 * and Boiler
 * Validity Rules.
 * Replaces the global PlatformConfigDTOs for estate-scoped operations.
 */
public final class EstateConfigDTOs {

        public EstateConfigDTOs() {
        }

        /** Response: one config entry. */
        public record EstateConfigDTO(
                        UUID estateId,
                        String configKey,
                        String configValue,
                        String valueType,
                        String description,
                        LocalDateTime updatedAt) {
        }

        /** Request: update a single config value. */
        public record UpdateEstateConfigRequest(
                        @NotBlank(message = "Config value is required") String configValue) {
        }

        /** Response: one asset-type → subcategory mapping. */
        public record AssetTypeMappingDTO(
                        String assetType,
                        Long subcategoryId,
                        String subcategoryName) {
        }

        /** Request: update an asset-type mapping. */
        public record UpdateAssetTypeMappingRequest(
                        Long subcategoryId // null = clear mapping
        ) {
        }

        // ─── Boiler Service Validity Rules ────────────────────────────────────────

        /** Response: one validity rule. */
        public record BoilerServiceValidityRuleDTO(
                        Long id,
                        UUID estateId,
                        LocalDate validFrom,
                        int validityDurationMonths,
                        String description,
                        LocalDateTime createdAt) {
        }

        /** Request: add a new validity rule. */
        public record AddBoilerServiceValidityRuleRequest(
                        @NotNull(message = "Valid from date is required") @PastOrPresent(message = "Valid from date cannot be in the future") LocalDate validFrom,

                        @NotNull(message = "Validity duration is required") @Min(value = 1, message = "Validity duration must be at least 1 month") Integer validityDurationMonths,

                        String description) {
        }

        // ─── Well-known config keys ───────────────────────────────────────────────

        public static final String KEY_ASSET_MAPPING_BOILER = "assets.type.subcategory.mapping.boiler";
        public static final String KEY_ASSET_MAPPING_FIRE_EXTINGUISHER = "assets.type.subcategory.mapping.fire_extinguisher";
        public static final String KEY_ASSET_MAPPING_METER = "assets.type.subcategory.mapping.meter";
        // IMPORTS
        public static final String KEY_IMPORTS_CSV_SUGGESTION_CONFIDENCE = "imports.csv.suggestion.confidence.threshold";
        // BOILERS
        public static final String KEY_BOILERS_ALERT_THRESHOLD_MONTHS = "boilers.service.alert.threshold.months";
        // CONTACTS
        public static final String KEY_CONTRACTS_ALERT_LEASE_END_NOTICE_DAYS = "contracts.alert.lease.end.notice.days";
        public static final String KEY_CONTRACTS_ALERT_LEASE_INDEXATION_DAYS = "contracts.alert.lease.indexation.days";
        public static final String KEY_CONTRACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS = "contracts.alert.peb.expiry.warning.months";

        /** Default config entries seeded for every new estate. */
        public record EstateConfigSeed(String key, String value, String valueType, String description) {
        }

        public static final List<EstateConfigSeed> DEFAULT_CONFIG = List.of(
                        new EstateConfigSeed(KEY_IMPORTS_CSV_SUGGESTION_CONFIDENCE, "3", "INTEGER",
                                        "Minimum confidence threshold for transaction import suggestions (1-5)"),
                        new EstateConfigSeed(KEY_ASSET_MAPPING_BOILER, "", "STRING",
                                        "Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping)"),
                        new EstateConfigSeed(KEY_ASSET_MAPPING_FIRE_EXTINGUISHER, "", "STRING",
                                        "Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added"),
                        new EstateConfigSeed(KEY_ASSET_MAPPING_METER, "", "STRING",
                                        "Subcategory ID to pre-fill when a METER asset link is added"),
                        new EstateConfigSeed(KEY_BOILERS_ALERT_THRESHOLD_MONTHS, "3", "INTEGER",
                                        "Months before service expiry to display a warning alert"),
                        new EstateConfigSeed(KEY_CONTRACTS_ALERT_LEASE_END_NOTICE_DAYS, "30", "INTEGER",
                                        "Days before lease end to display a warning alert"),
                        new EstateConfigSeed(KEY_CONTRACTS_ALERT_LEASE_INDEXATION_DAYS, "30", "INTEGER",
                                        "Days before lease indexation to display a warning alert"),
                        new EstateConfigSeed(KEY_CONTRACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS, "3", "INTEGER",
                                        "Months before PEB certificate expiry to display a warning alert"));

}
