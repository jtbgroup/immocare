# User Story US035: Delete User

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US035 |
| **Story Name** | Delete User |
| **Epic** | User Management |
| **Related UC** | UC007 - Manage Users |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** delete a user account
**So that** I can revoke system access when a person leaves

---

## Acceptance Criteria

### AC1: Delete User Successfully
**Given** a user exists who is neither my own account nor the last remaining ADMIN
**When** I click "Delete" on their details page
**And** a confirmation dialog appears showing the username
**And** I click "Delete" to confirm
**Then** the user account is permanently deleted
**And** I am redirected to the users list
**And** I see success message "User deleted successfully"
**And** the deleted user no longer appears in the list

### AC2: Confirmation Dialog
**Given** I click "Delete" on a user's details page
**Then** a confirmation dialog appears with:
- The username shown in bold
- Message: "This action cannot be undone"
- "Cancel" button
- "Delete" button (styled as destructive action)

### AC3: Cancel Deletion
**Given** the confirmation dialog is displayed
**When** I click "Cancel"
**Then** the dialog closes
**And** the user account is not deleted
**And** I remain on the user details page

### AC4: Prevent Self-Deletion
**Given** I am logged in as "admin_a"
**When** I view my own details page
**Then** the "Delete" button is disabled
**And** a tooltip explains: "You cannot delete your own account"

### AC5: Prevent Last Admin Deletion
**Given** only one user account exists in the system
**When** I try to delete it
**Then** the "Delete" button is disabled
**And** a tooltip explains: "Cannot delete the last administrator account"

### AC6: Deleted User Cannot Log In
**Given** a user account has been deleted
**When** that user attempts to log in with their credentials
**Then** login is rejected with "Invalid username or password"

### AC7: Session Invalidated After Deletion
**Given** the target user has an active session at the time of deletion
**When** the ADMIN deletes their account
**Then** the target user's session is invalidated immediately

---

## Technical Notes

- Endpoint: `DELETE /api/v1/users/{id}`
- Guard checks performed server-side (not only UI-level)
- Deletion is hard delete (no soft delete in Phase 1)
- Audit records referencing this user (created_by on other entities) are kept with SET NULL

---

## Dependencies

- US032 (Create User) must be completed
- User must be authenticated as ADMIN

---

## Edge Cases

### Edge Case 1: Concurrent Deletion
**Given** two ADMINs view the same user
**When** ADMIN A deletes the user
**And** ADMIN B tries to delete them seconds later
**Then** ADMIN B sees "User not found" error

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] All acceptance criteria met
- [ ] Edge cases handled
- [ ] Manual testing completed

---

**Last Updated**: 2024-01-15
**Status**: Ready for Development
