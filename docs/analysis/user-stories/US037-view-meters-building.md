# User Story US037: View Meters of a Building

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US037 |
| **Story Name** | View Meters of a Building |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** view the active meters associated with a building
**So that** I can see which common-area water, gas, and electricity meters are assigned

---

## Acceptance Criteria

### AC1: Display Active Meters Grouped by Type
**Given** a building has:
- 2 active WATER meters (WTR-B01, WTR-B02, each with installation number and customer number)
- 1 active GAS meter (GAS-B01, EAN 54100000000099)
- 1 active ELECTRICITY meter (ELC-B01, EAN 54200000000099)

**When** I view the building details page
**Then** the Meters section shows 3 grouped blocks (WATER / GAS / ELECTRICITY)
**And** each WATER meter block also displays the customer number

### AC2: Multiple Active Meters of the Same Type
**Given** a building has 2 active WATER meters
**When** I view the building details
**Then** both water meters are shown in the WATER block

### AC3: Customer Number Displayed for Water Meters
**Given** a building has an active WATER meter with customer number "CLI-00123"
**When** I view the building details
**Then** the WATER block shows "Customer number: CLI-00123"

### AC4: No Meter Assigned for a Type
**Given** a building has no ELECTRICITY meter
**When** I view the building details
**Then** the ELECTRICITY block shows "No electricity meter assigned"
**And** an "Add Meter" button is visible for that type

### AC5: Empty State — No Meters at All
**Given** a building has no meters of any type
**When** I view the building details
**Then** all three blocks show "No meter assigned"
**And** an "Add Meter" button is visible in each block

### AC6: View History Link
**Given** a building has at least one meter (active or closed)
**When** I view the building details
**Then** a "View History" link is visible in the Meters section

---

## Technical Notes

- Endpoint: `GET /api/v1/buildings/{buildingId}/meters?status=active`
- Section integrates into `BuildingDetailsComponent`
- customer_number is only displayed for WATER meters on BUILDING owner_type

---

## Dependencies

- Building must exist
- US039 (Add Meter to Building) required for test data

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] All acceptance criteria met
- [ ] Manual testing completed

---

**Last Updated**: 2025-02-23
**Status**: Ready for Development
