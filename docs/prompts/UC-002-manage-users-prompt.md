# ImmoCare — UC006 Manage Users — Implementation Prompt

I want to implement Use Case UC006 - Manage Users for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Note**: `app_user` table and `AppUser` entity already exist (created for security/login)

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| UC002.001 | View User List | MUST HAVE | 2 |
| UC002.002 | Create User | MUST HAVE | 5 |
| UC002.003 | Edit User | MUST HAVE | 3 |
| UC002.004 | Change User Password | MUST HAVE | 3 |
| UC002.005 | Delete User | MUST HAVE | 2 |

## USER ENTITY (already exists)

```
app_user {
  id            BIGINT PK
  username      VARCHAR(50)  UNIQUE NOT NULL  -- 3–50 chars, alphanumeric + underscore
  password_hash VARCHAR(255) NOT NULL          -- BCrypt strength 10
  email         VARCHAR(100) UNIQUE NOT NULL
  role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
  created_at    TIMESTAMP    NOT NULL
  updated_at    TIMESTAMP    NOT NULL
}
Default seed: admin / admin@immocare.com / admin123 (BCrypt 12)
```

## BACKEND

1. `UserDTO` — `{ id, username, email, role, createdAt, updatedAt }` — NEVER includes password_hash
2. `CreateUserRequest` — `{ @NotBlank username, @Email email, @NotBlank password, confirmPassword, @NotBlank role }`
3. `UpdateUserRequest` — `{ username, email, role }` — NO password field
4. `ChangePasswordRequest` — `{ newPassword, confirmPassword }`
5. `UserMapper` (MapStruct) — `AppUser → UserDTO`, never map `password_hash`
6. `UserRepository` — add: `existsByUsernameIgnoreCase`, `existsByEmailIgnoreCase`, `countByRole`
7. `UserService`:
   - `getAllUsers()`, `getUserById(id)`, `createUser(req)`, `updateUser(id, req)`, `changePassword(id, req)`, `deleteUser(id, currentUserId)`
   - BR-UC006-02: username unique (case-insensitive)
   - BR-UC006-03: email unique
   - BR-UC006-04: password BCrypt strength 10; complexity: ≥8 chars, 1 upper, 1 lower, 1 digit
   - BR-UC006-05: cannot delete own account
   - BR-UC006-06: cannot delete last ADMIN
   - **DO NOT** inject `FindByIndexNameSessionRepository`
8. `UserController` — all endpoints `@PreAuthorize("isAuthenticated()")`:

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/users | UC002.001 |
| GET | /api/v1/users/{id} | UC002.001 |
| POST | /api/v1/users | UC002.002 |
| PUT | /api/v1/users/{id} | UC002.003 |
| PATCH | /api/v1/users/{id}/password | UC002.004 |
| DELETE | /api/v1/users/{id} | UC002.005 |

9. Exceptions: `UserNotFoundException`, `UsernameTakenException`, `EmailTakenException`, `CannotDeleteSelfException`, `CannotDeleteLastAdminException`

## FRONTEND

10. `user.model.ts` — `User`, `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest`
11. `user.service.ts`
12. `UserListComponent` — table: username, email, role, created at; sort by username; pagination (UC002.001)
13. `UserFormComponent` — create/edit; role dropdown (ADMIN only Phase 1); no password on edit (UC002.002, UC002.003)
14. `UserDetailsComponent` — display all fields; "Edit", "Change Password", "Delete" buttons
15. `ChangePasswordFormComponent` — new password + confirm only; no current password (UC002.004)

## BUSINESS RULES (server-side mandatory)

- `password_hash` NEVER in any DTO, API response, or log
- Self-deletion and last-admin-deletion blocked server-side (not just UI)
- Do NOT call `.subscribe()` on `authService.logout()` — it returns `void`
- Do NOT use `| min` pipe in Angular templates

## ACCEPTANCE CRITERIA

- [ ] Password hash never appears anywhere in API responses
- [ ] Cannot delete own account (409)
- [ ] Cannot delete last admin (409)
- [ ] Password complexity enforced server-side
- [ ] All UC002.001–UC002.005 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
