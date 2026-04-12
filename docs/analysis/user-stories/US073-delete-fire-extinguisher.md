# User Story US073: Delete Fire Extinguisher

| Attribute | Value |
|-----------|-------|
| **Story ID** | US073 |
| **Epic** | Fire Safety Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** delete a fire extinguisher **so that** I can remove equipment that is no longer in service.

## Acceptance Criteria

**AC1:** Each extinguisher card shows a "Delete" button.
**AC2:** Click "Delete" → inline confirmation prompt: "Delete extinguisher [ID]? This will also delete [N] revision record(s). This action cannot be undone."
**AC3:** Confirm → extinguisher and all its revision records are permanently deleted; card disappears from the list.
**AC4:** Cancel → no deletion, card remains unchanged.
**AC5:** Extinguisher has no revisions → confirmation reads "Delete extinguisher [ID]? This action cannot be undone." (no mention of revision count).

**Endpoint:** `DELETE /api/v1/fire-extinguishers/{id}` — HTTP 204.

**Last Updated:** 2026-03-04 | **Status:** Ready for Development
