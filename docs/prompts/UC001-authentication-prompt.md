I want to implement the security layer and login feature for ImmoCare.

## CONTEXT

- Project: ImmoCare (property management system)
- Stack: Spring Boot 3.x (backend) + Angular 17+ (frontend) + PostgreSQL 15
- Architecture: API-First, mono-repo
- Authentication: Form-based login (username + password)
- Authorization: Role-based (ADMIN only in Phase 1)
- All API endpoints must require authentication

## REFERENCE DOCUMENTS

- `docs/analysis/roles-permissions.md`: Security model, session config, password policy
- `docs/analysis/data-model.md`: USER entity definition
- `docs/analysis/data-dictionary.md`: USER attribute constraints and validation rules

## USER ENTITY (already in data model)

```
USER {
  id          BIGINT PK AUTO_INCREMENT
  username    VARCHAR(50)  UNIQUE NOT NULL   -- 3-50 chars, alphanumeric + underscore
  password_hash VARCHAR(255) NOT NULL        -- BCrypt hashed
  email       VARCHAR(100) UNIQUE NOT NULL   -- valid email format
  role        VARCHAR(20)  NOT NULL          -- default: ADMIN
  created_at  TIMESTAMP    NOT NULL
  updated_at  TIMESTAMP    NOT NULL
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
└── security/          ← main target

backend/src/main/resources/
└── db/migration/      ← add Flyway script for USER table

frontend/src/app/
├── core/
│   ├── auth/          ← guards, interceptors, services
│   └── services/
├── shared/
└── features/
    └── auth/          ← login page component
```

## WHAT TO IMPLEMENT

### Backend

1. **Flyway migration** — `db/migration/V001__create_users_table.sql`
   - Create `app_user` table (avoid reserved keyword `user`)
   - Insert default admin user: username=`admin`, password=`Admin1234!` (BCrypt hashed)

2. **`model/entity/AppUser.java`** — JPA entity implementing `UserDetails`

3. **`repository/UserRepository.java`** — `findByUsername(String username)`

4. **`security/UserDetailsServiceImpl.java`** — implements `UserDetailsService`, loads user by username

5. **`config/SecurityConfig.java`** — Spring Security configuration:
   - All `/api/v1/**` endpoints require `ROLE_ADMIN`
   - `/login` and `/logout` are public
   - Session-based authentication (no JWT in Phase 1)
   - Session timeout: 30 minutes
   - 1 concurrent session per user
   - CSRF: disabled for REST API (SPA client)
   - BCryptPasswordEncoder with strength 10
   - Returns 401 JSON on unauthorized (no redirect for API calls)

6. **`controller/AuthController.java`**
   - `GET /api/v1/auth/me` → returns current authenticated user info (username, role)
   - `POST /api/v1/auth/logout` → invalidates session

7. **`model/dto/AuthUserDTO.java`** — `{ username, role }`

### Frontend

8. **`features/auth/login/login.component.ts`** — login page:
   - Form: username + password fields
   - Submits `POST /login` (Spring Security default form login endpoint)
   - On success: redirect to `/` (main app)
   - On failure: display error message
   - No registration link (users are managed by admin only)

9. **`core/auth/auth.service.ts`**
   - `login(username, password)` → POST to `/login`
   - `logout()` → POST to `/api/v1/auth/logout`
   - `getCurrentUser()` → GET `/api/v1/auth/me`
   - `isAuthenticated()` → boolean observable

10. **`core/auth/auth.guard.ts`** — `CanActivate` guard:
    - Calls `getCurrentUser()`
    - If not authenticated → redirect to `/login`

11. **`core/auth/auth.interceptor.ts`** — HTTP interceptor:
    - On 401 response → redirect to `/login`
    - Passes cookies with each request (`withCredentials: true`)

12. **`app.routes.ts`** — routing:
    - `/login` → `LoginComponent` (public)
    - All other routes → protected by `AuthGuard`

## SECURITY RULES

- Password stored as BCrypt hash (strength 10), never plain text
- Session cookie: HTTP-only, Secure (HTTPS), SameSite=Strict
- Session timeout: 30 minutes of inactivity
- Maximum 1 concurrent session per user
- All `/api/v1/**` endpoints return 401 JSON (not HTML redirect) when unauthenticated
- Default admin credentials must be changed after first login (warn in UI, enforce later)

## EXPECTED BEHAVIOR

1. User opens the app → redirected to `/login` if not authenticated
2. User submits username + password → session created on success
3. All subsequent API calls include session cookie automatically
4. After 30 minutes of inactivity → session expires → user redirected to `/login`
5. User clicks logout → session destroyed → redirected to `/login`
6. `GET /api/v1/auth/me` returns `{ username: "admin", role: "ADMIN" }` when authenticated

## WHAT NOT TO DO

- No JWT tokens (Phase 1 uses server-side sessions only)
- No OAuth2 / Keycloak (planned for future phase)
- No user self-registration
- No password reset flow (future feature)
- Do not expose `password_hash` in any DTO or API response

## ACCEPTANCE CRITERIA

- [ ] Unauthenticated access to any `/api/v1/**` endpoint returns HTTP 401 with JSON body
- [ ] Login with valid credentials creates a session and returns HTTP 200
- [ ] Login with invalid credentials returns HTTP 401
- [ ] Session expires after 30 minutes of inactivity
- [ ] Logout invalidates the session
- [ ] Angular app redirects to `/login` on startup if not authenticated
- [ ] Angular app redirects to `/login` on any 401 API response
- [ ] Default admin user exists in the database after Flyway migration