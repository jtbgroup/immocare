# User Story US041: Remove a Meter

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US041 |
| **Story Name** | Remove a Meter |
| **Epic** | Meter Management |
| **Related UC** | UC008 - Manage Meters |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 4 |

---

## User Story

**As an** ADMIN
**I want to** remove an active meter without replacing it
**So that** I can handle cases where a meter is disconnected with no replacement

---

## Acceptance Criteria

### AC1: Display Remove Meter Confirmation
**Given** an active meter (GAS-001) exists
**When** I click "Remove" on that meter
**Then** a confirmation dialog appears showing:
  - Meter type and number: "GAS meter GAS-001"
  - Warning: "This meter will be deactivated. No replacement will be created."
  - A date picker for end date (default: today)
  - "Confirm" and "Cancel" buttons

### AC2: Remove a Meter Successfully
**Given** the confirmation dialog is open for meter WTR-001 (start date 2024-01-01)
**When** I enter end date 2025-01-15
**And** I click "Confirm"
**Then** WTR-001's end_date is set to 2025-01-15
**And** the meter no longer appears as active
**And** it remains visible in the history with status "Closed"

### AC3: Meter Still Visible in History After Removal
**Given** I have removed meter WTR-001
**When** I click "View History" on the unit
**Then** WTR-001 appears in the history table with status "Closed" and its end date

### AC4: Other Active Meters Unaffected
**Given** a unit has active meters WTR-001 and GAS-001
**When** I remove WTR-001
**Then** GAS-001 remains active and unchanged

### AC5: Validation — End Date Cannot Be Before Start Date
**Given** meter WTR-001 started on 2024-06-01
**When** I enter end date 2024-01-01
**Then** I see error "End date must be on or after the meter's start date (2024-06-01)"

### AC6: Validation — End Date Cannot Be in the Future
**Given** I am removing a meter
**When** I enter a future end date
**Then** I see error "End date cannot be in the future"

### AC7: Cancel Removal
**Given** the remove confirmation dialog is open
**When** I click "Cancel"
**Then** the dialog closes
**And** the meter remains active and unchanged

---

## Technical Notes

- Endpoint: `DELETE /api/v1/housing-units/{unitId}/meters/{meterId}` or `DELETE /api/v1/buildings/{buildingId}/meters/{meterId}`
- Body: `{ endDate }`
- Operation sets `end_date` on the meter record — the record is never physically deleted
- HTTP 204 No Content on success

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
