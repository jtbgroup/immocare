# User Story US051: Activate Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US051 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** activate a draft lease **so that** it becomes the official active lease for the unit.

## Acceptance Criteria

**AC1:** "Activate" button visible on DRAFT lease.
**AC2:** Confirmation: "Activate this lease? It will become the official active lease for unit [X]."
**AC3:** Confirm → status changes to ACTIVE, badge shows ACTIVE on unit details.
**AC4:** Another ACTIVE lease already exists → error "Activation blocked: another lease is already active for this unit".

**Endpoint:** `PATCH /api/v1/leases/{id}/status` — body `{ "targetStatus": "ACTIVE" }`.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
