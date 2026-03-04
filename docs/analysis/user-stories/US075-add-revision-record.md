# User Story US075: Add Revision Record

| Attribute | Value |
|-----------|-------|
| **Story ID** | US075 |
| **Epic** | Fire Safety Management |
| **Related UC** | UC013 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** record a revision inspection for a fire extinguisher **so that** I can track its maintenance history.

## Acceptance Criteria

**AC1:** Revision history panel shows an "Add revision" inline form: Revision Date* (date picker, max today), Notes (optional textarea).
**AC2:** Valid date → save → new revision appears at the top of the history table; revision count badge on the card updates.
**AC3:** Revision date empty → error "Revision date is required".
**AC4:** Revision date in the future → error "Revision date cannot be in the future".
**AC5:** Cancel → form closes, no revision created.
**AC6:** Latest revision date on the extinguisher card updates to the newly added date if it is more recent than the previous one.

**Endpoint:** `POST /api/v1/fire-extinguishers/{id}/revisions` — HTTP 201. Returns full `FireExtinguisherDTO` with updated revisions list.

**Last Updated:** 2026-03-04 | **Status:** Ready for Development
