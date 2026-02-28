# User Story US049: View Lease for Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US049 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** see the current lease directly on the unit details page **so that** I can quickly check occupancy.

## Acceptance Criteria

**AC1:** No active/draft lease → "No active lease" + "Create Lease" button.
**AC2:** Active lease → status badge ACTIVE (green), tenant name(s), monthly rent + charges, start/end dates, "View" + "Edit" buttons.
**AC3:** Draft lease → same info with status badge DRAFT (grey).
**AC4:** End notice deadline within 30 days → orange banner "⚠ Lease ending soon — notice deadline: [date]".
**AC5:** Indexation anniversary within `indexationNoticeDays` days and no indexation recorded this year → orange banner "⚠ Indexation due — anniversary: [date]".
**AC6:** Click "View all leases" → all leases (all statuses) sorted by startDate DESC.

**Endpoints:** `GET /api/v1/housing-units/{unitId}/leases` / `GET /api/v1/leases/{id}`

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
