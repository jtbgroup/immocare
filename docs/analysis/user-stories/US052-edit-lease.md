# User Story US052: Edit Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US052 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** edit a lease **so that** I can correct data or add information obtained after signing.

## Acceptance Criteria

**AC1:** Click "Edit" on DRAFT or ACTIVE lease → form pre-filled with all current values.
**AC2:** Change any field → save → new values shown.
**AC3:** Change start date or duration → end date recalculates automatically.
**AC4:** FINISHED or CANCELLED lease → no "Edit" button, all fields read-only.
**AC5:** Validation same as creation (monthly rent required and positive, etc.).

**Endpoint:** `PUT /api/v1/leases/{id}` — HTTP 422 if FINISHED or CANCELLED.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
