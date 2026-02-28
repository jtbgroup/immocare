# User Story US004: View Buildings List

| Attribute | Value |
|-----------|-------|
| **Story ID** | US004 |
| **Epic** | Building Management |
| **Related UC** | UC001 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** view all buildings **so that** I can see my property portfolio.

## Acceptance Criteria

**AC1:** 5 buildings exist → list shows all 5 with: name, address, owner (or "Not specified"), unit count. Title "Buildings", "Create Building" button visible.
**AC2:** No buildings → "No buildings yet" + "Create Building" button.
**AC3:** Click "Name" header → sort A→Z; click again → Z→A.
**AC4:** Click "City" header → sort by city.
**AC5:** Select city from filter dropdown → only that city's buildings shown.
**AC6:** Click a row → navigate to building details page.
**AC7:** >20 buildings → pagination controls shown, 20 per page.
**AC8:** Building with 5 units → shows "5 units".

**Endpoint:** `GET /api/v1/buildings?city=&search=&page=&size=&sort=` — default sort: name ASC.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
