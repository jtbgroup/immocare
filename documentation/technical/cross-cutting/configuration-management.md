---
title: Cross-Cutting Configuration Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "PlatformConfig, EstateConfig, PlatformConfigController, EstateConfigController, PlatformConfigService, EstateConfigService, EstateService, PlatformConfigDTOs, EstateConfigDTOs, PlatformConfigKey, V013__uc013_manage_config.sql, platform-config.component.ts, estate-config.component.ts, estate-config-assets.component.ts, estate-config-assets.component.html, estate-config.service.ts, platform-config.helpers.ts"
---

# Cross-Cutting Configuration Management

This page describes the application's configuration surfaces across platform-wide settings and estate-scoped settings. Configuration remains cross-cutting: a setting's owner and behavior depend on what it configures.

## Platform-wide configuration

Platform settings are stored by key in `platform_config`, independently of any estate. The platform settings page is routed through a platform-administrator guard. The API requires authentication for reads; writes require the `PLATFORM_ADMIN` role.

| Operation | Endpoint | Access | Behavior |
|---|---|---|---|
| List settings | `GET /api/v1/platform-config/settings` | Authenticated user | Returns stored entries and default entries not yet stored. |
| Read one setting | `GET /api/v1/platform-config/settings/get?key=...` | Authenticated user | Returns the stored or recognized default value, or `404` when no value is known. |
| Create or update a setting | `PUT /api/v1/platform-config/settings/{key}` | Platform administrator | Upserts the key; a new key is created as type `STRING`. |

Known default entries returned by the service include:

| Key | Default | Intended use |
|---|---|---|
| `app.name` | `ImmoCare` | Application display name |
| `default.country` | `BE` | Default country for estate creation |
| `imports.csv.suggestion.confidence.threshold` | `3` | Fallback confidence threshold for transaction-import suggestions |

The database migration seeds `app.name`; the service also supplies defaults from `PlatformConfigDTOs.DEFAULT_CONFIG` for keys not stored in the database. The recognized keys in `PlatformConfigKey` and the default list in `PlatformConfigDTOs` are not identical.

The update request requires a nonblank value. The service trims and stores the value but does not validate it against `valueType`; its generic integer getter uses the caller-provided fallback if parsing fails. The current platform settings UI exposes the general settings section, while the related historical requirements for other settings sections do not all match the present implementation.

## Estate-scoped configuration

Estate settings are stored in `estate_config` with the composite key `(estate_id, config_key)`. The API returns settings to estate members and permits estate managers to update existing keys; updates do not create an unknown key. Each new estate is seeded from `EstateConfigDTOs.DEFAULT_CONFIG`.

The current seed list includes transaction-import confidence, equipment-to-subcategory mapping values, boiler-service alert threshold, lease-end warning days, lease-indexation warning days, and PEB-expiry warning months. Their current default values and descriptions are declared in that seed list. This page records the observed implementation and does not establish how the supported setting registry, validation policies, or per-setting editability should be governed; that design review remains an [open question](../../../backlog/open_questions.md#estate-configuration).

The seed defaults are: confidence threshold `3`; empty equipment-mapping values; boiler-service warning `3` months; lease-end and indexation warnings `30` days each; and PEB-expiry warning `3` months. The list operation returns persisted estate entries and does not synthesize missing default entries for an existing estate.

Although equipment-to-subcategory mapping keys are seeded and a mapping section is present in the estate-settings UI, the controller routes and service operations for mappings are commented out. The UI section is also wired to lease and PEB alert keys, which are excluded from its save set, rather than to the equipment-mapping keys. Therefore the mapping workflow is not functional; completing it is tracked in the [backlog](../../../backlog/backlog.md).

## Data model

Platform-wide and estate-scoped settings are separate persisted entities. The estate configuration is related to one estate; platform configuration has no estate relationship. Their relationships are included in the cross-cutting [data model](./data-model.md).

## Sources

- [Platform configuration controller](../../../backend/src/main/java/com/immocare/controller/PlatformConfigController.java)
- [Estate configuration controller](../../../backend/src/main/java/com/immocare/controller/EstateConfigController.java)
- [Platform configuration service](../../../backend/src/main/java/com/immocare/service/PlatformConfigService.java)
- [Estate configuration service](../../../backend/src/main/java/com/immocare/service/EstateConfigService.java)
- [Platform configuration DTOs](../../../backend/src/main/java/com/immocare/model/dto/PlatformConfigDTOs.java)
- [Estate configuration DTOs](../../../backend/src/main/java/com/immocare/model/dto/EstateConfigDTOs.java)
- [Configuration migration](../../../backend/src/main/resources/db/migration/V013__uc013_manage_config.sql)
- [Platform configuration component](../../../frontend/src/app/features/platform-config/platform-config.component.ts)
- [Estate configuration component](../../../frontend/src/app/features/estate-config/estate-config.component.ts)
- [Estate equipment-mapping section](../../../frontend/src/app/features/estate-config/components/estate-config-assets.component/estate-config-assets.component.ts)
- [Estate equipment-mapping template](../../../frontend/src/app/features/estate-config/components/estate-config-assets.component/estate-config-assets.component.html)
- [Estate configuration service](../../../frontend/src/app/core/services/estate-config.service.ts)
