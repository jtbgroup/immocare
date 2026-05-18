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

// ─── Well-known keys — keep in sync with PlatformConfigDTOs.java ─────────────

export const PLATFORM_CONFIG_KEYS = {
  APP_NAME:        'app_name',
  DEFAULT_COUNTRY: 'default_country',
} as const;

export type PlatformConfigKey =
  (typeof PLATFORM_CONFIG_KEYS)[keyof typeof PLATFORM_CONFIG_KEYS];

// ─── UI labels ────────────────────────────────────────────────────────────────

export const PLATFORM_CONFIG_LABELS: Record<string, string> = {
  app_name:        "Nom de l'application",
  default_country: 'Pays par défaut (code ISO, ex: BE)',
};

// ─── Value type hints (for input rendering) ───────────────────────────────────

export const PLATFORM_CONFIG_STRING_KEYS = new Set<string>([
  PLATFORM_CONFIG_KEYS.APP_NAME,
  PLATFORM_CONFIG_KEYS.DEFAULT_COUNTRY,
]);
