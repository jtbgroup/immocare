# User Story US032: Create User

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US032 |
| **Story Name** | Create User |
| **Epic** | User Management |
| **Related UC** | UC007 - Manage Users |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** create a new user account
**So that** I can grant system access to another person

---

## Acceptance Criteria

### AC1: Display User Creation Form
**Given** I am logged in as ADMIN
**When** I navigate to the Users page
**And** I click the "Create User" button
**Then** the user creation form is displayed
**And** all required fields are marked with an asterisk (*)
**And** the Role field defaults to "ADMIN"

### AC2: Create User with Valid Data
**Given** I am on the user creation form
**When** I enter:
- Username: "jane_doe"
- Email: "jane.doe@example.com"
- Password: "Secure1234!"
- Confirm Password: "Secure1234!"
- Role: ADMIN

**And** I click "Save"
**Then** the user is created in the system
**And** I see success message "User created successfully"
**And** I am redirected to the user details page
**And** the password is NOT visible anywhere on the details page

### AC3: Validation - Duplicate Username
**Given** a user with username "jane_doe" already exists
**When** I try to create another user with username "jane_doe"
**Then** I see error "Username already exists"
**And** the form is not submitted

### AC4: Validation - Duplicate Email
**Given** a user with email "jane.doe@example.com" already exists
**When** I try to create another user with the same email
**Then** I see error "Email already in use"
**And** the form is not submitted

### AC5: Validation - Passwords Do Not Match
**Given** I am on the user creation form
**When** I enter "Password1!" in Password
**And** I enter "Different1!" in Confirm Password
**And** I click "Save"
**Then** I see error "Passwords do not match"
**And** the form is not submitted

### AC6: Validation - Password Complexity
**Given** I am on the user creation form
**When** I enter password "simple"
**And** I click "Save"
**Then** I see error "Password must be at least 8 characters and include uppercase, lowercase, and a digit"
**And** the form is not submitted

### AC7: Validation - Invalid Email Format
**Given** I am on the user creation form
**When** I enter "not-an-email" in the Email field
**And** I click "Save"
**Then** I see error "Please enter a valid email address"
**And** the form is not submitted

### AC8: Cancel User Creation
**Given** I am on the user creation form with data entered
**When** I click "Cancel"
**Then** a confirmation dialog appears: "You have unsaved changes. Are you sure you want to cancel?"
**And** if I confirm, I am returned to the users list without any user being created

### AC9: Audit Trail
**Given** I create a user
**Then** the user record shows:
- Created at: current timestamp
- Updated at: current timestamp

---

## Technical Notes

- Endpoint: `POST /api/v1/users`
- Password hashed with BCrypt strength 10 before storage
- Plain-text password never stored, logged, or returned by any API
- Username: 3–50 chars, alphanumeric and underscore only

---

## Dependencies

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
