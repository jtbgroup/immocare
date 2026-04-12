# User Story US063: View Boiler History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US063 |
| **Epic** | Boiler Management |
| **Related UC** | UC013 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the history of boilers for a housing unit **so that** I can track all past equipment.

## Acceptance Criteria

**AC1:** "View History" toggle in boiler section reveals full history table.
**AC2:** Columns: brand, model, fuel type, installation date, removal date, status badge (Active / Removed).
**AC3:** Ordered by installation date DESC.
**AC4:** Active boiler shown with green "Active" badge; removed boilers with grey "Removed" badge + removal date.
**AC5:** No boiler ever registered → history toggle not shown.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/boilers` — returns all boilers for the unit.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development