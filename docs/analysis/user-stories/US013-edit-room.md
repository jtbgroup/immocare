# User Story US013: Edit Room

| Attribute | Value |
|-----------|-------|
| **Story ID** | US013 |
| **Epic** | Room Management |
| **Related UC** | UC003 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** edit a room **so that** I can correct its type or surface.

## Acceptance Criteria

**AC1:** Click edit icon on a room → form pre-filled with current type and surface.
**AC2:** Change type from "Office" to "Bedroom" → save → room updated, success message.
**AC3:** Change surface → save → total surface of unit recalculated.
**AC4:** Click "Cancel" → changes discarded, room unchanged.

**Endpoint:** `PUT /api/v1/housing-units/{unitId}/rooms/{id}` — HTTP 200.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
