# User Story US040: Replace a Meter

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US040 |
| **Story Name** | Replace a Meter |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** replace an active meter with a new one
**So that** I can track meter changes over time without losing history

---

## Acceptance Criteria

### AC1: Display Replace Meter Form
**Given** an active meter exists
**When** I click "Replace" on that meter
**Then** the replace form is displayed
**And** the current meter's data is shown read-only (type, meter number, EAN / installation number, start date)
**And** the form contains fields for the new meter: Meter Number, conditional fields (EAN / installation number / customer number), Start Date, Reason (optional)

### AC2: Replace a GAS Meter Successfully
**Given** an active GAS meter (GAS-001, EAN 54100000000001, start 2024-01-01) exists
**When** I enter new meter:
  - Meter Number: GAS-002
  - EAN Code: 54100000000002
  - Start Date: 2025-01-01
  - Reason: END_OF_LIFE
**And** I click "Save"
**Then** GAS-001 is closed (end_date = 2025-01-01)
**And** GAS-002 is active (end_date = NULL)
**And** both appear in meter history

### AC3: Replace a WATER Meter on a Housing Unit Successfully
**Given** an active WATER meter (WTR-001, IN-001, start 2024-01-01) exists on a housing unit
**When** I enter new meter:
  - Meter Number: WTR-002
  - Installation Number: IN-002
  - Start Date: 2025-06-01
**And** I click "Save"
**Then** WTR-001 is closed (end_date = 2025-06-01)
**And** WTR-002 is active

### AC4: Replace a WATER Meter on a Building Successfully
**Given** an active WATER meter on a building exists
**When** I replace it with:
  - Meter Number: WTR-B02
  - Installation Number: IN-B-002
  - Customer Number: CLI-00456
  - Start Date: 2025-03-01
**Then** old meter is closed, new meter is active with customer number

### AC5: Reason Dropdown
**Given** I am on the replace meter form
**Then** I can select an optional reason:
  - BROKEN
  - END_OF_LIFE
  - UPGRADE
  - CALIBRATION_ISSUE
  - OTHER

### AC6: Validation — New Start Date Cannot Be Before Current Start Date
**Given** current meter started on 2024-01-01
**When** I enter new start date 2023-12-01
**Then** I see error "Start date must be on or after the current meter's start date (2024-01-01)"

### AC7: Validation — New Start Date Cannot Be in the Future
**Given** I am on the replace meter form
**When** I enter a future start date
**Then** I see error "Start date cannot be in the future"

### AC8: Validation — EAN Code Required for GAS/ELECTRICITY Replacement
**Given** I am replacing a GAS meter
**When** I leave EAN Code empty for the new meter
**Then** I see error "EAN code is required for gas meters"

### AC9: Validation — Installation Number Required for WATER Replacement
**Given** I am replacing a WATER meter
**When** I leave Installation Number empty
**Then** I see error "Installation number is required for water meters"

### AC10: Validation — Customer Number Required for WATER on Building
**Given** I am replacing a WATER meter on a building
**When** I leave Customer Number empty
**Then** I see error "Customer number is required for water meters on a building"

### AC11: Atomicity — Both Records Saved or Neither
**Given** replace form is valid
**When** the save operation fails mid-way
**Then** neither the closure of the old meter nor the creation of the new one is persisted

### AC12: Cancel Replace
**Given** I am on the replace meter form
**When** I click "Cancel"
**Then** the form closes
**And** the existing meter is unchanged

---

## Technical Notes

- Endpoint: `PUT /api/v1/housing-units/{unitId}/meters/{meterId}/replace` or `PUT /api/v1/buildings/{buildingId}/meters/{meterId}/replace`
- Operation is atomic (single transaction): sets `end_date` on current + inserts new record
- The `type` of the replacement meter is the same as the original (cannot change type via replace)
- Payload: `{ newMeterNumber, newEanCode?, newInstallationNumber?, newCustomerNumber?, newStartDate, reason? }`

---

## Dependencies

- US038 or US039 (Add Meter) must be completed
- Active meter must exist

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] All acceptance criteria met
- [ ] Manual testing completed

---

**Last Updated**: 2025-02-23
**Status**: Ready for Development
