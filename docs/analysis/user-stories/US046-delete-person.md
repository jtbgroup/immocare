# User Story US046: Delete Person

| Attribute | Value |
|-----------|-------|
| **Story ID** | US046 |
| **Epic** | Person Management |
| **Related UC** | UC006 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** delete a person **so that** I can remove records that were created by mistake.

## Acceptance Criteria

**AC1:** Person has no owned buildings, units, or active leases → confirmation dialog → confirm → person deleted (HTTP 204). All associated IBANs cascade-deleted.
**AC2:** Person is owner of buildings or units → error "Cannot delete: this person is owner of [X building(s), Y unit(s)]. Remove ownership first."
**AC3:** Person is active tenant on a lease → error "Cannot delete: this person is an active tenant on [N] lease(s)."
**AC4:** Cancel → person NOT deleted.

**Endpoint:** `DELETE /api/v1/persons/{id}` — HTTP 409 if referenced as owner or active tenant.

**Last Updated:** 2026-03-10 | **Status:** ✅ Implemented
