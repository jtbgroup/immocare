# User Story US022: Edit a Rent Record

| Attribute | Value |
|-----------|-------|
| **Story ID** | US022 |
| **Epic** | Rent Management |
| **Related UC** | UC005 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** update the rent of a housing unit **so that** I can record rent changes over time.

## Acceptance Criteria

**AC1:** Click "Update Rent" → form with new monthly rent, new effective date, optional notes.
**AC2:** Enter €900 from 2024-07-01 → save → previous record closes (effectiveTo = 2024-06-30), new current record created.
**AC3:** Live change preview shows "New rent: €900.00/month" before saving.
**AC4:** Enter same amount as current rent → warning "The new rent is identical to the current rent" (allowed, not blocked).
**AC5:** Effective date more than 1 year in future → error "Date cannot be more than 1 year in the future".

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rent-history` — timeline auto-recalculated.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
