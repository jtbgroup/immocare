# Use Case UC008: Manage Meters

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC008 |
| **Use Case Name** | Manage Meters |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit or Building exists |
| **Postconditions** | Meter history is updated in the system |
| **Related Use Cases** | UC002 (Manage Housing Units), UC001 (Manage Buildings) |

---

## Description

This use case describes how an administrator manages utility meters (water, gas, electricity) associated with housing units and buildings. Meters are tracked over time using an append-only history: when a meter changes, the current record is closed (end_date set) and a new record is created. Multiple meters of the same type can coexist for a single owner.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Record and track meter assignments over time for housing units and buildings
- **Characteristics**:
  - Has meter identification data (meter number, EAN code, installation number, customer number)
  - Knows installation and removal dates

---

## Preconditions

1. User authenticated as ADMIN
2. Housing unit or Building exists
3. System operational

---

## Data Model

### Table: `meter`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-generated |
| `type` | ENUM(WATER, GAS, ELECTRICITY) | Meter type |
| `meter_number` | VARCHAR(50) NOT NULL | Physical meter identifier — all types |
| `ean_code` | VARCHAR(18) | Required for GAS and ELECTRICITY |
| `installation_number` | VARCHAR(50) | Required for WATER |
| `customer_number` | VARCHAR(50) | Required for WATER on a BUILDING |
| `owner_type` | ENUM(HOUSING_UNIT, BUILDING) | Owner context |
| `owner_id` | BIGINT NOT NULL | FK to housing_unit.id or building.id |
| `start_date` | DATE NOT NULL | Activation date |
| `end_date` | DATE NULL | Closure date — NULL = active |
| `created_at` | TIMESTAMP NOT NULL | Record creation timestamp |

### Business Rules

| Rule | Description |
|---|---|
| BR-01 | `start_date` cannot be in the future |
| BR-02 | `end_date` must be ≥ `start_date` |
| BR-03 | Multiple active meters of the same type are allowed per owner |
| BR-04 | A meter belongs to exactly one owner (no sharing) |
| BR-05 | `ean_code` is required for GAS and ELECTRICITY meters |
| BR-06 | `installation_number` is required for WATER meters |
| BR-07 | `customer_number` is required for WATER meters on a BUILDING |
| BR-08 | Append-only: existing records are never modified — closing a meter sets `end_date` and creates a new record |

---

## Basic Flow

### 1. View Meters of a Housing Unit

**Trigger**: ADMIN views housing unit details

1. System displays the Meters section with active meters grouped by type (WATER, GAS, ELECTRICITY)
2. Each active meter shows: type, meter number, EAN or installation number, start date, duration
3. For each type, if no active meter exists: "No [type] meter assigned"
4. ADMIN can click "View History" to see all records (active + closed) for that owner

**Result**: ADMIN sees all active meters and can access history

---

### 2. View Meters of a Building

**Trigger**: ADMIN views building details

Same flow as §1, applied to the building context. The customer number is additionally displayed for WATER meters.

---

### 3. Add a Meter

**Trigger**: ADMIN clicks "Add Meter" on a housing unit or building details page

1. System displays the add meter form:
   - **Type** (required): WATER / GAS / ELECTRICITY
   - **Meter Number** (required)
   - **EAN Code** (required if GAS or ELECTRICITY)
   - **Installation Number** (required if WATER)
   - **Customer Number** (required if WATER and owner is BUILDING)
   - **Start Date** (required, date picker, default: today)

2. ADMIN fills the form and clicks "Save"

3. System validates according to BR-01, BR-05, BR-06, BR-07

4. System saves the meter with `end_date = NULL` (active)

5. System displays success message and updates the meters section

**Result**: New meter is active and visible in the section

---

### 4. Replace a Meter (Append-Only)

**Trigger**: ADMIN clicks "Replace" on an active meter

1. System displays the replace form:
   - Current meter shown read-only
   - New meter fields: Type (pre-filled, read-only), Meter Number, EAN / Installation Number / Customer Number, Start Date
   - **Reason** (optional): BROKEN / END_OF_LIFE / UPGRADE / CALIBRATION_ISSUE / OTHER

2. ADMIN fills the new meter fields and clicks "Save"

3. System validates:
   - New meter fields per type rules (BR-05 to BR-07)
   - New `start_date` ≥ current meter's `start_date` (BR-02)
   - New `start_date` not in future (BR-01)

4. System atomically:
   - Sets `end_date` on the current meter = new meter's `start_date`
   - Creates new meter record with `end_date = NULL`

5. System displays success message

**Result**: Old meter is closed, new meter is active

---

### 5. Remove a Meter (Without Replacement)

**Trigger**: ADMIN clicks "Remove" on an active meter

1. System displays confirmation dialog:
   - Meter type and number shown
   - Warning: "This meter will be deactivated. No replacement will be created."
   - Date picker for `end_date` (default: today)

2. ADMIN confirms and clicks "Remove"

3. System validates:
   - `end_date` ≥ meter's `start_date` (BR-02)
   - `end_date` not in future (BR-01)

4. System sets `end_date` on the meter record

5. System displays success message and updates section

**Result**: Meter is closed, no active meter of that type remains (unless others exist)

---

### 6. View Meter History

**Trigger**: ADMIN clicks "View History" on a meters section

1. System displays a history table for all meters of that owner, sorted by `start_date` DESC
2. Columns: Type, Meter Number, EAN / Installation Number, Start Date, End Date (or "Active"), Duration, Status badge
3. Active meters show green "Active" badge; closed meters show gray "Closed" badge

**Result**: ADMIN sees full meter history

---

## Alternative Flows

### A1: Cancel Any Form
**When**: ADMIN clicks "Cancel" during add / replace / remove
**Then**: Form closes, no data is modified

### A2: Validation Failure
**When**: Any required field is missing or invalid
**Then**: Inline error message displayed, form not submitted

---

## Test Scenarios

| ID | Scenario | Expected Result |
|---|---|---|
| TS-UC008-01 | Add GAS meter without EAN code | Error: "EAN code is required for gas meters" |
| TS-UC008-02 | Add WATER meter on BUILDING without customer number | Error: "Customer number is required for water meters on a building" |
| TS-UC008-03 | Add meter with future start_date | Error: "Start date cannot be in the future" |
| TS-UC008-04 | Replace meter with new start_date before current start_date | Error: "Start date must be ≥ current meter start date" |
| TS-UC008-05 | Remove meter with end_date before start_date | Error: "End date must be ≥ start date" |
| TS-UC008-06 | Add two ELECTRICITY meters to same unit | Both saved and shown as active |
| TS-UC008-07 | Replace meter successfully | Old meter closed, new meter active, both in history |
| TS-UC008-08 | View history with 3 past meters | Table shows 3 rows sorted by start_date DESC |

---

## Related User Stories

- **US036**: View meters of a housing unit
- **US037**: View meters of a building
- **US038**: Add a meter to a housing unit
- **US039**: Add a meter to a building
- **US040**: Replace a meter
- **US041**: Remove a meter
- **US042**: View meter history

---

**Last Updated**: 2025-02-23
**Version**: 1.0
**Status**: ✅ Ready for Implementation
