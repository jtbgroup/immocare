# Use Case UC005: Manage Rents

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC005 |
| **Use Case Name** | Manage Rents |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit exists |
| **Postconditions** | Rent history is updated in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages indicative rent amounts for housing units. Rents are tracked over time with full history retention, supporting rent increases, decreases, and adjustments.

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

### 1. View Rent History

**Trigger**: ADMIN views housing unit details

1. System displays Rent section showing:
   - **Current Rent**: Most recent amount (e.g., "€850.00/month")
   - **Effective From**: Start date of current rent
   - **Last Change**: Previous amount and date (if exists)
   - Link: "View History"

2. If no rent defined, show: "No rent recorded"

3. ADMIN can click "View History"

4. System displays history table:
   - Monthly Rent (€)
   - Effective From
   - Effective To
   - Duration (calculated)
   - Notes
   - Actions (View details)

5. History sorted by effective_from DESC

**Result**: ADMIN sees current and historical rents

---

### 2. Add First Rent

**Trigger**: ADMIN clicks "Set Rent" button (no rent exists yet)

1. System displays rent form:
   - **Housing Unit** (pre-selected, read-only)
   - **Monthly Rent (€)** (required, decimal)
   - **Effective From** (required, date picker)
   - **Notes** (optional, text area)

2. ADMIN fills form

3. ADMIN clicks "Save"

4. System validates:
   - Rent amount > 0
   - Effective from date provided
   - Effective from not too far in future (< 1 year)

5. System saves rent:
   - effective_to = NULL (current rent)
   - Append to history table

6. System displays success message

7. System updates unit details with new rent

**Result**: First rent is recorded

---

### 3. Update Rent (Add New Amount)

**Trigger**: ADMIN clicks "Update Rent" button (rent exists)

1. System displays rent update form:
   - **Current Rent**: Display current amount (read-only)
   - **New Monthly Rent (€)** (required, decimal)
   - **Effective From** (required, date picker, default: today)
   - **Notes** (optional, text area, placeholder: "Annual indexation", "Market adjustment", etc.)

2. System shows calculated change:
   - If increase: "+€50.00 (+5.88%)" in green
   - If decrease: "-€50.00 (-5.88%)" in red

3. ADMIN fills form

4. ADMIN clicks "Save"

5. System validates:
   - New rent amount > 0
   - Effective from >= current rent's effective_from
   - Effective from not in distant future

6. System updates rent history:
   - Set current rent's effective_to = new effective_from - 1 day
   - Add new rent record with effective_to = NULL
   - Both records preserved in history

7. System displays success message

8. System updates unit details with new rent

**Result**: Rent is updated; old rent becomes historical

---

### 4. View Rent Details

**Trigger**: ADMIN clicks on a rent in history table

1. System displays rent details modal:
   - Monthly Rent (large, formatted)
   - Effective From
   - Effective To (or "Current" if NULL)
   - Duration (calculated)
   - Notes
   - Created at timestamp

2. If not current rent, show comparison:
   - "This rent was replaced by €XXX on [date]"

**Result**: ADMIN sees complete rent information

---

### 5. Backdate Rent Entry

**Trigger**: ADMIN needs to enter historical rent retroactively

1. ADMIN clicks "Add Historical Rent"

2. System displays form with warning:
   - "Adding historical rent - ensure dates don't overlap with existing rents"

3. ADMIN enters past rent with old effective_from date

4. System validates no overlap with existing rent periods

5. System saves historical rent

**Result**: Historical rent added to timeline

---

## Alternative Flows

### Alternative Flow 3A: Validation Errors

**Trigger**: Validation fails in step 5 of Basic Flow 3

1. System displays errors:
   - "New rent is required"
   - "Rent must be positive"
   - "Effective from is required"
   - "Effective from cannot be before current rent start date"
   - "Effective from too far in future"

2. ADMIN corrects errors

3. Return to step 4 of Basic Flow 3

**Result**: Errors must be fixed

---

### Alternative Flow 3B: Same Rent Amount Warning

**Trigger**: New rent equals current rent in step 2

1. System shows warning:
   - "New rent is same as current rent"
   - "Continue anyway?"

2. ADMIN clicks "Yes" or "No"
   - Yes: Allow save (may be adjusting dates)
   - No: Return to form

**Result**: Warning shown but not blocked

---

### Alternative Flow 5A: Overlapping Dates Error

**Trigger**: Historical rent dates overlap with existing rent in step 4

1. System displays error:
   - "Date overlap detected"
   - "This date range conflicts with existing rent from [date] to [date]"
   - Show conflicting rent details

2. ADMIN adjusts dates or cancels

**Result**: Overlaps prevented

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable

1. System displays error message

2. ADMIN can retry

**Result**: Rent not saved

---

## Business Rules

### BR-UC005-01: Append-Only History
Rent records are never deleted, only appended.

**Rationale**: Complete audit trail of all rent changes

---

### BR-UC005-02: Current Rent Definition
Current rent = record with effective_to = NULL.

**Rationale**: Only one active rent per unit at any time

---

### BR-UC005-03: Automatic Period Closure
When adding new rent, system automatically sets previous rent's effective_to.

**Rationale**: Prevent manual calculation errors

---

### BR-UC005-04: No Overlapping Periods
Rent periods for same unit cannot overlap.

**Rationale**: Unit cannot have two different rents simultaneously

---

### BR-UC005-05: Positive Rent Amounts
Rent must be > 0.

**Rationale**: Negative or zero rent is not valid (free units tracked differently in future)

---

### BR-UC005-06: Future Date Limit
Effective from date cannot be more than 1 year in future.

**Rationale**: Prevent accidental far-future dates

---

### BR-UC005-07: Indicative Nature
Rents are indicative/target amounts, not actual tenant payments.

**Rationale**: Actual payments tracked separately in future (BACKLOG-004)

---

### BR-UC005-08: Sequential Dates
New rent's effective_from must be >= current rent's effective_from.

**Rationale**: Cannot backdate new rent before current period starts

---

## Data Elements

### Input Data
- Housing unit ID (selected)
- Monthly rent (decimal, EUR)
- Effective from (date)
- Notes (text, optional)

### Output Data
- Rent ID (generated)
- All input data
- Effective to (calculated or NULL)
- Created_at timestamp
- Duration (calculated)
- Change percentage (calculated)

---

## User Interface Requirements

### Rent Section (in Housing Unit Details)
- Current rent amount (large, formatted: "€850.00/month")
- Effective from date
- Rent change indicator (↑ or ↓ with percentage)
- "Update Rent" button
- "View History" link

### Rent History Modal/Page
- Timeline view (optional)
- Table with all rents
- Visual indicators for increases/decreases
- Filter by date range
- Export option

### Rent Form (Set/Update)
- Rent amount input with € prefix
- Date picker
- Notes text area with common templates
- Change calculator (update only)
- "Save" and "Cancel" buttons

---

## Performance Requirements

- Rent section loads with unit (< 500ms)
- Add/update rent completes in < 500ms
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
**Then**: Rent saved as current

### TS-UC005-02: Increase Rent
**Given**: Current rent €850 from 2024-01-01  
**When**: ADMIN sets €900 from 2024-07-01  
**Then**: New rent current, old rent effective_to = 2024-06-30

### TS-UC005-03: Decrease Rent
**Given**: Current rent €900  
**When**: ADMIN sets €850  
**Then**: Decrease recorded, percentage shown

### TS-UC005-04: Negative Rent Error
**Given**: ADMIN updating rent  
**When**: Enters amount = -100  
**Then**: Error "Rent must be positive"

### TS-UC005-05: Future Date Too Far Error
**Given**: ADMIN setting rent  
**When**: Sets effective from 2 years ahead  
**Then**: Error "Date too far in future"

### TS-UC005-06: View Rent History
**Given**: Unit has 3 rent periods  
**When**: ADMIN views history  
**Then**: All 3 shown with durations

### TS-UC005-07: Calculate Duration
**Given**: Rent from 2024-01-01 to 2024-06-30  
**When**: Viewing history  
**Then**: Duration shows "6 months"

### TS-UC005-08: Same Amount Warning
**Given**: Current rent €850  
**When**: ADMIN enters new rent €850  
**Then**: Warning shown, can continue

### TS-UC005-09: Backdate New Rent Error
**Given**: Current rent from 2024-06-01  
**When**: ADMIN tries effective from 2024-01-01  
**Then**: Error "Cannot backdate before current period"

### TS-UC005-10: Add Historical Rent
**Given**: Current rent from 2024-06-01  
**When**: ADMIN adds historical rent 2023-01-01 to 2024-05-31  
**Then**: Historical rent added successfully

---

## Related User Stories

- **US021**: Set initial rent for housing unit
- **US022**: Update rent amount
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

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation
