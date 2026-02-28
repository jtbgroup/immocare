# User Story US015: Batch Create Rooms

| Attribute | Value |
|-----------|-------|
| **Story ID** | US015 |
| **Epic** | Room Management |
| **Related UC** | UC003 |
| **Priority** | COULD HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** add multiple rooms at once **so that** I can quickly define all rooms in a unit.

## Acceptance Criteria

**AC1:** Click "Quick Add Rooms" → modal with 3 empty rows (each: type dropdown + surface input).
**AC2:** Click "Add Row" → new row added (max 20 rows).
**AC3:** Click "Remove" on a row → that row removed, others unchanged.
**AC4:** Fill 4 rows → "Save All" → all 4 rooms created, "4 rooms added successfully".
**AC5:** Some rows left empty → empty rows silently skipped on save.
**AC6:** Row with type but no surface → row highlighted in red, error shown, valid rows can still be saved.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rooms/batch` — HTTP 201.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
