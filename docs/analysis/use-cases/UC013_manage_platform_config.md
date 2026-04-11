# UC012 — Manage Platform Configuration

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC012 |
| **Name** | Manage Platform Configuration |
| **Actor** | ADMIN |
| **Epic** | Platform Administration |
| **Status** | 📋 Ready for Implementation |

An admin can manage the application's configurable parameters: boiler service validity rules (time-sensitive regulation history), general alert thresholds, and asset type to subcategory mappings used during transaction classification. This UC also covers the dedicated "Platform Settings" menu accessible from the Administration section.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US067 | View Platform Settings | MUST HAVE | 2 |
| US068 | Add Boiler Service Validity Rule | MUST HAVE | 3 |
| US069 | View Boiler Service Validity Rules History | MUST HAVE | 2 |
| US070 | Update General Alert Settings | MUST HAVE | 2 |
| US071 | Manage Asset Type to Subcategory Mappings | MUST HAVE | 2 |

---

## Actors

- **ADMIN**: Only role. Full access to all configuration.

---

## Preconditions

- User is authenticated as ADMIN.

---

## Main Flows

### View Platform Settings (US067)
1. Admin clicks "Platform Settings" in the Administration menu (alongside Users).
2. Page has three sections:
   - **Boiler Service Validity Rules** — table of temporal rules + "Add Rule" button.
   - **General Settings** — current alert threshold values with "Edit" button.
   - **Asset Type Mappings** — mapping table between asset types and subcategories.

### Add Boiler Service Validity Rule (US068)
1. Admin clicks "Add Rule" in the validity rules section.
2. Form: valid from date (required), validity duration in months (required, > 0), description (optional).
3. Admin saves → new rule stored. The most recent rule (highest valid_from ≤ today) is the current rule.
4. All existing boiler service records whose valid_until was auto-calculated are **not** retroactively modified (historical values frozen).

### View Boiler Service Validity Rules History (US069)
1. Table lists all rules ordered by valid_from DESC.
2. Columns: valid from, duration (months), description, created by, created at.
3. Current rule highlighted (valid_from ≤ today AND no newer rule exists).
4. Rules are read-only after creation (no edit/delete).

### Update General Alert Settings (US070)
1. Admin clicks "Edit" on General Settings section.
2. Editable fields:
   - Boiler service expiry warning threshold (months) — key: `boiler.service.alert.threshold.months`
3. Admin saves → config values updated in platform_config table.
4. New threshold takes effect immediately for all alert computations.

### Manage Asset Type to Subcategory Mappings (US071)
1. Admin views the "Asset Type Mappings" section showing a table with one row per asset type.
2. Each row shows: asset type label, currently mapped subcategory (category / subcategory) or "Not mapped", "Edit" button.
3. Admin clicks "Edit" on a row → subcategory picker appears (category dropdown → subcategory dropdown filtered by direction EXPENSE or BOTH).
4. Admin selects a subcategory → saves → mapping updated immediately.
5. Admin can clear a mapping (set to "Not mapped") → no automatic suggestion for that asset type.
6. When an asset link of this type is added to a transaction, the mapped subcategory is **automatically pre-filled** on the transaction classification fields. The admin can override freely.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC012-01 | Validity rules are append-only (no edit, no delete) |
| BR-UC012-02 | valid_from must be unique across all rules |
| BR-UC012-03 | To find the rule applicable to a service_date: SELECT rule WHERE valid_from ≤ service_date ORDER BY valid_from DESC LIMIT 1 |
| BR-UC012-04 | Adding a new rule does NOT retroactively recalculate existing boiler_service.valid_until values |
| BR-UC012-05 | Alert threshold must be a positive integer (months) |
| BR-UC012-06 | platform_config values are stored as VARCHAR and cast by value_type |
| BR-UC012-07 | Asset type mappings reference a subcategory whose direction is EXPENSE or BOTH; INCOME subcategories cannot be mapped to an asset type |
| BR-UC012-08 | Asset type mappings are applied as suggestions only — the admin can always override the pre-filled subcategory on a transaction |
| BR-UC012-09 | Each asset type (BOILER, FIRE_EXTINGUISHER, METER) has at most one mapped subcategory |

---

## Data Model

### Table: `boiler_service_validity_rule`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `valid_from` | DATE | NOT NULL, UNIQUE |
| `validity_duration_months` | INTEGER | NOT NULL, CHECK > 0 |
| `description` | TEXT | NULL |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `created_by` | BIGINT | FK → app_user(id) ON DELETE SET NULL |

**Seed:** `valid_from = 1900-01-01`, `validity_duration_months = 24`, `description = 'Default — 2 years'`

### Table: `platform_config`

| Column | Type | Constraints |
|---|---|---|
| `config_key` | VARCHAR(100) | PK |
| `config_value` | VARCHAR(500) | NOT NULL |
| `value_type` | VARCHAR(20) | NOT NULL — STRING, INTEGER, BOOLEAN, DECIMAL |
| `description` | TEXT | NULL |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `updated_by` | BIGINT | FK → app_user(id) ON DELETE SET NULL |

**Seeds:**

| config_key | config_value | value_type | description |
|---|---|---|---|
| `boiler.service.alert.threshold.months` | `3` | `INTEGER` | Months before expiry to trigger warning alert |
| `asset.type.subcategory.mapping.BOILER` | `''` | `STRING` | Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping) |
| `asset.type.subcategory.mapping.FIRE_EXTINGUISHER` | `''` | `STRING` | Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added |
| `asset.type.subcategory.mapping.METER` | `''` | `STRING` | Subcategory ID to pre-fill when a METER asset link is added |

> **Note:** The config_value stores the subcategory ID as a string (cast to Long at runtime). An empty string means no mapping is configured for that asset type.

---

## DTOs

### `BoilerServiceValidityRuleDTO`
```
id, validFrom, validityDurationMonths, description,
isCurrent,       -- computed: validFrom ≤ today AND no newer rule exists
createdAt, createdByUsername
```

### `AddBoilerServiceValidityRuleRequest`
```
validFrom*                 LocalDate   required, must be unique
validityDurationMonths*    Integer     required, > 0
description                String      optional
```

### `PlatformConfigDTO`
```
configKey, configValue, valueType, description, updatedAt, updatedByUsername
```

### `UpdatePlatformConfigRequest`
```
configValue*   String   required, must be valid for the key's value_type
```

### `AssetTypeMappingDTO`
```
assetType           AssetType (BOILER / FIRE_EXTINGUISHER / METER)
assetTypeLabel      String    human-readable label
subcategoryId       Long      nullable
subcategoryName     String    nullable
categoryId          Long      nullable
categoryName        String    nullable
```

### `UpdateAssetTypeMappingRequest`
```
subcategoryId   Long   nullable (null = clear mapping)
```

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| valid_from already exists | 409 | `A validity rule already exists for this date` |
| validity_duration_months ≤ 0 | 400 | `Validity duration must be greater than zero` |
| config_key not found | 404 | `Configuration key not found` |
| configValue invalid type | 400 | `Invalid value for type INTEGER` |
| Subcategory direction incompatible with asset mapping | 400 | `Asset type mappings must use a subcategory with direction EXPENSE or BOTH` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | /api/v1/config/boiler-validity-rules | US069 |
| POST | /api/v1/config/boiler-validity-rules | US068 |
| GET | /api/v1/config/settings | US067 + US070 |
| PUT | /api/v1/config/settings/{key} | US070 |
| GET | /api/v1/config/asset-type-mappings | US071 |
| PUT | /api/v1/config/asset-type-mappings/{assetType} | US071 |

---

**Last Updated:** 2026-04-04 | **Status:** 📋 Ready for Implementation