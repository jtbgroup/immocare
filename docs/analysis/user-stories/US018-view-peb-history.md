# User Story US018: View PEB Score History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US018 |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view PEB score history **so that** I can see the energy performance evolution of a unit.

## Acceptance Criteria

**AC1:** Unit has 3 PEB records → click "View History" → table shows all 3 sorted by date DESC.
**AC2:** Table columns: Score, Score Date, Certificate Number, Valid Until, Status (CURRENT / HISTORICAL / EXPIRED).
**AC3:** Most recent record marked CURRENT; older records HISTORICAL; records with valid_until < today marked EXPIRED.
**AC4:** No history → "No PEB score recorded yet" with "Add PEB Score" button.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/peb-scores`

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
