# UC005 — Manage Rents

## Overview

| Attribute | Value |
|---|---|
| ID | UC005 |
| Name | Manage Rents (Housing Unit) |
| Actor | Admin |
| Module | Housing Units → Rent History |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Manages the rent history of a **housing unit** (not a lease). This is a standalone timeline of monthly rent amounts with effective date ranges. It is independent from lease rent adjustments. Only one record at a time can have `effectiveTo = NULL` (the current rent). Inserting or editing a record automatically recalculates adjacent `effectiveTo` values to maintain a consistent timeline.

> **Important distinction:** `rent_history` belongs to a `housing_unit` and represents the "market rent" of the unit. `lease_rent_adjustment` belongs to a `lease` and represents adjustments to a specific tenant's rent. These are two separate features.

---

## User Stories

### US021 — Set Initial Rent

**As an** admin, **I want to** set the initial rent amount for a housing unit **so that** I can establish the baseline rental price.

**Acceptance Criteria:**
- AC1: When a unit has no rent defined, the Rent section shows "No rent recorded" and a "Set Rent" button.
- AC2: Required fields: `monthlyRent` (> 0), `effectiveFrom`. Validation: "Rent must be positive", "Effective from date is required".
- AC3: `effectiveFrom` cannot be more than 1 year in the future.
- AC4: The new record is inserted in chronological order; if most recent, `effectiveTo = NULL` and the previous current record's `effectiveTo` is set to `effectiveFrom - 1 day`.
- AC5: Optional `notes` stored with the record.
- AC6: Currency displayed with € symbol formatted as "€850.00/month".

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rents`

---

### US022 — Edit a Rent Record

**As an** admin, **I want to** edit an existing rent record **so that** I can correct mistakes or adjust amounts and dates.

**Acceptance Criteria:**
- AC1: Each row in history has an edit (✏️) button; the inline form opens pre-filled with current values.
- AC2: When editing an amount, a live change preview is shown: "+€50.00 (+5.88%)" in green or "-€50.00" in red.
- AC3: After saving, adjacent `effectiveTo` values are recalculated to maintain a consistent timeline.
- AC4: Updated current rent card reflects the new amount and date.
- AC5: Validation — amount must be positive; date cannot be more than 1 year in the future.

**Endpoint:** `PUT /api/v1/housing-units/{unitId}/rents/{rentId}`

---

### US023 — View Rent History

**As an** admin, **I want to** view the complete rent history of a housing unit **so that** I can see all past rent amounts and changes.

**Acceptance Criteria:**
- AC1: The Rent section on unit details shows the current rent (e.g., "€900.00/month") with "Effective from" date and a "View History" link.
- AC2: Clicking "View History" shows all rent periods sorted newest first.
- AC3: Table columns: Monthly Rent (€), Effective From, Effective To ("Current" when NULL), Duration (calculated in months), Notes.
- AC4: Duration: e.g., 2024-01-01 to 2024-06-30 = "5 months" (not 6).
- AC5: On the unit details, shows "Last change: +€100 on 2024-07-01".

**Endpoints:**
- `GET /api/v1/housing-units/{unitId}/rents`
- `GET /api/v1/housing-units/{unitId}/rents/current`

---

### US024 — Track Rent Increases Over Time

**As an** admin, **I want to** track how rent has increased over time **so that** I can analyze rental income trends.

**Acceptance Criteria:**
- AC1: Per-period trend indicators: "+€50 (+6.67%) ↑" or "-€50 (-5.56%) ↓".
- AC2: Total increase summary: "Total increase: +€100 (+13.33%) over 2 years".
- AC3: Decrease shows a red indicator with ↓.
- AC4: Optional line chart of rent evolution over time.

---

### US025 — Add Notes to Rent Changes

**As an** admin, **I want to** add notes when changing rent **so that** I can document the reason for the change.

**Acceptance Criteria:**
- AC1: Notes can be added when setting or updating rent.
- AC2: Notes are displayed in the history table (empty column if null).
- AC3: Notes field is optional — rent saves successfully without notes.
- AC4: Common templates available (optional): "Annual indexation", "Market adjustment", "After renovation", "Tenant negotiation".

---

## Data Model

### Table: `rent_history`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `housing_unit_id` | BIGINT | NOT NULL, FK → `housing_unit.id` ON DELETE CASCADE |
| `monthly_rent` | NUMERIC(10,2) | NOT NULL, CHECK > 0 |
| `effective_from` | DATE | NOT NULL |
| `effective_to` | DATE | nullable (NULL = current rent) |
| `notes` | VARCHAR(500) | nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

**Constraint:** `UNIQUE (housing_unit_id, effective_to) DEFERRABLE INITIALLY DEFERRED` — ensures only one current (NULL) record per unit.

---

## DTOs

### `RentHistoryDTO` (response)
```
id, housingUnitId, monthlyRent, effectiveFrom, effectiveTo,
notes, createdAt, isCurrent, durationMonths
```

`durationMonths` = months between `effectiveFrom` and `effectiveTo` (or today if current).

### `SetRentRequest` (POST/PUT body)
```
monthlyRent*   BigDecimal  (> 0.01)
effectiveFrom* LocalDate   (not more than 1 year in future)
notes          String      (max 500 chars)
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC005-01 | `monthlyRent` must be > 0 |
| BR-UC005-02 | `effectiveFrom` cannot be more than 1 year in the future |
| BR-UC005-03 | Only one record per unit can have `effectiveTo = NULL` (the current rent) |
| BR-UC005-04 | Inserting a record automatically recalculates adjacent `effectiveTo` values |
| BR-UC005-05 | Deleting a record automatically restores the previous record's `effectiveTo` |

---

## Timeline Logic

```
Records ordered by effectiveFrom ASC:
  [R1: from=2022-01-01, to=2022-12-31]
  [R2: from=2023-01-01, to=2023-06-30]
  [R3: from=2023-07-01, to=null]   ← current

Insert R_new at 2022-07-01:
  R1.effectiveTo = 2022-06-30
  R_new: from=2022-07-01, to=2022-12-31

Insert R_new2 at 2024-01-01:
  R3.effectiveTo = 2023-12-31
  R_new2: from=2024-01-01, to=null  ← becomes current
```

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Unit not found | 404 | `HousingUnitNotFoundException` |
| Rent record not found | 409 (via IllegalArgument) | `DUPLICATE` |
| Date too far in future | 409 | `DUPLICATE` |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC005 — Manage Rents in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS
- Database: Flyway V001 already contains `rent_history` table — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Entity: `RentHistory` — table `rent_history`. Constructor: (HousingUnit, monthlyRent, effectiveFrom, effectiveTo, notes). @PrePersist on createdAt. No @PreUpdate (no updatedAt column).
2. DTOs: `RentHistoryDTO` (record with computed isCurrent, durationMonths), `SetRentRequest` (validated record — monthlyRent @DecimalMin("0.01"), effectiveFrom @NotNull, notes @Size(max=500))
3. Mapper: `RentHistoryMapper` (MapStruct) — housingUnit.id → housingUnitId. isCurrent and durationMonths set manually in service.
4. Repository: `RentHistoryRepository` — findByHousingUnitIdOrderByEffectiveFromDesc, findByHousingUnitIdAndEffectiveToIsNull, findById (plain JPA)
5. Service: `RentHistoryService`:
   - getRentHistory(unitId): returns list with isCurrent and durationMonths computed
   - getCurrentRent(unitId): returns Optional<RentHistoryDTO>
   - addRent(unitId, req): validates date (max 1 year future), inserts in sorted order, recalculates adjacent effectiveTo values using the sorted list. @Transactional.
   - updateRent(unitId, rentId, req): updates fields, recalculates timeline. @Transactional.
   - deleteRent(unitId, rentId): removes record, restores previous effectiveTo. @Transactional.
6. Controller: `RentHistoryController` — @RequestMapping("/api/v1/housing-units/{unitId}/rents"), @PreAuthorize("hasRole('ADMIN')")

Timeline recalculation algorithm:
- Load all records for unit ordered by effectiveFrom ASC
- Find insertion position
- Set new record's effectiveTo = next.effectiveFrom - 1 day (or null if newest)
- Set previous record's effectiveTo = new.effectiveFrom - 1 day
- Save all modified records in single transaction

Frontend classes to generate:
1. Model: `rent.model.ts` — RentHistory, SetRentRequest, RentChange (amount, percentage, isIncrease), computeRentChange(from, to)
2. Service: `RentService` (or `RentHistoryService`) — getRentHistory(unitId), getCurrentRent(unitId), addRent(unitId, req), updateRent(unitId, rentId, req), deleteRent(unitId, rentId)
3. Component: `RentSectionComponent` (standalone, [unitId] input, inside HousingUnitDetailsComponent)
   - Shows current rent prominently (badge with €/month)
   - "Show History" toggle reveals full history table
   - History table columns: Monthly Rent, From, To, Duration (months), Change (delta + %, colored), Notes, Actions
   - "Add Rent" button opens inline form
   - Edit/delete per row with confirmation
   - Total change summary (first → current, € and %)

Business rules to enforce in frontend:
- monthlyRent > 0
- effectiveFrom: max = today + 1 year (date picker constraint)
- Computed RentChange displayed with ↑/↓ arrow and color (green/red)
```
