# Authentication

This document describes the authentication behavior currently implemented by the application. It consolidates the login, logout, and current-user behavior without preserving the former use-case or story breakdown.

## Authentication and authorization

Authentication establishes the identity of the signed-in application user and maintains that identity in a server-side session. Authorization is a separate concern: platform administrator status and estate membership determine which operations the authenticated user may perform. See [Roles and Responsibilities](../business/roles.md) for the current application access roles.

## Authentication lifecycle

### Sign in

- The client sends a username and password to `POST /api/v1/auth/login`.
- On successful authentication, the server invalidates any existing session for the request, creates a new session, stores the security context, and responds with HTTP 200 and an empty body.
- Invalid credentials produce HTTP 401.
- The client then retrieves the current user and the estates available to that user to determine the appropriate destination:
  - one estate: open that estate's dashboard;
  - multiple estates: open the estate selector;
  - no estates and a platform administrator: open the platform estate administration page;
  - otherwise: open the estate selector.

### Sign out

- The client sends `POST /api/v1/auth/logout` with the session cookie.
- The server invalidates the current session, clears the security context, and responds with HTTP 200.
- The client clears its current-user and active-estate state and navigates to the sign-in page.

### Retrieve the current user

- `GET /api/v1/auth/me` returns the authenticated user's `UserDTO`: `id`, `username`, `email`, `isPlatformAdmin`, `createdAt`, and `updatedAt`.
- An unauthenticated request receives HTTP 401.
- The frontend calls this endpoint to restore/check the signed-in state, and redirects to the sign-in page when it receives HTTP 401.

## Current security configuration

| Setting | Current implementation |
|---|---|
| Session model | Server-side HTTP session |
| Public endpoints | `POST /api/v1/auth/login` and `/actuator/**`; other requests require authentication |
| Password encoder | BCrypt with strength 12 |
| Session timeout | 30 minutes, as configured in `application.yml` |
| Session cookie | `HttpOnly`, `SameSite=Lax`, and `Secure=false` in the current configuration |
| CSRF | Disabled in `SecurityConfig` |
| Unauthenticated API response | HTTP 401, not an HTML login redirect |

The session cookie's `Secure=false` setting is the value currently present in configuration; it must be reviewed for deployments served over HTTPS.

## Related access behavior

Authentication does not select or store the active estate in the server session. After sign-in, the frontend chooses an estate context; access to estate-scoped resources is authorized separately using the authenticated user's platform-admin status or estate membership. The estate context and role rules are described in [Roles and Responsibilities](../business/roles.md).

## Implementation references

- Backend authentication endpoints: [AuthController](../../backend/src/main/java/com/immocare/controller/AuthController.java)
- Backend security configuration: [SecurityConfig](../../backend/src/main/java/com/immocare/config/SecurityConfig.java)
- Session settings: [application.yml](../../backend/src/main/resources/application.yml)
- Frontend session and post-sign-in flow: [AuthService](../../frontend/src/app/core/auth/auth.service.ts)
- Frontend route check: [AuthGuard](../../frontend/src/app/core/auth/auth.guard.ts)
- Frontend handling of session cookies and HTTP 401 responses: [AuthInterceptor](../../frontend/src/app/core/auth/auth.interceptor.ts)
