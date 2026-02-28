# User Story US010: Add Terrace to Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US010 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** add terrace information to a housing unit **so that** I can track outdoor spaces.

## Acceptance Criteria

**AC1:** Unit has no terrace → edit → check "Has Terrace" + surface 15.5m² + orientation S → save → terrace shown in unit details.
**AC2:** Orientation dropdown options: N, S, E, W, NE, NW, SE, SW.
**AC3:** Check "Has Terrace" but leave surface and orientation empty → save allowed (both optional).
**AC4:** Unit has terrace → edit → uncheck "Has Terrace" → save → hasTerrace=false, surface and orientation cleared from record.

**Endpoint:** `PUT /api/v1/units/{id}` (part of unit edit)

**Last Updated:** 2026-02-21 | **Status:** Ready for Development
