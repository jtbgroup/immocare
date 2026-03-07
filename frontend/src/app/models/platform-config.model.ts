// models/platform-config.model.ts — UC012

export interface PlatformConfigDTO {
  configKey: string;
  configValue: string;
  description: string | null;
  updatedAt: string;
}

export interface UpdateConfigRequest {
  configValue: string;
}

export interface BulkUpdateConfigRequest {
  entries: { configKey: string; configValue: string }[];
}

// Well-known keys — keep in sync with backend PlatformConfigKeys.java
export const CONFIG_KEYS = {
  // ── General ────────────────────────────────────────────────────────────────
  APP_NAME:                    "app_name",
  DEFAULT_COUNTRY:             "default_country",
  APP_DATE_FORMAT:             "app.date_format",

  // ── Alert thresholds ───────────────────────────────────────────────────────
  PEB_EXPIRY_WARNING_DAYS:          "peb_expiry_warning_days",
  BOILER_SERVICE_WARNING_DAYS:      "boiler_service_warning_days",
  LEASE_END_NOTICE_WARNING_DAYS:    "lease_end_notice_warning_days",
  INDEXATION_NOTICE_DAYS:           "indexation_notice_days",

  // ── Import behaviour (global) ──────────────────────────────────────────────
  IMPORT_ON_DUPLICATE:              "import.on_duplicate",
  IMPORT_SUGGESTION_CONFIDENCE:     "csv.import.suggestion.confidence.threshold",
} as const;

export const CONFIG_LABELS: Record<string, string> = {
  // General
  app_name:          "Nom de l'application",
  default_country:   "Pays par défaut",
  "app.date_format": "Format de date",

  // Alerts
  peb_expiry_warning_days:       "Alerte expiration PEB (jours avant)",
  boiler_service_warning_days:   "Alerte entretien chaudière (jours avant)",
  lease_end_notice_warning_days: "Alerte fin de bail (jours avant)",
  indexation_notice_days:        "Délai préavis indexation (jours)",

  // Import
  "import.on_duplicate":                       "Comportement sur doublon détecté",
  "csv.import.suggestion.confidence.threshold": "Seuil de confiance pour suggestions de catégorie",
};

// Preset options for the date-format selector
export const DATE_FORMAT_PRESETS: { value: string; label: string }[] = [
  { value: "dd/MM/yyyy", label: "Européen  —  dd/MM/yyyy" },
  { value: "MM/dd/yyyy", label: "Américain  —  MM/dd/yyyy" },
  { value: "yyyy-MM-dd", label: "ISO 8601  —  yyyy-MM-dd" },
];

export const ON_DUPLICATE_OPTIONS: { value: string; label: string }[] = [
  { value: "WARN",   label: "Importer et signaler comme doublon potentiel" },
  { value: "SKIP",   label: "Ignorer silencieusement les doublons" },
  { value: "IMPORT", label: "Toujours importer (pas de détection)" },
];
