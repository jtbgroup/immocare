# User Story US047: Assign Person as Owner

| Attribute | Value |
|-----------|-------|
| **Story ID** | US047 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** assign a person as owner of a building or housing unit **so that** ownership is tracked with a real person record.

## Acceptance Criteria

**AC1:** Building/unit edit form has "Owner" field with person picker.
**AC2:** Type 2+ chars in picker → suggestions shown (max 10, <300ms response).
**AC3:** Select a person → their name shown in owner field.
**AC4:** Save → building/unit linked to person via `ownerId` FK.
**AC5:** Person details show "Owned Buildings" and "Owned Units" sections reflecting all linked entities.
**AC6:** Clear owner → `ownerId` = NULL, "Owner: Not specified" shown.

**Implemented via:** `PUT /api/v1/buildings/{id}` and `PUT /api/v1/units/{id}` (ownerId field).

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
