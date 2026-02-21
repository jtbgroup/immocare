# User Story US034: Change User Password

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US034 |
| **Story Name** | Change User Password |
| **Epic** | User Management |
| **Related UC** | UC007 - Manage Users |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** change a user's password
**So that** I can reset access when a user forgets their credentials

---

## Acceptance Criteria

### AC1: Display Change Password Form
**Given** I am viewing a user's details page
**When** I click "Change Password"
**Then** a form is displayed with:
- New Password field
- Confirm New Password field
**And** no existing password is shown

### AC2: Change Password Successfully
**Given** I am on the change password form
**When** I enter:
- New Password: "NewSecure1!"
- Confirm New Password: "NewSecure1!"

**And** I click "Save"
**Then** the password is updated
**And** I see success message "Password changed successfully"
**And** I am returned to the user details page

### AC3: Session Invalidated After Password Change
**Given** the target user has an active session
**When** the ADMIN changes that user's password
**Then** the target user's session is invalidated
**And** the target user is forced to log in again with the new password

### AC4: Validation - Passwords Do Not Match
**Given** I am on the change password form
**When** I enter "NewSecure1!" in New Password
**And** I enter "Different1!" in Confirm New Password
**And** I click "Save"
**Then** I see error "Passwords do not match"
**And** the password is not changed

### AC5: Validation - Password Complexity
**Given** I am on the change password form
**When** I enter "simple" as the new password
**And** I click "Save"
**Then** I see error "Password must be at least 8 characters and include uppercase, lowercase, and a digit"
**And** the password is not changed

### AC6: Cancel Password Change
**Given** I am on the change password form
**When** I click "Cancel"
**Then** I am returned to the user details page
**And** the password is not changed

---

## Technical Notes

- Endpoint: `PATCH /api/v1/users/{id}/password`
- New password hashed with BCrypt strength 10
- Plain-text password never stored, logged, or returned
- Session invalidation applies to the target user only, not the ADMIN performing the action

---

## Dependencies

- US032 (Create User) must be completed
- User must be authenticated as ADMIN

---

## Definition of Done

- [ ] Code implemented and reviewed
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] All acceptance criteria met
- [ ] Manual testing completed

---

**Last Updated**: 2024-01-15
**Status**: Ready for Development
