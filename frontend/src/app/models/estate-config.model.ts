export interface EstateConfigDTO {
  configKey: string;
  configValue: string;
  valueType?: string; // Phase 5: 'INTEGER' | 'STRING'
  description: string | null;
  updatedAt: string;
}

export interface UpdateConfigRequest {
  configValue: string;
}

export interface BulkUpdateConfigRequest {
  entries: { configKey: string; configValue: string }[];
}

// ─── Asset type mapping (Phase 5) ────────────────────────────────────────────

export interface AssetTypeMappingDTO {
  assetType: string; // 'BOILER' | 'FIRE_EXTINGUISHER' | 'METER'
  subcategoryId: number | null;
  subcategoryName: string | null;
}

export interface UpdateAssetTypeMappingRequest {
  subcategoryId: number | null;
}

// ─── Boiler service validity rule (Phase 5) ───────────────────────────────────

export interface BoilerServiceValidityRuleDTO {
  id: number;
  validFrom: string; // ISO date
  validityDurationMonths: number;
  description: string | null;
}

export interface AddBoilerServiceValidityRuleRequest {
  validFrom: string;
  validityDurationMonths: number;
  description?: string | null;
}

// ─── Well-known keys — keep in sync with backend PlatformConfigKeys.java ─────

export const CONFIG_KEYS = {
  // ── General ────────────────────────────────────────────────────────────────

  // ── CONTACTS ───────────────────────────────────────────────────────
  CONTACTS_ALERT_PEB_EXPIRY_WARNING_MONTHS:
    "contracts.alert.peb.expiry.warning.months",
  CONTACTS_ALERT_LEASE_END_NOTICE_DAYS: "contracts.alert.lease.end.notice.days",
  CONTACTS_ALERT_LEASE_INDEXATION_DAYS: "contracts.alert.lease.indexation.days",

  // ── IMPORTS ───────────────────────────────────────────────────────
  IMPORTS_CSV_COL_DATE: "imports.csv.col.date",
  IMPORTS_CSV_COL_AMOUNT: "imports.csv.col.amount",
  IMPORTS_CSV_COL_DESCRIPTION: "imports.csv.col.description",
  IMPORTS_CSV_COL_COUNTERPARTY: "imports.csv.col.counterparty_account",
  IMPORTS_CSV_COL_EXTERNAL_REF: "imports.csv.col.external_reference",
  IMPORTS_CSV_COL_BANK_ACCOUNT: "imports.csv.col.bank_account",
  IMPORTS_CSV_COL_VALUE_DATE: "imports.csv.col.value_date",
  IMPORTS_CSV_SKIP_HEADER_ROWS: "imports.csv.skip_header_rows",
  IMPORTS_CSV_DELIMITER: "imports.csv.delimiter",
  IMPORTS_CSV_DATE_FORMAT: "imports.csv.date_format",
  IMPORTS_CSV_SUGGESTION_CONFIDENCE:
    "imports.csv.suggestion.confidence.threshold",

  // ── BOILERS ───────────────────────────────────────────────────────
  BOILER_ALERT_THRESHOLD_MONTHS: "boilers.service.alert.threshold.months",

  // ── ASSETS MAPPING ───────────────────────────────────────────────────────
  ASSETS_MAPPING_BOILER: "assets.type.subcategory.mapping.boiler",
  ASSETS_MAPPING_FIRE_EXTINGUISHER:
    "assets.type.subcategory.mapping.fire_extinguisher",
  ASSETS_MAPPING_METER: "assets.type.subcategory.mapping.meter",

  // *************************
  // TO CHECK FROM HERE
  // IMPORT_ON_DUPLICATE: "import.on_duplicate",
  // ── Boiler alert (Phase 5) ────────────────────────────────────────────────
  // BOILER_SERVICE_WARNING_DAYS: "boiler_service_warning_days",
  // ── Asset type subcategory mappings (Phase 5) ─────────────────────────────
} as const;

// Preset options for the date-format selector
export const DATE_FORMAT_PRESETS: { value: string; label: string }[] = [
  { value: "dd/MM/yyyy", label: "Européen  —  dd/MM/yyyy" },
  { value: "MM/dd/yyyy", label: "Américain  —  MM/dd/yyyy" },
  { value: "yyyy-MM-dd", label: "ISO 8601  —  yyyy-MM-dd" },
];

// export const ON_DUPLICATE_OPTIONS: { value: string; label: string }[] = [
//   { value: "WARN", label: "Importer et signaler comme doublon potentiel" },
//   { value: "SKIP", label: "Ignorer silencieusement les doublons" },
//   { value: "IMPORT", label: "Toujours importer (pas de détection)" },
// ];
