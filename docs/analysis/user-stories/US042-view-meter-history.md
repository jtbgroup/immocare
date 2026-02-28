# User Story US042: View Meter History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US042 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the complete meter history **so that** I can track all past and current meter assignments.

## Acceptance Criteria

**AC1:** Click "View History" → inline table with all records (active + closed) sorted by startDate DESC.
**AC2:** Columns: Type, Meter Number, EAN / Installation Number, Start Date, End Date (or "—"), Duration, Status badge (Active green / Closed grey).
**AC3:** BUILDING WATER meters also show customerNumber column.
**AC4:** Duration: active = months since startDate; closed = months between start and end.
**AC5:** Type filter available (optional).
**AC6:** "View History" link not shown if unit/building has no meters at all.

**Endpoints:** `GET /api/v1/housing-units/{unitId}/meters` and `GET /api/v1/buildings/{buildingId}/meters`

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
