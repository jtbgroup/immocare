# User Story US031: View User List

## Story Information

| Attribute | Value |
|-----------|-------|
| **Story ID** | US031 |
| **Story Name** | View User List |
| **Epic** | User Management |
| **Related UC** | UC007 - Manage Users |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |
| **Sprint** | Sprint 1 |

---

## User Story

**As an** ADMIN
**I want to** view a list of all user accounts
**So that** I can oversee who has access to the system

---

## Acceptance Criteria

### AC1: Display Users List
**Given** I am logged in as ADMIN
**And** 3 users exist in the system
**When** I navigate to the Users page
**Then** I see a list displaying all 3 users
**And** each user shows:
- Username
- Email
- Role
- Created at (date)
**And** the page title is "Users"
**And** a "Create User" button is visible

### AC2: View Empty Users List
**Given** no users other than the current ADMIN exist
**When** I navigate to the Users page
**Then** only my own account is displayed
**And** a "Create User" button is visible

### AC3: Sort Users by Username
**Given** I am viewing the users list
**When** I click the "Username" column header
**Then** the list is sorted alphabetically (A → Z)
**When** I click again
**Then** the list is sorted in reverse order (Z → A)

### AC4: Navigate to User Details
**Given** I am viewing the users list
**When** I click on a user row
**Then** I am taken to that user's details page

### AC5: Pagination
**Given** more than 20 users exist
**When** I view the users list
**Then** the list shows 20 users per page
**And** pagination controls are displayed

---

## Technical Notes

- Endpoint: `GET /api/v1/users`
- Password hash must never appear in the response
- Sort default: username ASC

---

## Dependencies

- User must be authenticated as ADMIN
- US032 (Create User) required for meaningful test data

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
