# User Story US068: Add Boiler Service Validity Rule

| Attribute | Value |
|-----------|-------|
| **Story ID** | US068 |
| **Epic** | Platform Administration |
| **Related UC** | UC013 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a new boiler service validity rule **so that** I can reflect regulation changes over time.

## Acceptance Criteria

**AC1:** "Add Rule" button in the Boiler Service Validity Rules section.

**AC2:** Form: valid from date* (required), duration in months* (required, > 0), description (optional).

**AC3:** Save → new rule appears in history table; if valid_from ≤ today and is the most recent, it becomes the current rule (highlighted).

**AC4:** valid_from already used by another rule → error "A validity rule already exists for this date".

**AC5:** Duration ≤ 0 → error "Validity duration must be greater than zero".

**AC6:** Existing boiler service records are NOT retroactively modified.

**AC7:** Created by and created at shown in history table.

**Endpoint:** `POST /api/v1/config/boiler-validity-rules` — HTTP 201.

**Last Updated:** 2026-04-04 | **Status:** Ready for Development
