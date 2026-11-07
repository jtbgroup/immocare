# UC012 — Manage Boilers

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC012 |
| **Name** | Manage Boilers |
| **Actor** | ADMIN |
| **Epic** | Boiler Management |
| **Status** | 📋 Ready for Implementation |

An admin can attach a boiler to a housing unit, replace it (keeping history), and view the full boiler history for the unit. Each boiler has a service history with computed validity dates based on time-sensitive regulation rules.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| UC011.001 | Add Boiler to Housing Unit | MUST HAVE | 3 |
| UC011.002 | View Active Boiler | MUST HAVE | 2 |
| UC011.003 | Replace Boiler | MUST HAVE | 3 |
| UC011.004 | View Boiler History | SHOULD HAVE | 2 |
| UC011.005 | Add Boiler Service Record | MUST HAVE | 3 |
| UC011.006 | View Boiler Service History | MUST HAVE | 2 |
| UC011.007 | View Boiler Service Validity Alert | MUST HAVE | 2 |

---

## Actors

- **ADMIN**: Only role. Full access to all operations.

---

## Preconditions

- Housing unit exists.
- For UC011.003: an active boiler exists on the unit.
- For UC011.005–UC011.007: at least one boiler exists on the unit.

---

## Main Flows

### Add Boiler (UC011.001)
1. Admin opens housing unit details → Boiler section → "Add Boiler".
2. Form: brand (required), model (optional), fuel type (optional), installation date (required).
3. Admin saves → boiler created as active (removal_date = NULL).
4. Boiler section shows new boiler details.

### View Active Boiler (UC011.002)
1. Admin opens housing unit details.
2. Boiler section shows: brand, model, fuel type, installation date, current service validity status.
3. If no active boiler → "No boiler registered" + "Add Boiler" button.

### Replace Boiler (UC011.003)
1. Admin clicks "Replace Boiler" on the active boiler.
2. Form: removal date for old boiler (required, ≥ old installation date), then new boiler fields (same as add).
3. Admin saves → old boiler closed (removal_date set), new boiler created active in one transaction.
4. Both boilers visible in history.

### View Boiler History (UC011.004)
1. Admin clicks "View History" in boiler section.
2. Table lists all boilers (active + removed), ordered by installation date DESC.
3. Active boiler shown with green "Active" badge; removed boilers with grey "Removed" badge.

### Add Boiler Service Record (UC011.005)
1. Admin clicks "+ Add Service" in the service history section of the active boiler.
2. Form: service date (required, not future), notes (optional).
3. valid_until auto-calculated: service_date + validity duration in effect on service_date.
4. Admin can override valid_until before saving.
5. Record saved and listed in service history.

### View Boiler Service History (UC011.006)
1. Service history shows all service records for the current boiler, ordered by service_date DESC.
2. Columns: service date, valid until, notes, validity status badge.

### View Boiler Service Validity Alert (UC011.007)
1. On the housing unit details, the boiler section shows a validity badge on the most recent service record:
   - 🔴 **Expired**: valid_until < today
   - 🟠 **Expiring soon**: valid_until within the alert threshold (configurable, default 3 months)
   - ✅ **Valid**: valid_until beyond threshold
2. If no service record exists → 🔴 **No service recorded**.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC012-01 | Only one active boiler per housing unit (removal_date IS NULL) |
| BR-UC012-02 | Boiler records are never deleted — append-only history |
| BR-UC012-03 | Replace is atomic: old boiler closed + new boiler created in single transaction |
| BR-UC012-04 | Removal date must be ≥ installation date of the boiler being replaced |
| BR-UC012-05 | New boiler installation date must be ≥ removal date of the old boiler |
| BR-UC012-06 | service_date cannot be in the future |
| BR-UC012-07 | valid_until is auto-calculated using the validity rule in effect on service_date |
| BR-UC012-08 | valid_until can be manually overridden by admin |
| BR-UC012-09 | Service records are never deleted |
| BR-UC012-10 | Alert threshold is read from platform_config key `boiler.service.alert.threshold.months` |

---

## Data Model

### Table: `boiler`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `housing_unit_id` | BIGINT | NOT NULL, FK → housing_unit(id) ON DELETE RESTRICT |
| `brand` | VARCHAR(100) | NOT NULL |
| `model` | VARCHAR(100) | NULL |
| `fuel_type` | VARCHAR(50) | NULL |
| `installation_date` | DATE | NOT NULL |
| `removal_date` | DATE | NULL (NULL = active) |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `created_by` | BIGINT | FK → app_user(id) ON DELETE SET NULL |

### Table: `boiler_service`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `boiler_id` | BIGINT | NOT NULL, FK → boiler(id) ON DELETE RESTRICT |
| `service_date` | DATE | NOT NULL |
| `valid_until` | DATE | NOT NULL |
| `notes` | TEXT | NULL |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `created_by` | BIGINT | FK → app_user(id) ON DELETE SET NULL |

---

## DTOs

### `BoilerDTO`
```
id, housingUnitId,
brand, model, fuelType,
installationDate, removalDate,
isActive,                         -- removalDate == null
latestService: BoilerServiceDTO,  -- null if none
serviceStatus: ServiceStatus,     -- VALID | EXPIRING_SOON | EXPIRED | NO_SERVICE
createdAt
```

### `BoilerServiceDTO`
```
id, boilerId,
serviceDate, validUntil,
notes,
status: ServiceStatus,
createdAt
```

### `AddBoilerRequest`
```
brand*          VARCHAR(100)
model           VARCHAR(100)   optional
fuelType        VARCHAR(50)    optional
installationDate* LocalDate    not future
```

### `ReplaceBoilerRequest`
```
removalDate*         LocalDate   required, ≥ current boiler installationDate
newBrand*            VARCHAR(100)
newModel             VARCHAR(100)  optional
newFuelType          VARCHAR(50)   optional
newInstallationDate* LocalDate     ≥ removalDate
```

### `AddBoilerServiceRequest`
```
serviceDate*  LocalDate   not future
validUntil    LocalDate   optional override; if null → auto-calculated
notes         TEXT        optional
```

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Housing unit not found | 404 | `Housing unit not found` |
| Boiler not found | 404 | `Boiler not found` |
| No active boiler for replace | 409 | `No active boiler on this unit` |
| Active boiler already exists on add | 409 | `An active boiler already exists on this unit` |
| Removal date < installation date | 400 | `Removal date must be on or after the boiler installation date` |
| New installation date < removal date | 400 | `New boiler installation date must be on or after the removal date` |
| Service date in future | 400 | `Service date cannot be in the future` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | /api/v1/housing-units/{unitId}/boilers | UC011.002 + UC011.004 |
| GET | /api/v1/housing-units/{unitId}/boilers/active | UC011.002 |
| POST | /api/v1/housing-units/{unitId}/boilers | UC011.001 |
| PUT | /api/v1/housing-units/{unitId}/boilers/active/replace | UC011.003 |
| GET | /api/v1/boilers/{boilerId}/services | UC011.006 |
| POST | /api/v1/boilers/{boilerId}/services | UC011.005 |

---

**Last Updated:** 2026-03-01 | **Status:** 📋 Ready for Implementation
