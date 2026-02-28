# User Story US023: View Rent History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US023 |
| **Epic** | Rent Management |
| **Related UC** | UC005 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the complete rent history **so that** I can see all past rent amounts and changes.

## Acceptance Criteria

**AC1:** Unit has current rent €900 from 2024-07-01 → Rent section shows "€900.00/month", "Effective from: 2024-07-01", "View History" link.
**AC2:** Click "View History" → table with 2 entries (€800 2024-01-01→2024-06-30, €900 2024-07-01→current) sorted newest first.
**AC3:** Table columns: Monthly Rent (€), Effective From, Effective To (or "Current"), Duration, Notes.
**AC4:** Duration 2024-01-01 to 2024-06-30 → shows "6 months".
**AC5:** Rent changed €800→€900 → unit details shows "Last change: +€100 on 2024-07-01".

**Endpoint:** `GET /api/v1/housing-units/{unitId}/rent-history`

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
