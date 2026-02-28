# User Story US058: Remove Tenant from Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US058 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** remove a tenant from a lease **so that** I can correct errors or record departure.

## Acceptance Criteria

**AC1:** Each tenant row has "Remove" button.
**AC2:** Confirmation dialog: "Remove [Name] from this lease?"
**AC3:** Remove CO_TENANT → only PRIMARY tenant remains.
**AC4:** Remove last PRIMARY tenant → error "Cannot remove the only primary tenant. Add another primary tenant first."
**AC5:** Remove GUARANTOR → other tenants unaffected.

**Endpoint:** `DELETE /api/v1/leases/{id}/tenants/{personId}` — HTTP 200 with updated lease.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
