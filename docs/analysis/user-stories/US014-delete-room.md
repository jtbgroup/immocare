# User Story US014: Delete Room

| Attribute | Value |
|-----------|-------|
| **Story ID** | US014 |
| **Epic** | Room Management |
| **Related UC** | UC003 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** delete a room **so that** I can correct errors or remove obsolete room records.

## Acceptance Criteria

**AC1:** Click delete icon → confirmation dialog: "Delete this room? Type: Bedroom, Surface: 15.5m². This cannot be undone." → confirm → room deleted, list updated (n-1 rooms).
**AC2:** Total surface recalculated after deletion.
**AC3:** Cancel dialog → room NOT deleted.

**Endpoint:** `DELETE /api/v1/housing-units/{unitId}/rooms/{id}` — HTTP 200 `{ "message": "Room deleted successfully" }`.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
