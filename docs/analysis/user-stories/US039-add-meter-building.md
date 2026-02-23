# User Story US039: Add a Meter to a Building

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US039 |
| **Story Name** | Add a Meter to a Building |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** add a meter (water, gas, or electricity) to a building
**So that** I can track which meters serve the common areas

---

## Acceptance Criteria

### AC1: Add a WATER Meter to a Building Successfully
**Given** I am viewing a building details page
**When** I click "Add Meter" in the Meters section
**And** I select type: WATER
**And** I enter:
  - Meter Number: WTR-B01
  - Installation Number: IN-B-2024-001
  - Customer Number: CLI-00123
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active
**And** the WATER block on the building details shows the new meter with its customer number

### AC2: Add a GAS Meter to a Building Successfully
**Given** I am on the add meter form for a building
**When** I select type: GAS
**And** I enter:
  - Meter Number: GAS-B01
  - EAN Code: 54100000000099
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active

### AC3: Add an ELECTRICITY Meter to a Building Successfully
**Given** I am on the add meter form for a building
**When** I select type: ELECTRICITY
**And** I enter:
  - Meter Number: ELC-B01
  - EAN Code: 54200000000099
  - Start Date: 2024-01-01
**And** I click "Save"
**Then** the meter is saved as active

### AC4: Validation — Customer Number Required for WATER on Building
**Given** I am adding a WATER meter to a building
**When** I leave Customer Number empty
**And** I click "Save"
**Then** I see error "Customer number is required for water meters on a building"

### AC5: Validation — EAN Code Required for GAS
**Given** I select type GAS on a building
**When** I leave EAN Code empty
**Then** I see error "EAN code is required for gas meters"

### AC6: Validation — EAN Code Required for ELECTRICITY
**Given** I select type ELECTRICITY on a building
**When** I leave EAN Code empty
**Then** I see error "EAN code is required for electricity meters"

### AC7: Validation — Installation Number Required for WATER
**Given** I select type WATER on a building
**When** I leave Installation Number empty
**Then** I see error "Installation number is required for water meters"

### AC8: Validation — Start Date Not in Future
**Given** I am on the add meter form
**When** I enter a future start date
**Then** I see error "Start date cannot be in the future"

### AC9: Customer Number Field Only Visible for WATER on Building
**Given** I am adding a meter to a building
**When** I select type GAS or ELECTRICITY
**Then** the Customer Number field is NOT shown
**When** I select type WATER
**Then** the Customer Number field IS shown

### AC10: Add a Second WATER Meter to the Same Building
**Given** a building already has one active WATER meter
**When** I add a second WATER meter
**Then** both are shown as active in the WATER block

---

## Technical Notes

- Endpoint: `POST /api/v1/buildings/{buildingId}/meters`
- Payload: `{ type, meterNumber, eanCode?, installationNumber?, customerNumber?, startDate }`
- `owner_type` = BUILDING
- `customer_number` required only when `type` = WATER and `owner_type` = BUILDING

---

## Dependencies

- Building must exist
- US037 (View Meters of Building) must be completed

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
