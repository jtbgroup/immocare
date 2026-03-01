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

// Well-known keys — keep in sync with PlatformConfigDTOs.java
export const CONFIG_KEYS = {
  PEB_EXPIRY_WARNING_DAYS:       'peb_expiry_warning_days',
  BOILER_SERVICE_WARNING_DAYS:   'boiler_service_warning_days',
  LEASE_END_NOTICE_WARNING_DAYS: 'lease_end_notice_warning_days',
  INDEXATION_NOTICE_DAYS:        'indexation_notice_days',
  APP_NAME:                      'app_name',
  DEFAULT_COUNTRY:               'default_country',
} as const;

export const CONFIG_LABELS: Record<string, string> = {
  peb_expiry_warning_days:       'PEB certificate expiry warning (days)',
  boiler_service_warning_days:   'Boiler service alert threshold (days)',
  lease_end_notice_warning_days: 'Lease end notice warning (days)',
  indexation_notice_days:        'Indexation notice threshold (days)',
  app_name:                      'Application name',
  default_country:               'Default country',
};
