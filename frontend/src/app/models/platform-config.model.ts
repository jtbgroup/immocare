/**
 * Models for platform-wide configuration.
 *
 * Platform config is global (no estate scope) and managed by PLATFORM_ADMIN
 * only. Estate-scoped config lives in estate-config.model.ts.
 *
 * Mirrors: PlatformConfigDTOs.java
 */

export interface PlatformConfigDTO {
  configKey: string;
  configValue: string;
  valueType?: string; // 'INTEGER' | 'STRING'
  description: string | null;
  updatedAt: string;
}

export interface UpdatePlatformConfigRequest {
  configValue: string;
}

export const CONFIG_KEYS = {
  // ── General ────────────────────────────────────────────────────────────────
  GENERAL_APP_NAME: "app.name",
  GENERAL_DEFAULT_COUNTRY: "default.country",

  // ── IMPORTS ───────────────────────────────────────────────────────
  //   IMPORTS_CSV_COL_DATE: "imports.csv.col.date",
  //   IMPORTS_CSV_COL_AMOUNT: "imports.csv.col.amount",
  //   IMPORTS_CSV_COL_DESCRIPTION: "imports.csv.col.description",
  //   IMPORTS_CSV_COL_COUNTERPARTY: "imports.csv.col.counterparty_account",
  //   IMPORTS_CSV_COL_EXTERNAL_REF: "imports.csv.col.external_reference",
  //   IMPORTS_CSV_COL_BANK_ACCOUNT: "imports.csv.col.bank_account",
  //   IMPORTS_CSV_COL_VALUE_DATE: "imports.csv.col.value_date",
  //   IMPORTS_CSV_SKIP_HEADER_ROWS: "imports.csv.skip_header_rows",
  //   IMPORTS_CSV_DELIMITER: "imports.csv.delimiter",
  //   IMPORTS_CSV_DATE_FORMAT: "imports.csv.date_format",
  //   IMPORTS_CSV_SUGGESTION_CONFIDENCE:
  //     "imports.csv.suggestion.confidence.threshold",
} as const;

//TODO : probably delete from here
// ─── Well-known keys — keep in sync with PlatformConfigDTOs.java ─────────────

export const PLATFORM_CONFIG_KEYS = {
  APP_NAME: "app_name",
  DEFAULT_COUNTRY: "default_country",
} as const;

export type PlatformConfigKey =
  (typeof PLATFORM_CONFIG_KEYS)[keyof typeof PLATFORM_CONFIG_KEYS];

// ─── UI labels ────────────────────────────────────────────────────────────────

export const PLATFORM_CONFIG_LABELS: Record<string, string> = {
  app_name: "Nom de l'application",
  default_country: "Pays par défaut (code ISO, ex: BE)",
};

// ─── Value type hints (for input rendering) ───────────────────────────────────

export const PLATFORM_CONFIG_STRING_KEYS = new Set<string>([
  PLATFORM_CONFIG_KEYS.APP_NAME,
  PLATFORM_CONFIG_KEYS.DEFAULT_COUNTRY,
]);
