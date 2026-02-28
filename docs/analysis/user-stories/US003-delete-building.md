# User Story US003: Delete Building

| Attribute | Value |
|-----------|-------|
| **Story ID** | US003 |
| **Epic** | Building Management |
| **Related UC** | UC001 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** delete a building **so that** I can remove properties no longer managed.

## Acceptance Criteria

**AC1:** Delete empty building → confirmation dialog "Are you sure? This cannot be undone." → on confirm: building removed, redirected to list, success message.
**AC2:** Building has housing units → error "Cannot delete: this building has X unit(s). Delete all units first." — building NOT deleted.
**AC3:** Cancel dialog → building NOT deleted.
**AC4:** Deleted building URL → 404 "Building not found".

**Endpoint:** `DELETE /api/v1/buildings/{id}` — HTTP 409 with `unitCount` if units exist.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
