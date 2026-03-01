# UC012 — Manage Platform Configuration

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC012 |
| **Name** | Manage Platform Configuration |
| **Actor** | ADMIN |
| **Epic** | Platform Administration |
| **Status** | 📋 Ready for Implementation |

An admin can manage the application's configurable parameters: boiler service validity rules (time-sensitive regulation history) and general alert thresholds. This UC also covers the dedicated "Platform Settings" menu accessible from the Administration section.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US067 | View Platform Settings | MUST HAVE | 2 |
| US068 | Add Boiler Service Validity Rule | MUST HAVE | 3 |
| US069 | View Boiler Service Validity Rules History | MUST HAVE | 2 |
| US070 | Update General Alert Settings | MUST HAVE | 2 |

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
2. Page has two sections:
   - **Boiler Service Validity Rules** — table of temporal rules + "Add Rule" button.
   - **General Settings** — current alert threshold values with "Edit" button.

### Add Boiler Service Validity Rule (US068)
1. Admin clicks "Add Rule" in the validity rules section.
2. Form: valid from date (required), validity duration in months (required, > 0), description (optional).
3. Admin saves → new rule stored. The most recent rule (highest valid_from ≤ today) is the current rule.
4. All existing service records whose valid_until was auto-calculated are **not** retroactively modified (historical values frozen).

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

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| valid_from already exists | 409 | `A validity rule already exists for this date` |
| validity_duration_months ≤ 0 | 400 | `Validity duration must be greater than zero` |
| config_key not found | 404 | `Configuration key not found` |
| configValue invalid type | 400 | `Invalid value for type INTEGER` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | /api/v1/config/boiler-validity-rules | US069 |
| POST | /api/v1/config/boiler-validity-rules | US068 |
| GET | /api/v1/config/settings | US067 + US070 |
| PUT | /api/v1/config/settings/{key} | US070 |

---

**Last Updated:** 2026-03-01 | **Status:** 📋 Ready for Implementation
