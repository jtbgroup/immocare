# User Story US036: View Meters of a Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US036 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view active meters of a housing unit **so that** I can see which meters are assigned.

## Acceptance Criteria

**AC1:** Meters section shows 3 blocks (WATER / GAS / ELECTRICITY); each active meter shows: number, EAN or installation number, start date, duration in months.
**AC2:** Multiple active meters of same type → all shown in their block.
**AC3:** No meter for a type → block shows "No [type] meter assigned" + "Add Meter" button.
**AC4:** No meters at all → all 3 blocks show empty state with "Add Meter" buttons.
**AC5:** At least one meter (active or closed) → "View History" link visible.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/meters?status=active`

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
