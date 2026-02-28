# User Story US011: Add Garden to Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US011 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** add garden information to a housing unit **so that** I can track private outdoor garden spaces.

## Acceptance Criteria

**AC1:** Unit has no garden → edit → check "Has Garden" + surface 25.0m² + orientation W → save → garden shown in unit details.
**AC2:** Orientation dropdown: N, S, E, W, NE, NW, SE, SW.
**AC3:** Unit has both terrace and garden → both displayed in unit details simultaneously.
**AC4:** Check "Has Garden" but leave surface empty → save allowed (surface optional).

**Endpoint:** `PUT /api/v1/units/{id}` (part of unit edit)

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
