# ImmoCare - UC007 Manage Users — Implementation Prompt

I want to implement Use Case UC007 - Manage Users for ImmoCare.

## CONTEXT

- Project: ImmoCare (property management system)
- Stack: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- Architecture: API-First, mono-repo
- Authentication: Form-based login (username + password), already implemented
- Authorization: ADMIN role only (Phase 1)
- The `app_user` table and `AppUser` entity already exist (created for the security/login feature)

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC007-manage-users.md`: Flows, business rules, test scenarios
- `docs/analysis/user-stories/US031-035`: Acceptance criteria
- `docs/analysis/roles-permissions.md`: Security model, password policy
- `docs/analysis/data-model.md`: USER entity definition
- `docs/analysis/data-dictionary.md`: USER attribute constraints and validation rules

## USER ENTITY (already exists)

```
app_user {
  id            BIGINT PK AUTO_INCREMENT
  username      VARCHAR(50)  UNIQUE NOT NULL   -- 3-50 chars, alphanumeric + underscore
  password_hash VARCHAR(255) NOT NULL          -- BCrypt hashed, strength 10
  email         VARCHAR(100) UNIQUE NOT NULL   -- valid email format
  role          VARCHAR(20)  NOT NULL          -- default: ADMIN
  created_at    TIMESTAMP    NOT NULL
  updated_at    TIMESTAMP    NOT NULL
}
```

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── config/
├── controller/
├── service/
├── repository/
├── model/
│   ├── entity/
│   └── dto/
├── mapper/
├── exception/
└── security/

frontend/src/app/
├── core/
│   ├── auth/
│   └── services/
├── shared/
└── features/
    └── user/          ← main target
```

## WHAT TO IMPLEMENT

### Backend

1. **`model/dto/UserDTO.java`** — response DTO:
   - `{ id, username, email, role, createdAt, updatedAt }`
   - `password_hash` NEVER included

2. **`model/dto/CreateUserRequest.java`**:
   - `{ username, email, password, confirmPassword, role }`
   - Validated with Bean Validation annotations

3. **`model/dto/UpdateUserRequest.java`**:
   - `{ username, email, role }`
   - Password NOT part of this DTO

4. **`model/dto/ChangePasswordRequest.java`**:
   - `{ newPassword, confirmPassword }`

5. **`mapper/UserMapper.java`** (MapStruct):
   - `AppUser → UserDTO`
   - Never map `password_hash`

6. **`repository/UserRepository.java`** — add the following methods to the existing interface
   (Spring Data JPA will auto-implement them — no `@Query` needed):
   ```java
   boolean existsByUsernameIgnoreCase(String username);
   boolean existsByEmailIgnoreCase(String email);
   long countByRole(String role);
   ```

7. **`service/UserService.java`**:
   - Constructor injects only: `UserRepository`, `UserMapper`, `PasswordEncoder`
   - **DO NOT inject `FindByIndexNameSessionRepository`** — not available with standard HTTP sessions
   - `getAllUsers()` → List<UserDTO>
   - `getUserById(Long id)` → UserDTO
   - `createUser(CreateUserRequest)` → UserDTO
   - `updateUser(Long id, UpdateUserRequest)` → UserDTO
   - `changePassword(Long id, ChangePasswordRequest)` → void (no session invalidation in Phase 1)
   - `deleteUser(Long id, Long currentUserId)` → void
   - Business rules enforced:
     - Username uniqueness check (case-insensitive)
     - Email uniqueness check (case-insensitive)
     - Password and confirmPassword match
     - Password complexity (min 8 chars, 1 uppercase, 1 lowercase, 1 digit)
     - Cannot delete own account (currentUserId == id)
     - Cannot delete last remaining ADMIN

   > **Note BR-UC007-07**: Session invalidation after password change is deferred to a future
   > phase (requires Spring Session JDBC). In Phase 1, the changed password takes effect on
   > the target user's next login after their current session expires naturally (30 min).

8. **`controller/UserController.java`**:
   - `GET    /api/v1/users` → list all users (US031)
   - `GET    /api/v1/users/{id}` → get user details
   - `POST   /api/v1/users` → create user (US032)
   - `PUT    /api/v1/users/{id}` → edit user (US033)
   - `PATCH  /api/v1/users/{id}/password` → change password (US034)
   - `DELETE /api/v1/users/{id}` → delete user (US035)
   - All endpoints: `@PreAuthorize("hasRole('ADMIN')")`

9. **`exception/UserNotFoundException.java`**
10. **`exception/UsernameTakenException.java`**
11. **`exception/EmailTakenException.java`**
12. **`exception/CannotDeleteSelfException.java`**
13. **`exception/CannotDeleteLastAdminException.java`**
    - Wire all into existing `GlobalExceptionHandler`
    - Do NOT create a `GlobalExceptionHandler_ADDITIONS.java` file — add directly to the existing handler

14. **`service/UserServiceTest.java`** (unit tests with Mockito)
    - Do NOT mock `FindByIndexNameSessionRepository` — it is not used
    - `changePassword` test verifies only `passwordEncoder.encode()` and `userRepository.save()`

15. **`controller/UserControllerTest.java`** (integration tests with MockMvc)

### Frontend

16. **`models/user.model.ts`**:
    - `User`, `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest` interfaces

17. **`core/services/user.service.ts`**:
    - `getAll()` → GET `/api/v1/users`
    - `getById(id)` → GET `/api/v1/users/{id}`
    - `create(req)` → POST `/api/v1/users`
    - `update(id, req)` → PUT `/api/v1/users/{id}`
    - `changePassword(id, req)` → PATCH `/api/v1/users/{id}/password`
    - `delete(id)` → DELETE `/api/v1/users/{id}`

18. **`features/user/`** module and routing:
    - `/users` → `UserListComponent`
    - `/users/new` → `UserFormComponent` (create mode)
    - `/users/:id` → `UserDetailsComponent`
    - `/users/:id/edit` → `UserFormComponent` (edit mode)

19. **`features/user/components/user-list/`**:
    - Table: username, email, role, created at
    - Sort by any column
    - Pagination (20/page) — use a `get endIndex()` getter for display, NOT the `| min` pipe
      (Angular has no built-in `min` pipe)
    - "Create User" button
    - Click row → navigate to details (US031)

20. **`features/user/components/user-form/`** (create + edit mode):
    - Fields: username, email, password + confirm (create only), role (dropdown)
    - Real-time validation feedback
    - "Save" and "Cancel" buttons (US032, US033)

21. **`features/user/components/user-details/`**:
    - Display user info (read-only)
    - "Edit" button
    - "Change Password" button → inline form or modal (US034)
    - "Delete" button: disabled with tooltip if self or last admin (US035)
    - Confirmation dialog before delete

22. **`app.module.ts`** — add the `/users` lazy-loaded route with `AuthGuard`:
    ```typescript
    {
      path: 'users',
      canActivate: [AuthGuard],
      loadChildren: () => import('./features/user/user.module').then(m => m.UserModule)
    }
    ```

23. **`app.component.ts`** — add a navigation bar with links to `/buildings` and `/users`,
    and a Logout button that calls `authService.logout()` directly (it returns `void`, not
    an `Observable` — do NOT call `.subscribe()` on it).

## BUSINESS RULES (must be enforced server-side, not only in UI)

- BR-UC007-01: username, email, password, role are required
- BR-UC007-02: Username unique (case-insensitive)
- BR-UC007-03: Email unique
- BR-UC007-04: Password stored as BCrypt hash (strength 10), never plain text, never returned
- BR-UC007-05: Cannot delete own account
- BR-UC007-06: Cannot delete last remaining ADMIN
- BR-UC007-07: Session invalidation after password change — **deferred to Phase 2**
- BR-UC007-08: No self-registration — only ADMIN can create users

## API ENDPOINTS

| Method | Endpoint                        | Description           | Story |
|--------|---------------------------------|-----------------------|-------|
| GET    | /api/v1/users                   | List all users        | US031 |
| GET    | /api/v1/users/{id}              | Get user details      | US031 |
| POST   | /api/v1/users                   | Create user           | US032 |
| PUT    | /api/v1/users/{id}              | Edit user             | US033 |
| PATCH  | /api/v1/users/{id}/password     | Change password       | US034 |
| DELETE | /api/v1/users/{id}              | Delete user           | US035 |

## SECURITY RULES

- `password_hash` never appears in any DTO, API response, or log
- All endpoints require ROLE_ADMIN
- Self-deletion and last-admin-deletion blocked server-side (not only UI)
- BCrypt strength 10 for all password hashing

## WHAT NOT TO DO

- Do NOT inject `FindByIndexNameSessionRepository` anywhere — it requires Spring Session JDBC
  which is not configured in this project
- Do NOT create `*_ADDITIONS.java` files — always modify existing files directly
- Do NOT use the `| min` pipe in Angular templates — it does not exist; use a TypeScript getter
- Do NOT call `.subscribe()` on `authService.logout()` — it returns `void`
- No self-registration endpoint
- No password reset by email (future feature)
- No user profile self-service (future feature)
- Do not expose `password_hash` anywhere
- Do not add a Flyway migration for the user table (already exists from security feature)
- Do not modify `BuildingService.java` — it does not need `HousingUnitRepository`

## ACCEPTANCE CRITERIA

- [ ] GET /api/v1/users returns list of users without password_hash
- [ ] POST /api/v1/users creates a user with hashed password
- [ ] POST /api/v1/users returns 409 if username or email already exists
- [ ] PUT /api/v1/users/{id} updates username/email/role
- [ ] PATCH /api/v1/users/{id}/password updates password hash
- [ ] DELETE /api/v1/users/{id} deletes user
- [ ] DELETE /api/v1/users/{id} returns 403 when attempting self-deletion
- [ ] DELETE /api/v1/users/{id} returns 409 when attempting to delete last ADMIN
- [ ] Angular user list displays username, email, role, created at
- [ ] Angular create form validates all fields before submission
- [ ] Angular delete button disabled with tooltip for self and last admin
- [ ] Navigation bar shows links to Buildings and Users
- [ ] All acceptance criteria from US031 to US035 are met

## IMPLEMENTATION ORDER

1. `UserDTO`, `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest`
2. `UserMapper`
3. `UserRepository` (add 3 methods)
4. `UserService` + `UserServiceTest`
5. Exception classes + `GlobalExceptionHandler` updates (in-place)
6. `UserController` + `UserControllerTest`
7. `user.model.ts`
8. `user.service.ts`
9. `UserListComponent`
10. `UserFormComponent`
11. `UserDetailsComponent`
12. User module and routing (`user.module.ts`)
13. `app.module.ts` (add `/users` route)
14. `app.component.ts` (add navigation bar)