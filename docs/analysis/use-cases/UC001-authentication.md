# UC001 — Authentication

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC001 |
| **Name** | Authentication |
| **Actor** | ADMIN |
| **Epic** | Security |
| **Flyway** | V001 (`app_user` table) |
| **Status** | ✅ Implemented |
| **Branch** | develop |

Provides session-based authentication for the ImmoCare application. Users authenticate with username and password. All API endpoints require authentication except `/api/v1/auth/**`. The current authenticated user's identity is available to the frontend via `GET /api/v1/auth/me`.

The Spring Security configuration is structured so that switching to OAuth2/Keycloak (or any other authentication provider) only requires adding the resource server starter and swapping the `SecurityFilterChain` bean — no other changes.

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

**Endpoint:** `POST /api/v1/auth/login`

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
| Session timeout | 30 minutes of inactivity |
| Concurrent sessions | 1 per user |
| CSRF | Disabled (SPA + session cookie) |
| CORS | `http://localhost:4200` allowed in dev |
| Protected endpoints | All `/api/v1/**` except `/api/v1/auth/**` |
| Unauthenticated response | HTTP 401 JSON (no HTML redirect) |
| Password encoding | BCrypt strength 12 |

### Spring Security Structure

The `SecurityFilterChain` bean is the only component that needs to be swapped when migrating to OAuth2/Keycloak. All other components (controllers, services, repositories) remain unchanged.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .sessionFixation().migrateSession()
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }
}
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC001-01 | All API endpoints except `/api/v1/auth/**` require authentication |
| BR-UC001-02 | Only ADMIN role exists in Phase 1 |
| BR-UC001-03 | Session timeout: 30 minutes of inactivity |
| BR-UC001-04 | Password verified via BCryptPasswordEncoder (strength 12) |
| BR-UC001-05 | Maximum 1 concurrent session per user |
| BR-UC001-06 | All `/api/v1/**` endpoints return HTTP 401 JSON (not HTML redirect) when unauthenticated |

---

## Migration Path to OAuth2 / Keycloak (Future)

### Planned Migration
- **From**: Embedded session-based authentication
- **To**: External identity provider (Keycloak)
- **Benefits**: SSO, MFA, centralized user management, LDAP/AD integration, social login

### Migration Strategy
1. Deploy Keycloak alongside the application
2. Migrate existing users to Keycloak
3. Swap `SecurityFilterChain` bean to use OAuth2 / OIDC resource server
4. Maintain role mappings (`ROLE_ADMIN`)
5. Deprecate embedded `UserDetailsServiceImpl`

No other backend or frontend changes required by design.

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

**Last Updated:** 2026-04-12
**Branch:** develop
**Status:** ✅ Implemented