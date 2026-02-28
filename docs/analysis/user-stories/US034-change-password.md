# User Story US034: Change User Password

| Attribute | Value |
|-----------|-------|
| **Story ID** | US034 |
| **Epic** | User Management |
| **Related UC** | UC007 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** change a user's password **so that** I can reset access when needed.

## Acceptance Criteria

**AC1:** Click "Change Password" on user details → form with New Password + Confirm New Password only (no current password field).
**AC2:** Enter matching valid passwords → save → "Password changed successfully", returned to user details.
**AC3:** Target user's session invalidated → they must log in again with new password.
**AC4:** Passwords don't match → error "Passwords do not match".
**AC5:** Weak password → error "Password must be at least 8 characters and include uppercase, lowercase, and a digit".
**AC6:** Cancel → returned to user details, password unchanged.

**Endpoint:** `PATCH /api/v1/users/{id}/password` — plain text never stored or returned.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
