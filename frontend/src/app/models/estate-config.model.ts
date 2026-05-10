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
  APP_NAME: "app_name",
  DEFAULT_COUNTRY: "default_country",
  APP_DATE_FORMAT: "app.date_format",

  // ── Alert thresholds ───────────────────────────────────────────────────────
  PEB_EXPIRY_WARNING_DAYS: "peb_expiry_warning_days",
  BOILER_SERVICE_WARNING_DAYS: "boiler_service_warning_days",
  LEASE_END_NOTICE_WARNING_DAYS: "lease_end_notice_warning_days",
  INDEXATION_NOTICE_DAYS: "indexation_notice_days",

  // ── Import behaviour (Phase 5: per-estate) ────────────────────────────────
  IMPORT_ON_DUPLICATE: "import.on_duplicate",
  IMPORT_SUGGESTION_CONFIDENCE: "csv.import.suggestion.confidence.threshold",

  // ── Boiler alert (Phase 5) ────────────────────────────────────────────────
  BOILER_ALERT_THRESHOLD_MONTHS: "boiler.service.alert.threshold.months",

  // ── Asset type subcategory mappings (Phase 5) ─────────────────────────────
  ASSET_MAPPING_BOILER: "asset.type.subcategory.mapping.BOILER",
  ASSET_MAPPING_FIRE_EXTINGUISHER:
    "asset.type.subcategory.mapping.FIRE_EXTINGUISHER",
  ASSET_MAPPING_METER: "asset.type.subcategory.mapping.METER",

  // ── CSV import (Phase 5: per-estate) ─────────────────────────────────────
  CSV_DELIMITER: "csv.import.delimiter",
  CSV_DATE_FORMAT: "csv.import.date_format",
  CSV_SKIP_HEADER_ROWS: "csv.import.skip_header_rows",
  CSV_COL_DATE: "csv.import.col.date",
  CSV_COL_AMOUNT: "csv.import.col.amount",
  CSV_COL_DESCRIPTION: "csv.import.col.description",
  CSV_COL_COUNTERPARTY: "csv.import.col.counterparty_account",
  CSV_COL_EXTERNAL_REF: "csv.import.col.external_reference",
  CSV_COL_BANK_ACCOUNT: "csv.import.col.bank_account",
  CSV_COL_VALUE_DATE: "csv.import.col.value_date",
} as const;

export const CONFIG_LABELS: Record<string, string> = {
  // General
  app_name: "Nom de l'application",
  default_country: "Pays par défaut",
  "app.date_format": "Format de date",

  // Alerts
  peb_expiry_warning_days: "Alerte expiration PEB (jours avant)",
  boiler_service_warning_days: "Alerte entretien chaudière (jours avant)",
  lease_end_notice_warning_days: "Alerte fin de bail (jours avant)",
  indexation_notice_days: "Délai préavis indexation (jours)",

  // Import
  "import.on_duplicate": "Comportement sur doublon détecté",
  "csv.import.suggestion.confidence.threshold":
    "Seuil de confiance pour suggestions de catégorie",

  // Phase 5
  "boiler.service.alert.threshold.months":
    "Seuil alerte chaudière (mois avant expiration)",
  "asset.type.subcategory.mapping.BOILER": "Sous-catégorie auto — Chaudière",
  "asset.type.subcategory.mapping.FIRE_EXTINGUISHER":
    "Sous-catégorie auto — Extincteur",
  "asset.type.subcategory.mapping.METER": "Sous-catégorie auto — Compteur",
  "csv.import.delimiter": "Délimiteur CSV",
  "csv.import.date_format": "Format de date dans le CSV",
  "csv.import.skip_header_rows": "Lignes en-tête à ignorer",
  "csv.import.col.date": "Colonne date",
  "csv.import.col.amount": "Colonne montant",
  "csv.import.col.description": "Colonne description",
  "csv.import.col.counterparty_account": "Colonne IBAN contrepartie",
  "csv.import.col.external_reference": "Colonne référence bancaire",
  "csv.import.col.bank_account": "Colonne IBAN propre",
  "csv.import.col.value_date": "Colonne date valeur (-1 = absente)",
};

// Preset options for the date-format selector
export const DATE_FORMAT_PRESETS: { value: string; label: string }[] = [
  { value: "dd/MM/yyyy", label: "Européen  —  dd/MM/yyyy" },
  { value: "MM/dd/yyyy", label: "Américain  —  MM/dd/yyyy" },
  { value: "yyyy-MM-dd", label: "ISO 8601  —  yyyy-MM-dd" },
];

export const ON_DUPLICATE_OPTIONS: { value: string; label: string }[] = [
  { value: "WARN", label: "Importer et signaler comme doublon potentiel" },
  { value: "SKIP", label: "Ignorer silencieusement les doublons" },
  { value: "IMPORT", label: "Toujours importer (pas de détection)" },
];
