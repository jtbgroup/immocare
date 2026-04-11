# UC007 — Manage Users

## Overview

| Attribute | Value |
|---|---|
| ID | UC007 |
| Name | Manage Users |
| Actor | Admin |
| Module | Administration |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to manage application users (accounts). Only ADMIN-role users exist in Phase 1. A user cannot delete their own account. Password changes follow a separate endpoint with confirmation validation.

---

## User Stories

### US031 — View User List

**As an** admin, **I want to** view a list of all user accounts **so that** I can oversee who has access to the system.

**Acceptance Criteria:**
- AC1: Displays all users: Username, Email, Role, Created at. Page title "Users". "Create User" button visible.
- AC2: If only my own account exists, only my account is displayed.
- AC3: Clicking a column header (Username, Email, Role, Created at) sorts the list; clicking again reverses the sort.
- AC4: Clicking a user row navigates to that user's details page (showing all fields; password hash never returned).
- AC5: More than 20 users: pagination controls shown, 20 users per page. Default sort: username ASC.

**Endpoints:**
- `GET /api/v1/users` — list all
- `GET /api/v1/users/{id}` — user details (HTTP 404 if not found)

---

### US032 — Create User

**As an** admin, **I want to** create a new user account **so that** I can grant system access to another person.

**Acceptance Criteria:**
- AC1: Form shows required fields marked with *; Role field defaults to "ADMIN".
- AC2: On valid save: user created, success message "User created successfully", redirected to user details page. Password NOT visible on details page.
- AC3: Duplicate username → error "Username already exists".
- AC4: Duplicate email → error "Email already in use".
- AC5: Passwords do not match → error "Passwords do not match".
- AC6: Password complexity (< 8 chars or missing upper/lower/digit) → error "Password must be at least 8 characters and include uppercase, lowercase, and a digit".
- AC7: Invalid email format → error "Please enter a valid email address".
- AC8: Cancel with unsaved changes → confirmation dialog before leaving.

**Endpoint:** `POST /api/v1/users`

---

### US033 — Edit User

**As an** admin, **I want to** edit a user's information **so that** I can keep user data up to date.

**Acceptance Criteria:**
- AC1: Edit form pre-filled with current values; password field NOT shown.
- AC2: Edit username → success message "User updated successfully"; details page shows new username.
- AC3: Edit email → user updated with new email.
- AC4: Duplicate username on edit → error "Username already exists".
- AC5: Duplicate email on edit → error "Email already in use".
- AC6: Clear required field → error (e.g., "Username is required").
- AC7: Cancel with changes → confirmation dialog; returns to details without saving.
- AC8: `updatedAt` timestamp updated after save.

**Endpoint:** `PUT /api/v1/users/{id}`

---

### US034 — Change User Password

**As an** admin, **I want to** change a user's password **so that** I can reset access when a user forgets their credentials.

**Acceptance Criteria:**
- AC1: "Change Password" form shows New Password and Confirm New Password fields only (no current password).
- AC2: On valid save: success message "Password changed successfully"; returned to user details.
- AC3: Target user's active session is invalidated; they must log in again with the new password.
- AC4: Passwords do not match → error "Passwords do not match".
- AC5: Password complexity failure → error "Password must be at least 8 characters and include uppercase, lowercase, and a digit".
- AC6: Cancel → returned to user details, password unchanged.

**Endpoint:** `PATCH /api/v1/users/{id}/password`

---

### US035 — Delete User

**As an** admin, **I want to** delete a user account **so that** I can revoke system access when a person leaves.

**Acceptance Criteria:**
- AC1: Confirmation dialog shows username in bold, "This action cannot be undone", Cancel and Delete buttons.
- AC2: On confirm: user permanently deleted, redirected to users list, success message "User deleted successfully".
- AC3: Cancel: dialog closes, user not deleted.
- AC4: Cannot delete own account — "Delete" button disabled with tooltip "You cannot delete your own account".
- AC5: Cannot delete last administrator account — button disabled with tooltip "Cannot delete the last administrator account".
- AC6: Deleted user cannot log in (credentials rejected).
- AC7: Deleted user's active session is invalidated.

**Endpoint:** `DELETE /api/v1/users/{id}`

---

## Data Model

### Table: `app_user`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT `'ADMIN'` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Default seed: `admin / admin@immocare.com / admin123` (BCrypt 12).

---

## DTOs

### `UserDTO` (response)
```
id, username, email, role, createdAt, updatedAt
```
Never includes password hash.

### `CreateUserRequest` (POST body)
```
username*         VARCHAR(50)   unique, case-insensitive
email*            VARCHAR(100)  unique, case-insensitive
password*         String        complexity rules
confirmPassword*  String        must equal password
role*             String        ADMIN (Phase 1 only)
```

### `UpdateUserRequest` (PUT body)
```
username*  VARCHAR(50)
email*     VARCHAR(100)
role*      String
```

### `ChangePasswordRequest` (PATCH body)
```
newPassword*        String  complexity rules
confirmNewPassword* String  must equal newPassword
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC007-01 | Only ADMIN role exists in Phase 1 |
| BR-UC007-02 | `username` must be unique (case-insensitive) |
| BR-UC007-03 | `email` must be unique (case-insensitive) |
| BR-UC007-04 | Password: ≥ 8 chars, ≥ 1 upper, ≥ 1 lower, ≥ 1 digit |
| BR-UC007-05 | `password` must equal `confirmPassword` |
| BR-UC007-06 | Password stored as BCrypt(12) hash |
| BR-UC007-07 | An admin cannot delete their own account |
| BR-UC007-08 | `UpdateUserRequest` does not update the password |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| User not found | 404 | `User not found` |
| Duplicate username | 409 | `UsernameTakenException` |
| Duplicate email | 409 | `EmailTakenException` |
| Password mismatch or complexity | 400 | validation message |
| Self-delete | 409 | `Cannot delete your own account` |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC007 — Manage Users in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, Spring Security (ROLE_ADMIN on all endpoints), BCrypt
- Frontend: Angular 17 standalone components, TypeScript, SCSS
- Database: Flyway V001 already contains `app_user` table — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Entity: `AppUser` implements `UserDetails` — table `app_user`. Fields: id, username, passwordHash, email, role, createdAt, updatedAt. @PrePersist/@PreUpdate. getAuthorities() returns ROLE_+role. getPassword() returns passwordHash.
2. DTOs: `UserDTO` (no password), `CreateUserRequest` (validated), `UpdateUserRequest` (validated), `ChangePasswordRequest` (validated)
3. Mapper: `UserMapper` (MapStruct) — ignore passwordHash
4. Repository: `UserRepository` — existsByUsernameIgnoreCase, existsByEmailIgnoreCase, findByUsername (for Spring Security UserDetailsService)
5. Exceptions: `UsernameTakenException`, `EmailTakenException`
6. Service: `UserService` — getAllUsers(), getUserById(id), createUser(req), updateUser(id, req), changePassword(id, req), deleteUser(id, currentUserId). Enforce all BR-UC007-xx. Password regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$
7. Controller: `UserController` — @RequestMapping("/api/v1/users"), all endpoints as per US030–US035. Use @AuthenticationPrincipal AppUser currentUser on DELETE to get current user id.
8. Security config: `UserDetailsService` implementation loading by username, BCryptPasswordEncoder bean (strength 12).

Frontend classes to generate:
1. Model: `user.model.ts` — User, CreateUserRequest, UpdateUserRequest, ChangePasswordRequest
2. Service: `UserService` — getAll(), getById(id), create(req), update(id, req), changePassword(id, req), delete(id)
3. Components (standalone):
   - `UserListComponent` — table of users with create/edit/delete actions
   - `UserFormComponent` — reactive form for create/edit. Create: shows password + confirm. Edit: no password fields.
   - `ChangePasswordFormComponent` — modal/inline form for password change (newPassword + confirmNewPassword)

Business rules to enforce in frontend:
- BR-UC007-04/05: password complexity regex + confirm match validator
- BR-UC007-07: hide delete button when row.id === currentUser.id
- Role dropdown: only shows ADMIN (Phase 1)
```
