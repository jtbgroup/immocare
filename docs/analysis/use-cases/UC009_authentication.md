# UC009 — Authentication

## Overview

| Attribute | Value |
|---|---|
| ID | UC009 |
| Name | Authentication |
| Actor | Admin |
| Module | Security |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Provides session-based authentication for the ImmoCare application. Users authenticate with username and password. All API endpoints require authentication except the login endpoint. The current authenticated user's identity is available to the frontend.

---

## User Stories

### US036 — Login

**As an** admin, **I want to** log in with my credentials **so that** I can access the application.

**Acceptance Criteria:**
- AC1: Required fields: `username`, `password`.
- AC2: Returns HTTP 200 with `UserDTO` on success.
- AC3: Returns HTTP 401 on invalid credentials.
- AC4: A session cookie is set on successful login.

**Endpoint:** `POST /api/v1/auth/login`

---

### US037 — Logout

**As an** admin, **I want to** log out **so that** I can end my session securely.

**Acceptance Criteria:**
- AC1: Invalidates the current session.
- AC2: Returns HTTP 200 on success.

**Endpoint:** `POST /api/v1/auth/logout`

---

### US038 — Get Current User

**As an** admin, **I want to** retrieve the current authenticated user **so that** the UI can display my identity.

**Acceptance Criteria:**
- AC1: Returns `UserDTO` for the currently authenticated user.
- AC2: Returns HTTP 401 if not authenticated.

**Endpoint:** `GET /api/v1/auth/me`

---

## Security Configuration

- Session-based authentication (no JWT in Phase 1).
- `JSESSIONID` cookie: `HttpOnly=true`, `Secure=false` (dev), `SameSite=strict`.
- Session timeout: 30 minutes.
- All `/api/v1/**` endpoints require `ROLE_ADMIN` except `/api/v1/auth/**`.
- CSRF disabled (SPA context, session cookie).
- CORS: `http://localhost:4200` allowed in development.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC009-01 | All API endpoints except `/auth/**` require authentication |
| BR-UC009-02 | Only ADMIN role is valid in Phase 1 |
| BR-UC009-03 | Session timeout: 30 minutes |
| BR-UC009-04 | Password verified via BCryptPasswordEncoder |

---

## Error Responses

| Condition | HTTP | Description |
|---|---|---|
| Invalid credentials | 401 | Unauthorized |
| Not authenticated | 401 | Unauthorized (on any protected endpoint) |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC009 — Authentication in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Security 6, session-based (no JWT)
- Frontend: Angular 17 standalone, HttpClient with withCredentials, AuthGuard, AuthService
- Branch: develop

Backend classes to generate:
1. `SecurityConfig` (@Configuration, @EnableWebSecurity):
   - Session management: ALWAYS (stateful), max 1 session
   - CSRF: disabled
   - CORS: allow origin http://localhost:4200, all methods, all headers, credentials=true
   - authorizeHttpRequests: permit /api/v1/auth/**, require ROLE_ADMIN on /api/v1/**
   - formLogin: disabled
   - httpBasic: disabled
   - logout: disabled (handled by controller)
   - sessionFixation: migrateSession
2. `UserDetailsServiceImpl` implements UserDetailsService — loadUserByUsername(username) from UserRepository
3. `AuthController` — @RequestMapping("/api/v1/auth"):
   - POST /login: @RequestBody LoginRequest(username, password), authenticates via AuthenticationManager, sets SecurityContext, returns UserDTO
   - POST /logout: invalidates HttpSession, clears SecurityContext, returns 200
   - GET /me: returns UserDTO for @AuthenticationPrincipal AppUser
4. `LoginRequest` DTO: username, password (both required)
5. `AuthenticationManager` bean using DaoAuthenticationProvider + BCryptPasswordEncoder

Frontend classes to generate:
1. Model: `auth.model.ts` — LoginRequest, CurrentUser (extends UserDTO)
2. Service: `AuthService` — login(req), logout(), getCurrentUser(), isLoggedIn(): Observable<boolean>. Caches currentUser in BehaviorSubject.
3. Guard: `AuthGuard` implements CanActivate — calls getCurrentUser(), redirects to /login if 401
4. Interceptor: `CredentialsInterceptor` (or configure HttpClient with withCredentials globally)
5. Components (standalone):
   - `LoginComponent` — simple form (username + password), calls AuthService.login(), navigates to / on success, shows error on 401
   - `NavbarComponent` — shows current username, logout button
6. App routing: /login route (no guard), all other routes guarded by AuthGuard

Notes:
- Use withCredentials: true on all HTTP requests (session cookie)
- On 401 from any API call, redirect to /login (HTTP interceptor)
- Store currentUser in AuthService BehaviorSubject, not localStorage
```
