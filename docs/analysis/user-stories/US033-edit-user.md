# User Story US033: Edit User

| Attribute | Value |
|-----------|-------|
| **Story ID** | US033 |
| **Epic** | User Management |
| **Related UC** | UC007 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** edit a user's information **so that** I can keep user data up to date.

## Acceptance Criteria

**AC1:** Click "Edit" on user details → form pre-filled with username, email, role. Password field NOT shown.
**AC2:** Change username → save → "User updated successfully", details show new username.
**AC3:** Change email → save → updated with new email.
**AC4:** Username already used by another user → error "Username already exists".
**AC5:** Email already used by another user → error "Email already in use".
**AC6:** Clear username → error "Username is required".
**AC7:** Cancel with changes → confirmation dialog; if confirmed, no changes saved.
**AC8:** After successful edit → "Updated at" timestamp updated.

**Endpoint:** `PUT /api/v1/users/{id}` — password NOT part of this form (see US034).

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
