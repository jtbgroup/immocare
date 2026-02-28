# User Story US037: View Meters of a Building

| Attribute | Value |
|-----------|-------|
| **Story ID** | US037 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view active meters of a building **so that** I can see which common-area meters are assigned.

## Acceptance Criteria

**AC1:** Same 3-block layout as US036. WATER meters on buildings also display `customerNumber`.
**AC2:** Multiple active meters of same type → all shown.
**AC3:** No meter for a type → "No [type] meter assigned" + "Add Meter" button.
**AC4:** No meters at all → all 3 blocks show empty state.
**AC5:** At least one meter → "View History" link visible.

**Endpoint:** `GET /api/v1/buildings/{buildingId}/meters?status=active`

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
