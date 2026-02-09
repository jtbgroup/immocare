# Use Case UC004: Manage PEB Scores

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC004 |
| **Use Case Name** | Manage PEB Scores |
| **Version** | 1.0 |
| **Status** | ✅ Ready for Implementation |
| **Priority** | HIGH - Foundation |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN; Housing unit exists |
| **Postconditions** | PEB score history is updated in the system |
| **Related Use Cases** | UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages Energy Performance Certificate (PEB) scores for housing units. PEB scores track energy efficiency over time with full history retention.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator
- **Goal**: Record and track PEB scores over time
- **Characteristics**: 
  - Has access to PEB certificates
  - Knows certificate validity periods

---

## Preconditions

1. User authenticated as ADMIN
2. Housing unit exists
3. System operational

---

## Basic Flow

### 1. View PEB Score History

**Trigger**: ADMIN views housing unit details

1. System displays PEB Score section showing:
   - **Current Score**: Most recent PEB score with badge (A++, A+, A, B, C, D, E, F, G)
   - **Score Date**: Date of current certificate
   - **Valid Until**: Expiration date (if specified)
   - **Certificate Number**: If available
   - Link: "View History"

2. If no score exists, show: "No PEB score recorded"

3. ADMIN can click "View History" to see all scores

4. System displays history table:
   - Score (with color coding)
   - Score Date
   - Certificate Number
   - Valid Until
   - Actions (View details)

5. History sorted by score_date DESC (newest first)

**Result**: ADMIN sees current and historical PEB scores

---

### 2. Add New PEB Score

**Trigger**: ADMIN clicks "Add PEB Score" button

1. System displays PEB score form:
   - **Housing Unit** (pre-selected, read-only)
   - **PEB Score** (required, dropdown)
   - **Score Date** (required, date picker)
   - **Certificate Number** (optional, text, max 50 chars)
   - **Valid Until** (optional, date picker)

2. PEB Score options (with color indicators):
   - A++ (dark green)
   - A+ (green)
   - A (light green)
   - B (yellow-green)
   - C (yellow)
   - D (orange)
   - E (light red)
   - F (red)
   - G (dark red)

3. ADMIN fills form

4. ADMIN clicks "Save"

5. System validates:
   - PEB score selected
   - Score date not in future
   - Valid until > score date (if provided)
   - Certificate number format (alphanumeric with hyphens)

6. System saves score to history table:
   - Append-only (no update of existing records)
   - New record becomes current score

7. System displays success message

8. System updates unit details to show new current score

**Result**: New PEB score is recorded in history

---

### 3. View PEB Score Details

**Trigger**: ADMIN clicks on a score in history table

1. System displays score details modal:
   - PEB Score (large badge)
   - Score Date
   - Certificate Number
   - Valid Until
   - Status (Current / Historical / Expired)
   - Created at timestamp

2. ADMIN can close modal or navigate back

**Result**: ADMIN sees complete score information

---

### 4. Check PEB Score Validity

**Trigger**: System checks score validity (background process or on-demand)

1. System checks if current score has valid_until date

2. If valid_until is in the past:
   - Mark as "Expired" in UI
   - Show warning icon
   - Suggest adding new score

3. If valid_until approaching (< 3 months):
   - Show warning: "PEB certificate expires soon"
   - Suggest renewal

**Result**: ADMIN is informed of score validity status

---

## Alternative Flows

### Alternative Flow 2A: Validation Errors

**Trigger**: Validation fails in step 5 of Basic Flow 2

1. System displays errors:
   - "PEB score is required"
   - "Score date is required"
   - "Score date cannot be in the future"
   - "Valid until must be after score date"
   - "Certificate number invalid format"

2. ADMIN corrects errors

3. Return to step 4 of Basic Flow 2

**Result**: Errors must be fixed

---

### Alternative Flow 2B: Correction Entry

**Trigger**: ADMIN realizes previous score was entered incorrectly

1. ADMIN adds new correct score with same or adjusted date

2. System accepts new entry (append-only model)

3. Old incorrect entry remains in history but becomes non-current

4. ADMIN can add note in certificate_number field: "Corrected entry"

**Result**: History shows both entries; most recent is current

---

### Alternative Flow 2C: Historical Score Entry

**Trigger**: ADMIN entering old certificate retroactively

1. ADMIN enters score with past date (e.g., 2 years ago)

2. System accepts entry

3. System determines current score by most recent score_date (not entry order)

4. If newly entered old score is NOT most recent, it becomes historical

**Result**: Historical scores can be added anytime

---

## Exception Flows

### Exception Flow 1: Database Error

**Trigger**: Database unavailable

1. System displays error message

2. ADMIN can retry

**Result**: Score not saved

---

## Business Rules

### BR-UC004-01: Append-Only History
PEB scores are never updated or deleted, only appended.

**Rationale**: Maintain complete audit trail of all certificates

---

### BR-UC004-02: Current Score Definition
Current score = record with most recent score_date.

**Rationale**: Date of certificate issuance determines currency, not entry date

---

### BR-UC004-03: Score Date Validation
Score date cannot be in the future.

**Rationale**: Cannot have certificate from future date

---

### BR-UC004-04: Validity Period
If valid_until is specified, it must be after score_date.

**Rationale**: Certificate cannot expire before it's issued

---

### BR-UC004-05: Multiple Scores Same Day
Multiple scores with same date are allowed (edge case: correction).

**Rationale**: Allow flexibility for corrections; system uses most recently entered

---

### BR-UC004-06: PEB Score Values
Only predefined values (A++ to G) are allowed.

**Rationale**: Standard energy performance scale

---

### BR-UC004-07: Certificate Number Optional
Certificate number is optional but recommended.

**Rationale**: Not all certificates may have numbers; or number unknown at entry time

---

## Data Elements

### Input Data
- Housing unit ID (selected)
- PEB score (enum: A++ to G)
- Score date (date)
- Certificate number (string, max 50, optional)
- Valid until (date, optional)

### Output Data
- Score ID (generated)
- All input data
- Created_at timestamp
- Current/Historical/Expired status (calculated)

---

## User Interface Requirements

### PEB Section (in Housing Unit Details)
- Current score badge (large, color-coded)
- Score date
- Valid until (with expiry warning if < 3 months)
- Certificate number
- "Add PEB Score" button
- "View History" link

### PEB History Modal/Page
- Table with all scores
- Color-coded score badges
- Sort by date
- Filter by score range
- Export to PDF option (future)

### PEB Score Form
- Score dropdown with visual color indicators
- Date pickers with calendar
- Certificate number input
- Valid until date picker
- Help text explaining each field
- "Save" and "Cancel" buttons

---

## Performance Requirements

- PEB section loads with unit details (< 500ms)
- Add score completes in < 500ms
- History loads in < 500ms

---

## Security Requirements

- Only ADMIN can manage PEB scores
- PEB data visible only to authenticated users

---

## Test Scenarios

### TS-UC004-01: Add First PEB Score
**Given**: Unit has no PEB score  
**When**: ADMIN adds score B dated today  
**Then**: Score saved, displayed as current

### TS-UC004-02: Add Second Score (Newer)
**Given**: Unit has score C from 2023  
**When**: ADMIN adds score B from 2024  
**Then**: B becomes current, C is historical

### TS-UC004-03: Add Historical Score
**Given**: Unit has score B from 2024  
**When**: ADMIN adds score D from 2022  
**Then**: B remains current, D is historical

### TS-UC004-04: Score Date in Future Error
**Given**: ADMIN adding score  
**When**: Enters date = tomorrow  
**Then**: Error "Score date cannot be future"

### TS-UC004-05: Invalid Validity Period
**Given**: ADMIN adding score dated 2024-01-01  
**When**: Sets valid until = 2023-12-31  
**Then**: Error "Valid until must be after score date"

### TS-UC004-06: View PEB History
**Given**: Unit has 3 scores: D (2020), C (2022), B (2024)  
**When**: ADMIN views history  
**Then**: All 3 shown, sorted newest first

### TS-UC004-07: Expired Certificate Warning
**Given**: Score valid until 2023-12-31, today is 2024-01-15  
**When**: ADMIN views unit  
**Then**: "Expired" warning shown

### TS-UC004-08: Expiring Soon Warning
**Given**: Score valid until 3 months from now  
**When**: ADMIN views unit  
**Then**: "Expires soon" warning shown

### TS-UC004-09: Add Score with Certificate Number
**Given**: ADMIN adding score  
**When**: Enters certificate PEB-2024-123456  
**Then**: Saved with certificate number

### TS-UC004-10: Color Coding Display
**Given**: Viewing scores A++, C, G  
**When**: ADMIN views list  
**Then**: A++ green, C yellow, G red

---

## Related User Stories

- **US017**: Add PEB score to housing unit
- **US018**: View PEB score history
- **US019**: Check PEB certificate validity
- **US020**: Track PEB score improvements

---

## Notes

- PEB = Energy Performance Certificate (European standard)
- Scores typically valid for 10 years
- Better score (A++) = more energy efficient
- History allows tracking improvements over time (e.g., after renovations)
- Future: Generate PEB report PDF
- Future: PEB score comparison across portfolio
- Future: Automatic expiry notifications

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: ✅ Ready for Implementation
