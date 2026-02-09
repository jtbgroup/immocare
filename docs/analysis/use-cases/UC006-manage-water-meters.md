# Use Case UC006: Manage Water Meters

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC006 |
| **Use Case Name** | Manage Water Meters |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit exists |
| **Postconditions** | Water meter history is updated in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages water meter assignments for housing units. Meters are tracked over time with installation and removal dates, supporting meter replacements and changes.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Record and track water meter assignments over time
- **Characteristics**: 
  - Has meter identification information
  - Knows installation/removal dates

---

## Preconditions

1. User authenticated as ADMIN
2. Housing unit exists
3. System operational

---

## Basic Flow

### 1. View Water Meter History

**Trigger**: ADMIN views housing unit details

1. System displays Water Meter section showing:
   - **Current Meter**: Meter number
   - **Installation Date**: When meter was installed
   - **Location**: Physical location (if specified)
   - Link: "View History"

2. If no meter assigned, show: "No water meter assigned"

3. ADMIN can click "View History"

4. System displays history table:
   - Meter Number
   - Location
   - Installation Date
   - Removal Date (or "Active")
   - Duration (calculated)
   - Actions (View details)

5. History sorted by installation_date DESC

**Result**: ADMIN sees current and historical meters

---

### 2. Assign First Water Meter

**Trigger**: ADMIN clicks "Assign Meter" button (no meter exists)

1. System displays meter assignment form:
   - **Housing Unit** (pre-selected, read-only)
   - **Meter Number** (required, text, max 50 chars)
   - **Meter Location** (optional, text, max 100 chars)
   - **Installation Date** (required, date picker)

2. ADMIN fills form

3. ADMIN clicks "Save"

4. System validates:
   - Meter number provided
   - Meter number format (alphanumeric with hyphens)
   - Installation date not in future
   - Installation date reasonable (not too far in past)

5. System saves meter:
   - removal_date = NULL (active meter)
   - Append to history table

6. System displays success message

7. System updates unit details with new meter

**Result**: First meter is assigned

---

### 3. Replace Water Meter

**Trigger**: ADMIN clicks "Replace Meter" button (meter exists)

1. System displays meter replacement form:
   - **Current Meter**: Display current meter number (read-only)
   - **Current Installation Date**: Show date (read-only)
   - **New Meter Number** (required, text)
   - **New Meter Location** (optional, text)
   - **Installation Date** (required, date picker, default: today)
   - **Reason**: Dropdown (Broken, End of life, Upgrade, Calibration issue, Other)

2. ADMIN fills form

3. ADMIN clicks "Save"

4. System validates:
   - New meter number different from current
   - Installation date >= current meter's installation date
   - Installation date not in future

5. System updates meter history:
   - Set current meter's removal_date = new installation date
   - Add new meter record with removal_date = NULL
   - Both records preserved in history

6. System displays success message

7. System updates unit details with new meter

**Result**: Meter is replaced; old meter becomes historical

---

### 4. View Meter Details

**Trigger**: ADMIN clicks on a meter in history table

1. System displays meter details modal:
   - Meter Number (large)
   - Location
   - Installation Date
   - Removal Date (or "Active")
   - Duration (calculated)
   - Status (Active / Replaced)
   - Created at timestamp

2. If replaced, show:
   - "Replaced by meter [number] on [date]"

**Result**: ADMIN sees complete meter information

---

### 5. Remove Meter (No Replacement)

**Trigger**: ADMIN clicks "Remove Meter" button

1. System displays confirmation:
   - "Remove water meter?"
   - Meter number shown
   - "Unit will have no active meter"
   - "You can assign a new meter later"

2. ADMIN clicks "Confirm"

3. System prompts for removal date:
   - Date picker (default: today)

4. System validates removal date >= installation date

5. System updates current meter's removal_date

6. System displays success message

7. System updates unit details (no active meter)

**Result**: Meter removed, unit has no active meter

---

## Alternative Flows

### Alternative Flow 2A: Validation Errors

**Trigger**: Validation fails

1. System displays errors:
   - "Meter number is required"
   - "Meter number invalid format"
   - "Installation date is required"
   - "Installation date cannot be in future"
   - "Installation date too far in past (> 50 years)"

2. ADMIN corrects errors

3. Return to previous step

**Result**: Errors must be fixed

---

### Alternative Flow 3A: Same Meter Number Warning

**Trigger**: New meter number equals current meter in step 4

1. System shows warning:
   - "New meter number is same as current"
   - "This is unusual. Continue?"

2. ADMIN clicks "Yes" or "No"
   - Yes: Allow (edge case: recalibrated same meter)
   - No: Return to form

**Result**: Warning shown but not blocked

---

### Alternative Flow 3B: Installation Date Too Early Error

**Trigger**: New installation date < current meter's installation date

1. System displays error:
   - "Installation date cannot be before current meter was installed"
   - "Current meter installed on [date]"

2. ADMIN adjusts date

**Result**: Prevents illogical timeline

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable

1. System displays error message

2. ADMIN can retry

**Result**: Meter not saved

---

## Business Rules

### BR-UC006-01: Append-Only History
Meter records are never deleted, only appended.

**Rationale**: Complete audit trail of all meter changes

---

### BR-UC006-02: Active Meter Definition
Active meter = record with removal_date = NULL.

**Rationale**: Only one active meter per unit at any time

---

### BR-UC006-03: Automatic Removal Date
When replacing meter, system automatically sets old meter's removal_date = new installation_date.

**Rationale**: Simplify data entry, ensure continuity

---

### BR-UC006-04: No Overlapping Meters
Only one meter can be active at a time for a unit.

**Rationale**: Physical constraint - one water supply per unit

---

### BR-UC006-05: Installation Date Validation
Installation date cannot be in future.

**Rationale**: Cannot install meter in future

---

### BR-UC006-06: Chronological Order
Removal date must be >= installation date.

**Rationale**: Cannot remove before installation

---

### BR-UC006-07: Meter Number Format
Meter number should be alphanumeric with optional hyphens/underscores.

**Rationale**: Standard utility meter numbering

---

### BR-UC006-08: Optional Location
Location is optional but recommended.

**Rationale**: Location may not be known at entry time

---

## Data Elements

### Input Data
- Housing unit ID (selected)
- Meter number (string, max 50)
- Meter location (string, max 100, optional)
- Installation date (date)

### Output Data
- Meter ID (generated)
- All input data
- Removal date (calculated or NULL)
- Created_at timestamp
- Duration (calculated)
- Status (Active/Replaced)

---

## User Interface Requirements

### Water Meter Section (in Housing Unit Details)
- Current meter number
- Installation date
- Location (if specified)
- Status badge (Active)
- "Replace Meter" button
- "Remove Meter" button
- "View History" link

### Meter History Modal/Page
- Timeline view (optional)
- Table with all meters
- Status indicators
- Filter by date range

### Meter Form (Assign/Replace)
- Meter number input
- Location input with help text
- Date picker
- Reason dropdown (replace only)
- "Save" and "Cancel" buttons

---

## Performance Requirements

- Meter section loads with unit (< 500ms)
- Assign/replace meter completes in < 500ms
- History loads in < 500ms

---

## Security Requirements

- Only ADMIN can manage meters
- Meter data visible to authenticated users

---

## Test Scenarios

### TS-UC006-01: Assign First Meter
**Given**: Unit has no meter  
**When**: ADMIN assigns meter "WM-2024-001" from 2024-01-01  
**Then**: Meter saved as active

### TS-UC006-02: Replace Meter
**Given**: Active meter "WM-2024-001" from 2024-01-01  
**When**: ADMIN replaces with "WM-2024-002" on 2024-06-01  
**Then**: Old meter removal_date = 2024-06-01, new meter active

### TS-UC006-03: Future Installation Error
**Given**: ADMIN assigning meter  
**When**: Sets installation date = tomorrow  
**Then**: Error "Installation date cannot be future"

### TS-UC006-04: View Meter History
**Given**: Unit has 3 meters: 2022, 2023, 2024  
**When**: ADMIN views history  
**Then**: All 3 shown with durations

### TS-UC006-05: Calculate Duration
**Given**: Meter from 2024-01-01 to 2024-06-01  
**When**: Viewing history  
**Then**: Duration shows "5 months"

### TS-UC006-06: Remove Meter
**Given**: Active meter exists  
**When**: ADMIN removes it with date 2024-12-31  
**Then**: Meter marked removed, unit has no active meter

### TS-UC006-07: Same Meter Number Warning
**Given**: Current meter "WM-001"  
**When**: ADMIN replaces with "WM-001"  
**Then**: Warning shown, can continue

### TS-UC006-08: Invalid Meter Format Error
**Given**: ADMIN assigning meter  
**When**: Enters meter number with special chars "WM@#$%"  
**Then**: Error "Invalid meter number format"

### TS-UC006-09: Backdate Installation Error
**Given**: Current meter from 2024-06-01  
**When**: ADMIN tries replace with installation 2024-01-01  
**Then**: Error "Cannot be before current meter"

### TS-UC006-10: View Active Meter
**Given**: Meter "WM-2024-001" active  
**When**: ADMIN views unit details  
**Then**: Meter number and installation date shown

---

## Related User Stories

- **US026**: Assign water meter to housing unit
- **US027**: Replace water meter
- **US028**: View water meter history
- **US029**: Remove water meter
- **US030**: Track meter installation dates

---

## Notes

- Water meters only in Phase 1
- Other utility meters (electricity, gas, heating) in Phase 2 (BACKLOG-012, 013, 014)
- Same pattern will be used for all meter types
- Meter readings tracked separately in future (BACKLOG-015)
- Location examples: "Kitchen under sink", "Bathroom", "Utility room"
- Future: Meter reading management
- Future: Automatic consumption calculation
- Future: Anomaly detection (unusual consumption)

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation
