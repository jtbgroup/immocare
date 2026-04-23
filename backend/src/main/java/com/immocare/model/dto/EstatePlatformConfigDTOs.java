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
 * DTOs for UC004_ESTATE_PLACEHOLDER Phase 5 — per-estate Platform Configuration and Boiler
 * Validity Rules.
 * Replaces the global PlatformConfigDTOs for estate-scoped operations.
 */
public final class EstatePlatformConfigDTOs {

        private EstatePlatformConfigDTOs() {
        }

        // ─── Platform Config ─────────────────────────────────────────────────────

        /** Response: one config entry. */
        public record PlatformConfigDTO(
                        UUID estateId,
                        String configKey,
                        String configValue,
                        String valueType,
                        String description,
                        LocalDateTime updatedAt) {
        }

        /** Request: update a single config value. */
        public record UpdatePlatformConfigRequest(
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

        public static final String KEY_BOILER_ALERT_THRESHOLD_MONTHS = "boiler.service.alert.threshold.months";
        public static final String KEY_ASSET_MAPPING_PREFIX = "asset.type.subcategory.mapping.";
        public static final String KEY_CSV_DELIMITER = "csv.import.delimiter";
        public static final String KEY_CSV_DATE_FORMAT = "csv.import.date_format";
        public static final String KEY_CSV_SKIP_HEADER_ROWS = "csv.import.skip_header_rows";
        public static final String KEY_CSV_COL_DATE = "csv.import.col.date";
        public static final String KEY_CSV_COL_AMOUNT = "csv.import.col.amount";
        public static final String KEY_CSV_COL_DESCRIPTION = "csv.import.col.description";
        public static final String KEY_CSV_COL_COUNTERPARTY_ACCOUNT = "csv.import.col.counterparty_account";
        public static final String KEY_CSV_COL_EXTERNAL_REFERENCE = "csv.import.col.external_reference";
        public static final String KEY_CSV_COL_BANK_ACCOUNT = "csv.import.col.bank_account";
        public static final String KEY_CSV_COL_VALUE_DATE = "csv.import.col.value_date";
        public static final String KEY_CSV_SUGGESTION_CONFIDENCE = "csv.import.suggestion.confidence.threshold";

        /** Default config entries seeded for every new estate. */
        public record PlatformConfigSeed(String key, String value, String valueType, String description) {
        }

        public static final List<PlatformConfigSeed> DEFAULT_CONFIG = List.of(
                        new PlatformConfigSeed(KEY_BOILER_ALERT_THRESHOLD_MONTHS, "3", "INTEGER",
                                        "Months before service expiry to display a warning alert"),
                        new PlatformConfigSeed(KEY_ASSET_MAPPING_PREFIX + "BOILER", "", "STRING",
                                        "Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping)"),
                        new PlatformConfigSeed(KEY_ASSET_MAPPING_PREFIX + "FIRE_EXTINGUISHER", "", "STRING",
                                        "Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added"),
                        new PlatformConfigSeed(KEY_ASSET_MAPPING_PREFIX + "METER", "", "STRING",
                                        "Subcategory ID to pre-fill when a METER asset link is added"),
                        new PlatformConfigSeed(KEY_CSV_DELIMITER, ";", "STRING",
                                        "CSV column delimiter"),
                        new PlatformConfigSeed(KEY_CSV_DATE_FORMAT, "dd/MM/yyyy", "STRING",
                                        "Date format in CSV"),
                        new PlatformConfigSeed(KEY_CSV_SKIP_HEADER_ROWS, "1", "INTEGER",
                                        "Header rows to skip"),
                        new PlatformConfigSeed(KEY_CSV_COL_DATE, "0", "INTEGER",
                                        "Column index for date"),
                        new PlatformConfigSeed(KEY_CSV_COL_AMOUNT, "1", "INTEGER",
                                        "Column index for amount"),
                        new PlatformConfigSeed(KEY_CSV_COL_DESCRIPTION, "2", "INTEGER",
                                        "Column index for description"),
                        new PlatformConfigSeed(KEY_CSV_COL_COUNTERPARTY_ACCOUNT, "3", "INTEGER",
                                        "Column index for counterparty IBAN"),
                        new PlatformConfigSeed(KEY_CSV_COL_EXTERNAL_REFERENCE, "4", "INTEGER",
                                        "Column index for bank reference"),
                        new PlatformConfigSeed(KEY_CSV_COL_BANK_ACCOUNT, "5", "INTEGER",
                                        "Column index for own IBAN"),
                        new PlatformConfigSeed(KEY_CSV_COL_VALUE_DATE, "-1", "INTEGER",
                                        "Column index for value date (-1 = absent)"),
                        new PlatformConfigSeed(KEY_CSV_SUGGESTION_CONFIDENCE, "3", "INTEGER",
                                        "Min confidence for tag suggestion"));
}
