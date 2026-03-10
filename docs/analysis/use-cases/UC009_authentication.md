# UC009 — Authentication

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC009 |
| **Name** | Authentication |
| **Actor** | ADMIN |
| **Epic** | Security |
| **Flyway** | — (no DDL migration; `app_user` table created in V001) |
| **Status** | ✅ Implemented |
| **Branch** | develop |

Provides session-based authentication for the ImmoCare application. Users authenticate with username and password. All API endpoints require authentication except `/api/v1/auth/**`. The current authenticated user's identity is available to the frontend via `GET /api/v1/auth/me`.

> **Note:** Person management (UC006) was previously misidentified as a second UC009 in the documentation. That file (`UC009_manage_persons.md`) has been removed — refer to `UC006_manage_persons.md` instead.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US036 | Login | MUST HAVE | 2 |
| US037 | Logout | MUST HAVE | 1 |
| US038 | Get Current User | MUST HAVE | 1 |

---

## US036 — Login

**As an** ADMIN **I want to** log in with my credentials **so that** I can access the application.

**Acceptance Criteria:**
- AC1: Required fields: `username`, `password`.
- AC2: Returns HTTP 200 with `UserDTO` on success; session cookie set.
- AC3: Returns HTTP 401 on invalid credentials.
- AC4: Frontend redirects to `/buildings` on successful login.

**Endpoint:** `POST /api/v1/auth/login` (form-encoded)

---

## US037 — Logout

**As an** ADMIN **I want to** log out **so that** I can end my session securely.

**Acceptance Criteria:**
- AC1: Invalidates the current server-side session.
- AC2: Returns HTTP 200. Frontend redirects to `/login`.

**Endpoint:** `POST /api/v1/auth/logout`

---

## US038 — Get Current User

**As an** ADMIN **I want to** retrieve the current authenticated user **so that** the UI can display my identity and enforce access control.

**Acceptance Criteria:**
- AC1: Returns `UserDTO` for the currently authenticated user.
- AC2: Returns HTTP 401 if not authenticated.
- AC3: Called on app bootstrap to restore session state.

**Endpoint:** `GET /api/v1/auth/me`

---

## Security Configuration

| Setting | Value |
|---|---|
| Authentication | Session-based (no JWT in Phase 1) |
| Cookie | `JSESSIONID`, `HttpOnly=true`, `Secure=false` (dev) |
| Session timeout | 30 minutes |
| CSRF | Disabled (SPA + session cookie) |
| CORS | `http://localhost:4200` allowed in dev |
| Protected endpoints | All `/api/v1/**` except `/api/v1/auth/**` |

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC009-01 | All API endpoints except `/api/v1/auth/**` require authentication |
| BR-UC009-02 | Only ADMIN role exists in Phase 1 |
| BR-UC009-03 | Session timeout: 30 minutes of inactivity |
| BR-UC009-04 | Password verified via BCryptPasswordEncoder (strength 12) |

---

## Error Responses

| Condition | HTTP | Description |
|---|---|---|
| Invalid credentials | 401 | Unauthorized |
| Not authenticated | 401 | Unauthorized (on any protected endpoint) |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| POST | `/api/v1/auth/login` | US036 |
| POST | `/api/v1/auth/logout` | US037 |
| GET | `/api/v1/auth/me` | US038 |

---

**Last Updated:** 2026-03-10
**Branch:** develop
**Status:** ✅ Implemented