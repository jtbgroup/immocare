# User Story US045: Edit Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US045 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** edit a person's details **so that** I can keep contact and identity information up to date.

## Acceptance Criteria

**AC1:** Person details page shows all fields + sections "Owned Buildings", "Owned Units" (if any).
**AC2:** Click "Edit" → form pre-filled with all current values.
**AC3:** Change any field → save → "Person updated successfully", new values shown.
**AC4:** Set national ID already used by another person → error "This national ID is already assigned to another person".
**AC5:** Cancel → changes discarded.

**Endpoint:** `PUT /api/v1/persons/{id}`

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
