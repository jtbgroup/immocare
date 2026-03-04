# User Story US072: Edit Fire Extinguisher

| Attribute | Value |
|-----------|-------|
| **Story ID** | US072 |
| **Epic** | Fire Safety Management |
| **Related UC** | UC013 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** edit a fire extinguisher **so that** I can correct or update its information.

## Acceptance Criteria

**AC1:** Each extinguisher card shows an "Edit" button.
**AC2:** Click "Edit" → inline form pre-filled with: identification number, unit, notes.
**AC3:** Modify identification number to one already used by another extinguisher in the building → error "An extinguisher with this identification number already exists in this building".
**AC4:** Modify unit assignment → extinguisher now shows the new unit (or "—" if cleared).
**AC5:** Valid changes → save → card updates immediately with success feedback.
**AC6:** Cancel → form closes, no changes applied.

**Endpoint:** `PUT /api/v1/fire-extinguishers/{id}` — HTTP 200.

**Last Updated:** 2026-03-04 | **Status:** Ready for Development
