# Use Case UC005: Manage Rents

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC005 |
| **Use Case Name** | Manage Rents |
| **Version** | 2.0 |
| **Status** | ✅ Implemented |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit exists |
| **Postconditions** | Rent history is updated in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages indicative rent amounts for housing units. Rents are tracked over time with full history. Records can be added, edited, and deleted — adjacent periods are automatically recalculated on any change.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Record and track rent amounts over time
- **Characteristics**:
  - Knows rent pricing for units
  - Tracks rent changes (indexation, market adjustments)

---

## Preconditions

1. User authenticated as ADMIN
2. Housing unit exists
3. System operational

---

## Basic Flow

### 1. View Current Rent

**Trigger**: ADMIN views housing unit details

1. System displays Rent section showing:
   - **Current Rent**: Most recent amount as badge (e.g., "€900")
   - **Effective From**: Start date of current rent
   - **Last Change**: vs previous amount with ↑/↓ indicator (if history exists)
   - Link: "View History"
   - Button: "+ Set Rent" (only if no rent defined)

2. If no rent defined, show: "No rent recorded" + "+ Set Rent" button

**Result**: ADMIN sees current rent at a glance

---

### 2. Add First Rent

**Trigger**: ADMIN clicks "+ Set Rent" button (no rent exists yet)

1. System displays inline rent form:
   - **Monthly Rent (€)** (required, decimal)
   - **Effective From** (required, date picker)
   - **Notes** (optional, text area with quick-select templates)

2. ADMIN fills form and clicks "Save"

3. System validates:
   - Rent amount > 0
   - Effective from date provided
   - Effective from not more than 1 year in the future

4. System saves rent record with `effective_to = NULL`

5. System refreshes section with new current rent

**Result**: First rent is recorded

---

### 3. Add New Rent Record

**Trigger**: ADMIN clicks "+ Set Rent" (rent already exists — button visible when no current rent, e.g., after a delete)

1. Same form as Flow 2

2. System inserts new record at the correct position in the timeline:
   - If most recent date → previous record gets `effective_to = new effectiveFrom - 1 day`
   - If inserted in middle → gets `effective_to = next record's effectiveFrom - 1 day`, previous record gets `effective_to = new effectiveFrom - 1 day`

3. System recalculates adjacent periods automatically

**Result**: New rent inserted; adjacent periods recalculated

---

### 4. Edit a Rent Record

**Trigger**: ADMIN clicks ✏️ on a row in the history table

1. System opens inline form pre-filled with current values:
   - Monthly Rent (editable)
   - Effective From (editable)
   - Notes (editable)
   - If editing current rent: live change preview vs previous

2. ADMIN modifies fields and clicks "Save"

3. System validates (same rules as creation)

4. System updates the record and recalculates neighbours:
   - Previous record (older): `effective_to = new effectiveFrom - 1 day`
   - This record: `effective_to = next record's effectiveFrom - 1 day` (or NULL if most recent)

5. System refreshes current rent card and history

**Result**: Record corrected; adjacent periods recalculated

---

### 5. Delete a Rent Record

**Trigger**: ADMIN clicks 🗑️ on a row in the history table

1. System shows inline confirmation with record details

2. ADMIN clicks "Confirm Delete"

3. System deletes the record and recalculates neighbours:
   - Previous record (older) inherits the deleted record's `effective_to`
   - If deleted record was the most recent → previous record becomes current (`effective_to = NULL`)

4. System refreshes current rent card and history

**Result**: Record removed; previous record inherits the period

---

### 6. View Rent History

**Trigger**: ADMIN clicks "View History" link

1. System expands inline history panel below the card

2. Panel shows:
   - Total change summary (first rent → current): "+€100 (+13.33%)" in green/red
   - Table sorted by `effectiveFrom` DESC

3. Table columns:
   - Monthly Rent
   - From
   - To (or "Current")
   - Duration (months)
   - Change vs previous (↑/↓ with amount and %)
   - Notes
   - Actions (✏️ 🗑️)

4. ADMIN clicks "Hide History" or ✕ to collapse

**Result**: Full rent history visible inline

---

## Alternative Flows

### Alternative Flow 2A / 3A / 4A: Validation Errors

**Trigger**: Validation fails on save

1. System displays inline errors:
   - "Rent must be positive"
   - "Effective from date is required"
   - "Effective from date cannot be more than 1 year in the future"

2. ADMIN corrects errors and retries

**Result**: Errors must be fixed before saving

---

### Alternative Flow 2B: Same Rent Amount Warning

**Trigger**: New rent amount equals current rent

1. System shows warning: "New rent is the same as the current rent. Continue anyway?"

2. ADMIN can save anyway (useful for date corrections)

**Result**: Warning shown but not blocked

---

### Alternative Flow 5A: Delete with No Previous Record

**Trigger**: ADMIN deletes the only rent record

1. System deletes the record

2. Section returns to empty state: "No rent recorded" + "+ Set Rent" button

**Result**: Unit has no rent recorded

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable

1. System displays inline error message

2. ADMIN can retry

**Result**: Operation not performed

---

## Business Rules

### BR-UC005-01: Full Audit Trail
Rent records can be added, edited, and deleted. All changes automatically recalculate adjacent `effective_to` dates to maintain a consistent, non-overlapping timeline.

**Rationale**: Data accuracy is more important than append-only immutability; recalculation ensures the timeline is always coherent

---

### BR-UC005-02: Current Rent Definition
Current rent = record with `effective_to = NULL`. Only one per unit at any time.

**Rationale**: Only one active rent per unit at any time

---

### BR-UC005-03: Automatic Period Recalculation
On any add, edit, or delete:
- Previous record (older): `effective_to = new/edited effectiveFrom - 1 day`
- Deleted record: previous record inherits deleted record's `effective_to`

**Rationale**: Prevent manual calculation errors; always maintain a gapless timeline

---

### BR-UC005-04: No Overlapping Periods
Rent periods for the same unit cannot overlap. The system enforces this via automatic recalculation.

**Rationale**: Unit cannot have two different rents simultaneously

---

### BR-UC005-05: Positive Rent Amounts
Rent must be > 0.

**Rationale**: Negative or zero rent is not valid (free units tracked differently in future)

---

### BR-UC005-06: Future Date Limit
`effective_from` cannot be more than 1 year in the future.

**Rationale**: Prevent accidental far-future dates

---

### BR-UC005-07: Indicative Nature
Rents are indicative/target amounts, not actual tenant payments.

**Rationale**: Actual payments tracked separately in future (BACKLOG-004)

---

## Data Elements

### Input Data
- Housing unit ID (pre-selected)
- Monthly rent (decimal, EUR)
- Effective from (date)
- Notes (text, optional)

### Output Data
- Rent ID (generated)
- All input data
- Effective to (calculated or NULL)
- Created at timestamp
- Duration (calculated)
- Change percentage (calculated)

---

## User Interface Requirements

### Rent Section (in Housing Unit Details)
- Badge showing current rent amount (e.g., "€900")
- Effective from date
- Rent change indicator (↑/↓ with amount and %) vs previous
- "+ Set Rent" button (header, only when no rent exists)
- "View History" link (below card, only when rent exists)

### Rent History Panel (inline, expandable)
- Total change summary since first rent
- Table sorted by date descending
- Columns: Monthly Rent | From | To | Duration | Change | Notes | Actions
- ✏️ Edit and 🗑️ Delete buttons per row
- ✕ button to close panel

### Rent Form (inline panel)
- Monthly rent input (€, required)
- Effective from date picker (required)
- Live change preview vs current rent (edit mode only)
- Notes field with quick-select templates: "Annual indexation", "Market adjustment", "After renovation", "Tenant negotiation"
- "Save" and "Cancel" buttons

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/housing-units/{unitId}/rents` | Full history, newest first |
| GET | `/api/v1/housing-units/{unitId}/rents/current` | Current rent (204 if none) |
| POST | `/api/v1/housing-units/{unitId}/rents` | Add new rent record |
| PUT | `/api/v1/housing-units/{unitId}/rents/{rentId}` | Edit existing record |
| DELETE | `/api/v1/housing-units/{unitId}/rents/{rentId}` | Delete record |

---

## Performance Requirements

- Rent section loads with unit (< 500ms)
- Add/edit/delete rent completes in < 500ms
- History loads in < 500ms

---

## Security Requirements

- Only ADMIN can manage rents
- Rent data considered financially sensitive

---

## Test Scenarios

### TS-UC005-01: Set First Rent
**Given**: Unit has no rent
**When**: ADMIN sets rent €850 from 2024-01-01
**Then**: Rent saved as current with `effective_to = NULL`

### TS-UC005-02: Add More Recent Rent
**Given**: Current rent €850 from 2024-01-01
**When**: ADMIN adds €900 from 2024-07-01
**Then**: New rent current; old rent `effective_to = 2024-06-30`

### TS-UC005-03: Add Rent in Middle of History
**Given**: History has €800 (2023-01-01) and €900 (2024-01-01)
**When**: ADMIN adds €850 from 2023-07-01
**Then**: €800 gets `effective_to = 2023-06-30`; €850 gets `effective_to = 2023-12-31`

### TS-UC005-04: Edit Rent Date
**Given**: Rent €850 from 2024-01-01 (current)
**When**: ADMIN edits date to 2024-03-01
**Then**: Previous record gets `effective_to = 2024-02-28`

### TS-UC005-05: Delete Current Rent
**Given**: Two records: €800 (2023) and €900 (2024, current)
**When**: ADMIN deletes €900
**Then**: €800 becomes current (`effective_to = NULL`)

### TS-UC005-06: Delete Middle Record
**Given**: Three records: €750 (2022), €800 (2023), €900 (2024)
**When**: ADMIN deletes €800
**Then**: €750 `effective_to` updated to 2023-12-31 (was end of €800's period)

### TS-UC005-07: Negative Rent Error
**Given**: ADMIN adding rent
**When**: Enters amount = -100
**Then**: Error "Rent must be positive"

### TS-UC005-08: Future Date Too Far Error
**Given**: ADMIN adding rent
**When**: Sets effective from 2 years ahead
**Then**: Error "Date too far in future"

### TS-UC005-09: View Rent History
**Given**: Unit has 3 rent periods
**When**: ADMIN clicks "View History"
**Then**: Panel expands inline; all 3 shown with durations and ↑/↓ indicators

### TS-UC005-10: Calculate Duration
**Given**: Rent from 2024-01-01 to 2024-06-30
**When**: Viewing history
**Then**: Duration shows "5 months"

### TS-UC005-11: Same Amount Warning
**Given**: Current rent €850
**When**: ADMIN enters new rent €850
**Then**: Warning shown, can continue

### TS-UC005-12: Delete Only Record
**Given**: Unit has exactly one rent record
**When**: ADMIN deletes it
**Then**: Section shows "No rent recorded" + "+ Set Rent" button

---

## Related User Stories

- **US021**: Set initial rent for housing unit
- **US022**: Edit a rent record
- **US023**: View rent history
- **US024**: Track rent increases over time
- **US025**: Add notes to rent changes

---

## Notes

- Rent is indicative (market rate), not actual tenant payments
- Currency fixed as EUR for Phase 1
- Typical rent changes: annual indexation, market adjustment, renovation
- Future: Link rent to actual lease contracts (BACKLOG-002)
- Future: Calculate indexation automatically based on official index
- Future: Support different currencies (BACKLOG-030)
- Future: Rent comparison across portfolio

---

**Last Updated**: 2026-02-23
**Version**: 2.0
**Status**: ✅ Implemented
