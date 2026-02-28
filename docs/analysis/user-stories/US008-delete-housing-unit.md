# User Story US008: Delete Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US008 |
| **Epic** | Housing Unit Management |
| **Related UC** | UC002 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** delete a housing unit **so that** I can remove units that no longer exist.

## Acceptance Criteria

**AC1:** Unit has no rooms → confirmation dialog → confirm → unit deleted, redirected to building details.
**AC2:** Unit has 3 rooms → error "Cannot delete: this unit has 3 room(s). Delete all rooms first."
**AC3:** Cancel confirmation → unit NOT deleted.
**AC4:** After deletion, building unit count decremented.

**Endpoint:** `DELETE /api/v1/units/{id}` — HTTP 409 with `roomCount` if rooms exist.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
