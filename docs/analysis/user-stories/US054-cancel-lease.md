# User Story US054: Cancel Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US054 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** cancel a lease **so that** I can record an early termination or unused draft.

## Acceptance Criteria

**AC1:** "Cancel Lease" button (red/warning) shown on DRAFT and ACTIVE leases.
**AC2:** Dialog: "Are you sure? This cannot be undone." + Cancellation date + optional Reason/notes.
**AC3:** Confirm → status = CANCELLED, unit available for new lease.
**AC4:** Cancelled lease preserved in history with CANCELLED badge (red).

**Endpoint:** `PATCH /api/v1/leases/{id}/status` — body `{ "targetStatus": "CANCELLED", "effectiveDate": "...", "notes": "..." }`.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
