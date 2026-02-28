# User Story US032: Create User

| Attribute | Value |
|-----------|-------|
| **Story ID** | US032 |
| **Epic** | User Management |
| **Related UC** | UC007 |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** create a new user account **so that** I can grant system access.

## Acceptance Criteria

**AC1:** Click "Create User" → form with username*, email*, password*, confirm password*, role* (default ADMIN).
**AC2:** Fill valid data → save → user created, "User created successfully", redirected to user details. Password NOT shown anywhere.
**AC3:** Username "jane_doe" already exists → error "Username already exists".
**AC4:** Email already in use → error "Email already in use".
**AC5:** Passwords don't match → error "Passwords do not match".
**AC6:** Password "simple" (too weak) → error "Password must be at least 8 characters and include uppercase, lowercase, and a digit".
**AC7:** Invalid email format → error "Please enter a valid email address".
**AC8:** Cancel with data entered → confirmation dialog; if confirmed, no user created.

**Endpoint:** `POST /api/v1/users` — HTTP 201. Password hashed BCrypt strength 10.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
