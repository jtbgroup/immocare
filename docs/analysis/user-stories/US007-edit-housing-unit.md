# User Story US007: Edit Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US007 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** edit a housing unit **so that** I can update details when unit characteristics change.

## Acceptance Criteria

**AC1:** Click "Edit" on unit details → form pre-filled with all current values.
**AC2:** Change total surface → save → new surface shown.
**AC3:** Unit has no terrace → check "Has Terrace" + enter 10.5m² E → save → terrace added.
**AC4:** Unit has terrace → uncheck "Has Terrace" → save → hasTerrace=false, surface+orientation cleared.
**AC5:** Change unit number to one already used in the building → error "Unit number must be unique".
**AC6:** Set owner that differs from building owner → unit overrides with new owner.

**Endpoint:** `PUT /api/v1/units/{id}` — HTTP 404 if not found.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
