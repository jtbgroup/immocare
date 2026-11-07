# UC009 — Manage Meters

## Overview

| Attribute | Value |
|---|---|
| ID | UC009 |
| Name | Manage Meters |
| Actor | Admin |
| Module | Housing Units / Buildings → Meters |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Manages energy and utility meters for both housing units and buildings. Meters have a type (WATER, GAS, ELECTRICITY), a start date, and an optional end date (NULL = active). Multiple meters of the same type can coexist on the same owner. Replace creates a new meter and closes the current one atomically. Remove closes a meter without replacement.

---

## User Stories

### UC009.001 — View Meters of a Housing Unit

**As an** admin, **I want to** view the active meters associated with a housing unit **so that** I can quickly see which water, gas, and electricity meters are assigned.

**Acceptance Criteria:**
- AC1: Meters section shows 3 grouped blocks (WATER / GAS / ELECTRICITY); each block displays: meter number, EAN or installation number, start date, duration in months.
- AC2: Multiple active meters of the same type all shown in their block.
- AC3: No meter for a type → block shows "No [type] meter assigned" and an "Add Meter" button.
- AC4: No meters at all → all blocks show "No meter assigned" with "Add Meter" buttons.
- AC5: At least one meter (active or closed) → "View History" link visible.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/meters?status=active`

---

### UC009.002 — View Meters of a Building

**As an** admin, **I want to** view the active meters associated with a building **so that** I can see which common-area meters are assigned.

**Acceptance Criteria:**
- AC1: Same 3-block layout as UC009.001. WATER meters on buildings also display `customerNumber`.
- AC2: Multiple active meters of the same type all shown.
- AC3: No meter for a type → "No [type] meter assigned" and "Add Meter" button.
- AC4: No meters at all → all blocks show empty state with buttons.
- AC5: At least one meter → "View History" link visible.

**Endpoint:** `GET /api/v1/buildings/{buildingId}/meters?status=active`

---

### UC009.003 — Add a Meter to a Housing Unit

**As an** admin, **I want to** add a meter (water, gas, or electricity) to a housing unit **so that** I can track which meters serve the apartment.

**Acceptance Criteria:**
- AC1: Form fields: Type, Meter Number, conditional fields (EAN / installation number), Start Date.
- AC2: `startDate` cannot be in the future.
- AC3: `eanCode` required for GAS and ELECTRICITY — errors "EAN code is required for gas/electricity meters".
- AC4: `installationNumber` required for WATER — error "Installation number is required for water meters".
- AC5: No `customerNumber` field shown for housing unit context.
- AC6: Multiple active meters of the same type are allowed.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/meters`

---

### UC009.004 — Add a Meter to a Building

**As an** admin, **I want to** add a meter to a building **so that** I can track which meters serve the common areas.

**Acceptance Criteria:**
- AC1: Same validation as UC009.003 plus: `customerNumber` required for WATER meters — error "Customer number is required for water meters on a building".
- AC2: `eanCode` required for GAS and ELECTRICITY.
- AC3: `installationNumber` required for WATER.
- AC4: `startDate` cannot be in the future.

**Endpoint:** `POST /api/v1/buildings/{buildingId}/meters`

---

### UC009.005 — Replace a Meter

**As an** admin, **I want to** replace an active meter with a new one **so that** I can track meter changes without losing history.

**Acceptance Criteria:**
- AC1: Replace form shows current meter data read-only and fields for the new meter.
- AC2: Atomic operation: current meter gets `endDate = newStartDate`, new meter created (active).
- AC3: `newStartDate` cannot be in the future.
- AC4: `newStartDate` must be ≥ current meter's `startDate`.
- AC5: Same conditional field rules as add (EAN, installation number, customer number).
- AC6: Optional `reason` dropdown: BROKEN, END_OF_LIFE, UPGRADE, CALIBRATION_ISSUE, OTHER.
- AC7: Cancel: form closes, existing meter unchanged.

**Endpoints:**
- `PUT /api/v1/housing-units/{unitId}/meters/{meterId}/replace`
- `PUT /api/v1/buildings/{buildingId}/meters/{meterId}/replace`

---

### UC009.006 — Remove a Meter

**As an** admin, **I want to** remove an active meter without replacing it **so that** I can handle cases where a meter is disconnected with no replacement.

**Acceptance Criteria:**
- AC1: Confirmation dialog shows meter type/number, warning "This meter will be deactivated. No replacement will be created.", and a date picker (default: today).
- AC2: On confirm: meter `endDate` set, meter no longer shown as active, still visible in history with "Closed" badge.
- AC3: Other active meters on the same unit/building are unaffected.
- AC4: `endDate` cannot be before meter's `startDate`.
- AC5: `endDate` cannot be in the future.
- AC6: Cancel: dialog closes, meter remains active.

**Endpoints:**
- `DELETE /api/v1/housing-units/{unitId}/meters/{meterId}` (body: `{ endDate }`)
- `DELETE /api/v1/buildings/{buildingId}/meters/{meterId}` (body: `{ endDate }`)

---

### UC009.007 — View Meter History

**As an** admin, **I want to** view the complete meter history of a housing unit or building **so that** I can see all past and current meter assignments.

**Acceptance Criteria:**
- AC1: History table shows all records (active and closed) sorted by `startDate DESC`.
- AC2: Columns: Type, Meter Number, EAN / Installation Number, Start Date, End Date (or "—"), Duration, Status badge (Active green / Closed grey).
- AC3: For BUILDING WATER meters: `customerNumber` also shown.
- AC4: Duration for active meters = months since `startDate`; for closed = months between `startDate` and `endDate`.
- AC5: Optional filter by type.
- AC6: "View History" link not shown if the unit/building has no meters at all.

**Endpoints:**
- `GET /api/v1/housing-units/{unitId}/meters` — full history
- `GET /api/v1/buildings/{buildingId}/meters` — full history

---

## Data Model

### Table: `meter`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `type` | VARCHAR(15) | NOT NULL, CHECK IN (WATER, GAS, ELECTRICITY) |
| `meter_number` | VARCHAR(50) | NOT NULL |
| `label` | VARCHAR(100) | nullable |
| `ean_code` | VARCHAR(50) | nullable |
| `installation_number` | VARCHAR(50) | nullable |
| `customer_number` | VARCHAR(50) | nullable |
| `owner_type` | VARCHAR(15) | NOT NULL, CHECK IN (HOUSING_UNIT, BUILDING) |
| `owner_id` | BIGINT | NOT NULL |
| `start_date` | DATE | NOT NULL |
| `end_date` | DATE | nullable (NULL = active) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `MeterDTO` (response)
```
id, type, meterNumber, label, eanCode, installationNumber, customerNumber,
ownerType, ownerId, startDate, endDate,
status: 'ACTIVE' | 'CLOSED',   ← computed: endDate IS NULL → ACTIVE
createdAt
```

### `AddMeterRequest` (POST body)
```
type*              MeterType (WATER, GAS, ELECTRICITY)
meterNumber*       VARCHAR(50)
label              VARCHAR(100)
eanCode            VARCHAR(50)    required if type GAS or ELECTRICITY
installationNumber VARCHAR(50)    required if type WATER
customerNumber     VARCHAR(50)    required if type WATER and ownerType BUILDING
startDate*         LocalDate      not future
```

### `ReplaceMeterRequest` (PUT body)
```
newMeterNumber*       VARCHAR(50)
newLabel              VARCHAR(100)
newEanCode            VARCHAR(50)    conditional (same rules)
newInstallationNumber VARCHAR(50)    conditional
newCustomerNumber     VARCHAR(50)    conditional
newStartDate*         LocalDate      not future, ≥ current startDate
reason                ReplacementReason  nullable
```

### `RemoveMeterRequest` (DELETE body)
```
endDate*  LocalDate  not future, ≥ meter startDate
```

---

## Enums

### `MeterType`
`WATER`, `GAS`, `ELECTRICITY`

### `ReplacementReason`
`BROKEN`, `END_OF_LIFE`, `UPGRADE`, `CALIBRATION_ISSUE`, `OTHER`

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC009-01 | `startDate` cannot be in the future |
| BR-UC009-02 | `endDate` must be ≥ meter's `startDate` |
| BR-UC009-05 | `eanCode` required for GAS and ELECTRICITY |
| BR-UC009-06 | `installationNumber` required for WATER |
| BR-UC009-07 | `customerNumber` required for WATER on BUILDING |
| BR-UC009-08 | Replace is atomic (close current + create new in single transaction) |
| BR-UC009-09 | Replace `newStartDate` must be ≥ current meter's `startDate` |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Owner not found | 404/409 | `HousingUnitNotFoundException` or `BuildingNotFoundException` |
| Meter not found or already closed | 409 | `MeterNotFoundException` |
| Business rule violation | 409 | `MeterBusinessRuleException` |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC009 — Manage Meters in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, Lombok, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS, ReactiveFormsModule
- Database: Flyway V001 already contains `meter` table — do NOT generate a migration
- Branch: develop

User Stories: UC009.001 (View unit meters), UC009.002 (View building meters), UC009.003 (Add to unit), UC009.004 (Add to building), UC009.005 (Replace), UC009.006 (Remove), UC009.007 (View history)

Backend classes to generate:
1. Entity: `Meter` — table `meter`. Fields: id, type (String), meterNumber, label, eanCode, installationNumber, customerNumber, ownerType (String), ownerId (Long — not a FK, polymorphic), startDate, endDate, createdAt (@PrePersist). No @PreUpdate.
2. DTOs: `MeterDTO` (computed status = endDate==null ? ACTIVE : CLOSED), `AddMeterRequest` (record, validated), `ReplaceMeterRequest` (record), `RemoveMeterRequest` (record)
3. Mapper: `MeterMapper` (MapStruct) — map entity to MeterDTO, ignore status (set in service)
4. Repository: `MeterRepository` — findByOwnerTypeAndOwnerIdAndEndDateIsNull (active — UC009.001, UC009.002), findByOwnerTypeAndOwnerIdOrderByStartDateDesc (history — UC009.007), findByIdAndEndDateIsNull (for replace/remove)
5. Exceptions: `MeterNotFoundException`, `MeterBusinessRuleException`
6. Service: `MeterService` — getActiveMeters(ownerType, ownerId), getMeterHistory(ownerType, ownerId), addMeter(ownerType, ownerId, req), replaceMeter(ownerType, ownerId, meterId, req), removeMeter(ownerType, ownerId, meterId, endDate). Enforce all BR-UC009-xx. `validateOwnerExists` checks HousingUnitRepository or BuildingRepository by ownerType.
7. Controller: `MeterController` (no @RequestMapping prefix) — two sets of endpoints sharing the same service:
   - HOUSING_UNIT: /api/v1/housing-units/{unitId}/meters (GET ?status=active UC009.001, GET full UC009.007, POST UC009.003, PUT /{id}/replace UC009.005, DELETE /{id} UC009.006)
   - BUILDING: /api/v1/buildings/{buildingId}/meters (same structure — UC009.002, UC009.004)
   Uses constants HOUSING_UNIT / BUILDING to resolve ownerType.

Frontend classes to generate:
1. Model: `meter.model.ts` — MeterType, MeterOwnerType, ReplacementReason, MeterDTO, AddMeterRequest, ReplaceMeterRequest, RemoveMeterRequest, METER_TYPE_LABELS, METER_TYPE_ICONS, REPLACEMENT_REASON_LABELS, meterDurationMonths() utility
2. Service: `MeterService` — separate methods for unit and building: getUnitMeters/getBuildingMeters, getUnitActiveMeters/getBuildingActiveMeters, addUnitMeter/addBuildingMeter, replaceUnitMeter/replaceBuildingMeter, removeUnitMeter/removeBuildingMeter
3. Component: `MeterSectionComponent` (standalone, inputs: [ownerType], [ownerId]) — shared for both housing units and buildings
   - Groups meters by type (WATER, GAS, ELECTRICITY) in TypeBlock structure (UC009.001, UC009.002)
   - Each block shows active meter(s) with meterNumber, label, eanCode/installationNumber/customerNumber, startDate, duration in months
   - Per-block panel states: idle | add | replace | remove
   - "Add" opens inline form for the block's type (UC009.003, UC009.004)
   - "Replace" opens form with new meter fields and reason dropdown (UC009.005)
   - "Remove" opens confirmation dialog with date picker (UC009.006)
   - "View History" toggle loads and shows full history table with status badges (UC009.007): columns Type, Meter Number, EAN/Installation, Start Date, End Date, Duration, Status
   - Conditional fields shown/hidden based on type and ownerType

Business rules to enforce in frontend:
- eanCode shown/required only for GAS and ELECTRICITY (UC009.003 AC3, UC009.004 AC2)
- installationNumber shown/required only for WATER (UC009.003 AC4, UC009.004 AC3)
- customerNumber shown/required only for WATER when ownerType=BUILDING (UC009.004 AC1)
- startDate: max = today (UC009.003 AC2)
- replaceMeter: newStartDate min = current meter's startDate (UC009.005 AC4)
```
