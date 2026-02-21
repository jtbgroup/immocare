# User Story US033: Edit User

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US033 |
| **Story Name** | Edit User |
| **Epic** | User Management |
| **Related UC** | UC007 - Manage Users |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** edit a user's information
**So that** I can keep user data up to date

---

## Acceptance Criteria

### AC1: Navigate to Edit Form
**Given** I am viewing a user's details page
**When** I click the "Edit" button
**Then** the edit form is displayed
**And** all fields are pre-filled with the current values
**And** the password field is NOT shown

### AC2: Edit Username Successfully
**Given** I am on the user edit form
**When** I change the username to "jane_smith"
**And** I click "Save"
**Then** the user is updated
**And** I see success message "User updated successfully"
**And** the details page shows the new username

### AC3: Edit Email Successfully
**Given** I am on the user edit form
**When** I change the email to "new.email@example.com"
**And** I click "Save"
**Then** the user is updated with the new email

### AC4: Validation - Duplicate Username on Edit
**Given** user "other_admin" already exists
**When** I edit a different user and set their username to "other_admin"
**Then** I see error "Username already exists"
**And** the form is not submitted

### AC5: Validation - Duplicate Email on Edit
**Given** email "taken@example.com" belongs to another user
**When** I edit a user and set their email to "taken@example.com"
**Then** I see error "Email already in use"
**And** the form is not submitted

### AC6: Validation - Required Field Cleared
**Given** I am on the user edit form
**When** I clear the Username field
**And** I click "Save"
**Then** I see error "Username is required"
**And** the form is not submitted

### AC7: Cancel Editing
**Given** I am on the user edit form with changes made
**When** I click "Cancel"
**Then** a confirmation dialog appears: "You have unsaved changes. Are you sure you want to cancel?"
**And** if I confirm, I am returned to the user details page without changes

### AC8: Audit Trail Updated
**Given** I edit a user
**Then** the user record shows an updated "Updated at" timestamp

---

## Technical Notes

- Endpoint: `PUT /api/v1/users/{id}`
- Password is not part of this form (see US034)
- Role dropdown present for future extensibility but only ADMIN available in Phase 1

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
