# User Story US017: Add PEB Score

| Attribute | Value |
|-----------|-------|
| **Story ID** | US017 |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a PEB score to a housing unit **so that** I can track its energy performance.

## Acceptance Criteria

**AC1:** Click "Add PEB Score" → form shown with score dropdown, date, optional certificate number, optional valid until.
**AC2:** Score dropdown shows color-coded options: A++ (dark green) → A+ → A → B → C → D → E → F → G (dark red).
**AC3:** Unit has no score → add score B dated 2024-01-15 → score saved, unit details show B as current.
**AC4:** Unit has score C (2023) → add score B (2024) → B becomes current, C becomes historical.
**AC5:** Add with certificate "PEB-2024-123456" + valid until 2034-01-15 → all details stored.
**AC6:** Leave date empty → error "Score date is required".
**AC7:** Enter future date → error "Score date cannot be in the future".

**Endpoint:** `POST /api/v1/housing-units/{unitId}/peb-scores` — append-only, HTTP 201.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
