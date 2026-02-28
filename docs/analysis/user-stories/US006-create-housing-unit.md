# User Story US006: Create Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US006 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** create a housing unit in a building **so that** I can manage individual apartments.

## Acceptance Criteria

**AC1:** From building details, click "Add Housing Unit" → form shown with building pre-selected.
**AC2:** Enter Unit Number "A101" + Floor 1 → save → unit created, redirected to unit details.
**AC3:** Check "Has Terrace" + enter surface 12.5 + orientation S → save → unit created with terrace.
**AC4:** Check "Has Terrace" but leave surface and orientation empty → save allowed (surface/orientation optional).
**AC5:** Unit number "A101" already exists in building → error "Unit number must be unique within this building".
**AC6:** Building has owner → new unit inherits that owner if none specified.

**Endpoint:** `POST /api/v1/units` — HTTP 201 on success; HTTP 409 if unit number duplicated.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
