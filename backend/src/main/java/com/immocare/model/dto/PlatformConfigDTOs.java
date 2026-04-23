package com.immocare.model.dto;

/**
 * Legacy placeholder — UC004_ESTATE_PLACEHOLDER Phase 5.
 *
 * All platform configuration is now per-estate.
 * Use {@link EstatePlatformConfigDTOs} for all config operations.
 *
 * This class is retained only to avoid compile errors in any files not yet
 * migrated. Remove it once all references have been updated.
 *
 * @deprecated Use {@link EstatePlatformConfigDTOs} instead.
 */
@Deprecated(since = "UC004_ESTATE_PLACEHOLDER-Phase5", forRemoval = true)
public final class PlatformConfigDTOs {

        private PlatformConfigDTOs() {
        }

        // ─── Keys retained as constants so callers compile during migration ───────
        // All of these are superseded by EstatePlatformConfigDTOs constants.

        /**
         * @deprecated Use
         *             {@link EstatePlatformConfigDTOs#KEY_BOILER_ALERT_THRESHOLD_MONTHS}
         */
        @Deprecated
        public static final String KEY_PEB_EXPIRY_WARNING_DAYS = "peb_expiry_warning_days";
        /**
         * @deprecated Use
         *             {@link EstatePlatformConfigDTOs#KEY_BOILER_ALERT_THRESHOLD_MONTHS}
         */
        @Deprecated
        public static final String KEY_BOILER_SERVICE_WARNING_DAYS = "boiler.service.alert.threshold.months";
        /** @deprecated */
        @Deprecated
        public static final String KEY_LEASE_END_NOTICE_WARNING_DAYS = "lease_end_notice_warning_days";
        /** @deprecated */
        @Deprecated
        public static final String KEY_INDEXATION_NOTICE_DAYS = "indexation_notice_days";
        /** @deprecated */
        @Deprecated
        public static final String KEY_APP_NAME = "app_name";
        /** @deprecated */
        @Deprecated
        public static final String KEY_DEFAULT_COUNTRY = "default_country";
}