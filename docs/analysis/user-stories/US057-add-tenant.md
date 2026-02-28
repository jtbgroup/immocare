# User Story US057: Add Tenant to Lease

| Attribute | Value |
|-----------|-------|
| **Story ID** | US057 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a tenant to a lease **so that** all occupants are formally registered.

## Acceptance Criteria

**AC1:** Tenants section shows all current tenants with role and contact info.
**AC2:** Click "Add Tenant" → person picker opens with role selector (PRIMARY / CO_TENANT / GUARANTOR).
**AC3:** Multiple tenants of different roles allowed.
**AC4:** Person already a tenant on this lease → error "This person is already a tenant on this lease".
**AC5:** "Create new person" shortcut available in picker.

**Endpoint:** `POST /api/v1/leases/{id}/tenants` — body `{ "personId": X, "role": "CO_TENANT" }`.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
