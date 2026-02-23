# User Story US042: View Meter History

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US042 |
| **Story Name** | View Meter History |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** view the complete meter history of a housing unit or building
**So that** I can see all past and current meter assignments for any type

---

## Acceptance Criteria

### AC1: View Full History for a Housing Unit
**Given** a housing unit has the following meter history:
  - WTR-001 (WATER, 2023-01-01 → 2024-06-01, Closed)
  - WTR-002 (WATER, 2024-06-01 → present, Active)
  - GAS-001 (GAS, 2023-01-01 → present, Active)

**When** I click "View History" on the unit's Meters section
**Then** a history table shows all 3 records
**And** sorted by start_date DESC (newest first)

### AC2: History Table Columns
**Given** I am viewing meter history
**Then** the table shows the following columns:
  - Type (WATER / GAS / ELECTRICITY)
  - Meter Number
  - EAN Code or Installation Number (depending on type)
  - Start Date
  - End Date (or "—" if active)
  - Duration
  - Status badge

### AC3: Status Badges
**Given** I am viewing meter history
**Then** active meters show a green "Active" badge
**And** closed meters show a gray "Closed" badge

### AC4: Duration Calculation
**Given** meter WTR-001 ran from 2024-01-01 to 2024-06-01 (5 months)
**When** viewing history
**Then** the Duration column shows "5 months"

**Given** meter GAS-001 started 2024-01-01 and is still active today (2025-02-23)
**When** viewing history
**Then** the Duration column shows "13 months"

### AC5: Customer Number Visible for WATER on Building
**Given** I am viewing meter history for a building
**And** a WATER meter record has customer number "CLI-00123"
**When** viewing history
**Then** the customer number is shown in the table row

### AC6: Filter by Type (Optional)
**Given** I am viewing meter history with records of all 3 types
**When** I filter by type "WATER"
**Then** only WATER meter records are shown

### AC7: Close History View
**Given** the history table is open
**When** I click "Close" or "Hide History"
**Then** the history table collapses
**And** the active meters view is restored

### AC8: Empty History
**Given** a housing unit or building has no meters of any type
**When** I try to access history
**Then** the "View History" link is not shown

---

## Technical Notes

- Endpoint: `GET /api/v1/housing-units/{unitId}/meters` or `GET /api/v1/buildings/{buildingId}/meters`
- Returns all records (active and closed), sorted by start_date DESC
- Duration for active meters = months between start_date and today
- Duration for closed meters = months between start_date and end_date
- The history table is rendered inline in the Meters section (no separate page)

---

## Dependencies

- US038 or US039 (Add Meter) must be completed for test data
- US040 (Replace Meter) and US041 (Remove Meter) required for meaningful history

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] All acceptance criteria met
- [ ] Manual testing completed

---

**Last Updated**: 2025-02-23
**Status**: Ready for Development
