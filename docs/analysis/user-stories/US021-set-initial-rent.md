# User Story US021: Set Initial Rent

| Attribute | Value |
|-----------|-------|
| **Story ID** | US021 |
| **Epic** | Rent Management |
| **Related UC** | UC005 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** set the initial rent for a housing unit **so that** I can establish the baseline rental price.

## Acceptance Criteria

**AC1:** Unit has no rent → Rent section shows "No rent recorded" + "Set Rent" button.
**AC2:** Enter Monthly Rent €850.00 + Effective From 2024-01-01 → save → shown as "€850.00/month".
**AC3:** Add optional notes "Initial market rate" → notes stored and visible in history.
**AC4:** Enter amount ≤ 0 → error "Rent must be positive".
**AC5:** Leave effective date empty → error "Effective from date is required".
**AC6:** Amount displays with € symbol formatted as "€850.00/month".

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rent-history` — HTTP 201.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
