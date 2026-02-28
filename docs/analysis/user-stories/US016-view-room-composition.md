# User Story US016: View Room Composition

| Attribute | Value |
|-----------|-------|
| **Story ID** | US016 |
| **Epic** | Room Management |
| **Related UC** | UC003 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the room composition of a housing unit **so that** I can see the layout and total surface.

## Acceptance Criteria

**AC1:** Rooms section on unit details shows all rooms with type and surface.
**AC2:** Total surface shown below list (sum of all room surfaces).
**AC3:** No rooms → "No rooms defined yet" + "Add Room" button.
**AC4:** Rooms grouped by type (optional enhancement).

**Endpoint:** `GET /api/v1/housing-units/{unitId}/rooms` — returns list + `totalSurface`.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
