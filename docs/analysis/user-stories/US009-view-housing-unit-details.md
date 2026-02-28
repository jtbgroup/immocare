# User Story US009: View Housing Unit Details

| Attribute | Value |
|-----------|-------|
| **Story ID** | US009 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** view complete housing unit details **so that** I can see all information about a specific apartment.

## Acceptance Criteria

**AC1:** Unit details page shows: number, floor, landing, total surface, terrace info (if applicable), garden info (if applicable), owner name, building link.
**AC2:** Rooms section shows all rooms with type + surface + total calculated surface + "Add Room" button.
**AC3:** PEB section shows current score badge + date + "View History" link + "Add PEB Score" button.
**AC4:** Rent section shows current rent + effective date + "Update Rent" + "View History".
**AC5:** Meters section shows active meters grouped by type (WATER/GAS/ELECTRICITY).
**AC6:** Click building name link → navigate to building details.

**Endpoint:** `GET /api/v1/units/{id}`

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
