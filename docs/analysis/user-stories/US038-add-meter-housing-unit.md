# User Story US038: Add a Meter to a Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US038 |
| **Story Name** | Add a Meter to a Housing Unit |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** add a meter (water, gas, or electricity) to a housing unit
**So that** I can track which meters serve the apartment

---

## Acceptance Criteria

### AC1: Display Add Meter Form
**Given** I am viewing a housing unit details page
**When** I click "Add Meter" in the Meters section (or in a specific type block)
**Then** the add meter form is displayed
**And** the form contains: Type, Meter Number, conditional fields, Start Date

### AC2: Add a GAS Meter Successfully
**Given** I am on the add meter form
**When** I select type: GAS
**And** I enter:
  - Meter Number: GAS-001
  - EAN Code: 54100000000001
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active (end_date = NULL)
**And** the GAS block on the unit details shows the new meter

### AC3: Add an ELECTRICITY Meter Successfully
**Given** I am on the add meter form
**When** I select type: ELECTRICITY
**And** I enter:
  - Meter Number: ELC-001
  - EAN Code: 54200000000001
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active
**And** the ELECTRICITY block on the unit details shows the new meter

### AC4: Add a WATER Meter Successfully
**Given** I am on the add meter form
**When** I select type: WATER
**And** I enter:
  - Meter Number: WTR-001
  - Installation Number: IN-2024-001
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active
**And** the WATER block on the unit details shows the new meter
**And** no customer number field is shown (housing unit context)

### AC5: Add a Second Meter of the Same Type
**Given** the unit already has one active ELECTRICITY meter (ELC-001)
**When** I add another ELECTRICITY meter (ELC-002)
**Then** both ELC-001 and ELC-002 are shown as active in the ELECTRICITY block

### AC6: Validation — EAN Code Required for GAS
**Given** I select type GAS
**When** I leave EAN Code empty
**And** I click "Save"
**Then** I see error "EAN code is required for gas meters"

### AC7: Validation — EAN Code Required for ELECTRICITY
**Given** I select type ELECTRICITY
**When** I leave EAN Code empty
**And** I click "Save"
**Then** I see error "EAN code is required for electricity meters"

### AC8: Validation — Installation Number Required for WATER
**Given** I select type WATER
**When** I leave Installation Number empty
**And** I click "Save"
**Then** I see error "Installation number is required for water meters"

### AC9: Validation — Start Date Required
**Given** I am on the add meter form
**When** I leave Start Date empty
**And** I click "Save"
**Then** I see error "Start date is required"

### AC10: Validation — Start Date Not in Future
**Given** I am on the add meter form
**When** I enter a future start date
**And** I click "Save"
**Then** I see error "Start date cannot be in the future"

### AC11: Conditional Fields Based on Type
**Given** I am on the add meter form
**When** I select type GAS or ELECTRICITY
**Then** I see the EAN Code field
**And** the Installation Number field is hidden
**When** I select type WATER
**Then** I see the Installation Number field
**And** the EAN Code field is hidden

### AC12: Cancel Add Meter
**Given** I am on the add meter form
**When** I click "Cancel"
**Then** the form closes
**And** no meter is added

---

## Technical Notes

- Endpoint: `POST /api/v1/housing-units/{unitId}/meters`
- Payload: `{ type, meterNumber, eanCode?, installationNumber?, startDate }`
- `owner_type` = HOUSING_UNIT, `customer_number` not applicable
- EAN code format: 18-digit numeric string (Belgian standard)
- Meter number format: alphanumeric with hyphens/underscores, max 50 chars

---

## Dependencies

- Housing unit must exist
- US036 (View Meters) must be completed

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
