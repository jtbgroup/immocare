# User Story US053: Finish Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US053 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** mark a lease as finished **so that** I can record that the tenant has vacated.

## Acceptance Criteria

**AC1:** "Finish Lease" button shown on ACTIVE lease.
**AC2:** Dialog asks for: Effective end date (required, default today) + Notes (optional).
**AC3:** Confirm → status = FINISHED, unit shows "No active lease" + "Create Lease" button.
**AC4:** Finished lease preserved in lease history for the unit.

**Endpoint:** `PATCH /api/v1/leases/{id}/status` — body `{ "targetStatus": "FINISHED", "effectiveDate": "...", "notes": "..." }`.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
