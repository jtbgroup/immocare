# User Story US035: Delete User

| Attribute | Value |
|-----------|-------|
| **Story ID** | US035 |
| **Epic** | User Management |
| **Related UC** | UC007 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** delete a user account **so that** I can revoke system access.

## Acceptance Criteria

**AC1:** Click "Delete" → confirmation dialog: "Delete [username]? This cannot be undone." with Cancel + Delete buttons.
**AC2:** Confirm → user deleted, redirected to user list, "User deleted successfully".
**AC3:** Cancel → dialog closes, user NOT deleted.
**AC4:** Cannot delete own account → "Delete" button disabled with tooltip "You cannot delete your own account".
**AC5:** Cannot delete last admin account → button disabled with tooltip "Cannot delete the last administrator account".
**AC6:** Deleted user cannot log in (credentials rejected).
**AC7:** Deleted user's active session invalidated.

**Endpoint:** `DELETE /api/v1/users/{id}` — HTTP 409 if self-deletion or last admin.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
