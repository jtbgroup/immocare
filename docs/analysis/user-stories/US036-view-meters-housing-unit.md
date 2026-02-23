# User Story US036: View Meters of a Housing Unit

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US036 |
| **Story Name** | View Meters of a Housing Unit |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** view the active meters associated with a housing unit
**So that** I can quickly see which water, gas, and electricity meters are assigned

---

## Acceptance Criteria

### AC1: Display Active Meters Grouped by Type
**Given** a housing unit has:
- 1 active WATER meter (WTR-001, installation number IN-001)
- 1 active GAS meter (GAS-001, EAN 54100000000001)
- 1 active ELECTRICITY meter (ELC-001, EAN 54200000000001)

**When** I view the unit details page
**Then** the Meters section shows 3 grouped blocks (WATER / GAS / ELECTRICITY)
**And** each block displays: meter number, EAN or installation number, start date, duration in months

### AC2: Multiple Active Meters of the Same Type
**Given** a housing unit has 2 active ELECTRICITY meters (ELC-001 and ELC-002)
**When** I view the unit details
**Then** both electricity meters are shown in the ELECTRICITY block

### AC3: No Meter Assigned for a Type
**Given** a housing unit has no GAS meter
**When** I view the unit details
**Then** the GAS block shows "No gas meter assigned"
**And** an "Add Meter" button is visible for that type

### AC4: Empty State — No Meters at All
**Given** a housing unit has no meters of any type
**When** I view the unit details
**Then** all three blocks show "No meter assigned"
**And** an "Add Meter" button is visible in each block

### AC5: View History Link
**Given** a unit has at least one meter (active or closed)
**When** I view the unit details
**Then** a "View History" link is visible in the Meters section

---

## Technical Notes

- Endpoint: `GET /api/v1/housing-units/{unitId}/meters?status=active`
- Duration = months between `start_date` and today (for active meters)
- Section integrates into `HousingUnitDetailsComponent`

---

## Dependencies

- Housing unit must exist
- US038 (Add Meter to Housing Unit) required for test data

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] All acceptance criteria met
- [ ] Manual testing completed

---

**Last Updated**: 2025-02-23
**Status**: Ready for Development
