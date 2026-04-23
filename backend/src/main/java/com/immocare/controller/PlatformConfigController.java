package com.immocare.controller;

/**
 * UC004_ESTATE_PLACEHOLDER Phase 5 — this controller has been removed.
 *
 * <p>
 * All global {@code /api/v1/platform-config/**} endpoints are replaced by
 * per-estate routes in {@link EstateConfigController}:
 * 
 * <pre>
 *   GET  /api/v1/estates/{estateId}/config/settings
 *   GET  /api/v1/estates/{estateId}/config/settings/{key}
 *   PUT  /api/v1/estates/{estateId}/config/settings/{key}
 *   GET  /api/v1/estates/{estateId}/config/boiler-validity-rules
 *   POST /api/v1/estates/{estateId}/config/boiler-validity-rules
 *   GET  /api/v1/estates/{estateId}/config/asset-type-mappings
 *   PUT  /api/v1/estates/{estateId}/config/asset-type-mappings/{assetType}
 * </pre>
 *
 * <p>
 * <strong>Action:</strong> delete this file from your source tree.
 * It is a non-functional placeholder that compiles cleanly so the build
 * does not break during the migration step.
 *
 * @deprecated Removed in UC004_ESTATE_PLACEHOLDER Phase 5. Use {@link EstateConfigController}.
 */
@Deprecated(since = "UC004_ESTATE_PLACEHOLDER-Phase5", forRemoval = true)
public class PlatformConfigController {
    // Intentionally empty — see EstateConfigController
}