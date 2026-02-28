# User Story US046: Delete Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US046 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** delete a person **so that** I can remove records that were created by mistake.

## Acceptance Criteria

**AC1:** Person has no associated buildings or units → confirmation dialog → confirm → person deleted.
**AC2:** Person is owner of buildings/units → error "Cannot delete: this person is owner of [X building(s), Y unit(s)]. Remove ownership first."
**AC3:** Cancel → person NOT deleted.

**Endpoint:** `DELETE /api/v1/persons/{id}` — HTTP 409 if person is owner.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
