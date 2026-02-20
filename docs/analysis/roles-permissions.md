# ImmoCare - Roles and Permissions

## Overview

This document defines user roles and their associated permissions in the ImmoCare application. The initial version supports a simple role model that will be expanded in future phases.

---

## Current Roles (Phase 1)

### ADMIN

**Description**: Full system administrator with unrestricted access to all features and data.

**Capabilities**:
- ✅ Full CRUD operations on all entities
- ✅ User management (create, edit, delete users)
- ✅ Building management
- ✅ Housing unit management
- ✅ Room management
- ✅ PEB score management
- ✅ Rent management
- ✅ Water meter management
- ✅ View all data across the system
- ✅ Access to system configuration
- ✅ Export data

**Use Cases**:
- System administrators
- Property managers with full control
- Initial system setup

**Default User**: The system will have a default admin user created during installation.

---

## Permission Matrix (Phase 1)

| Resource | ADMIN |
|----------|-------|
| **Buildings** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **Housing Units** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **Rooms** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **PEB Scores** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **Rents** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **Water Meters** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **Users** | |
| View | ✅ |
| Create | ✅ |
| Edit | ✅ |
| Delete | ✅ |
| **System** | |
| Configuration | ✅ |
| Export Data | ✅ |
| View Audit Logs | ✅ |

---

## Authentication

### Login Mechanism
- **Type**: Form-based authentication
- **Credentials**: Username + Password
- **Session**: Server-side session management
- **Password Storage**: BCrypt hashing with salt

### Security Features (Phase 1)
- ✅ Password complexity requirements
- ✅ Encrypted password storage
- ✅ Session timeout (30 minutes of inactivity)
- ✅ CSRF protection
- ❌ Multi-factor authentication (future)
- ❌ Account lockout (future)
- ❌ Password history (future)
- ❌ Secure password reset (future)

---

## Authorization

### Implementation
- **Framework**: Spring Security
- **Method**: Role-based Access Control (RBAC)
- **Annotation**: `@PreAuthorize("hasRole('ADMIN')")`
- **Enforcement**: Controller and Service layer

### Access Decision
```
User Authentication → Session Creation → Role Check → Resource Access
```

If user has required role → **ALLOW**  
If user lacks required role → **DENY (403 Forbidden)**

---

## Data Access Rules

### Phase 1 (ADMIN)
- **Scope**: All data
- **Filter**: None
- **Restriction**: None

---

## API Security

### Endpoint Protection
All API endpoints are protected by role-based authentication:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/v1/buildings")
public List<Building> getAllBuildings() { ... }
```

### Error Responses
- **401 Unauthorized**: User not authenticated
- **403 Forbidden**: User authenticated but lacks required role
- **404 Not Found**: Resource doesn't exist or user has no access (hide existence)

---

## Session Management

### Session Properties
- **Timeout**: 30 minutes of inactivity
- **Storage**: Server-side session store
- **Cookie**: HTTP-only, Secure flag (HTTPS only)
- **Concurrent Sessions**: 1 session per user (configurable)

### Logout
- **Action**: Invalidate session
- **Effect**: User must re-authenticate
- **URL**: `/logout`

---

## Password Policy

### Requirements (Phase 1)
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (optional)

---

## Audit Trail

### Logged Events
- User login/logout
- Failed login attempts
- User creation/modification/deletion
- Role changes
- Data modifications (via `created_by`, `updated_at`)

### Audit Data Retention
- **Retention Period**: 3 years
- **Storage**: Audit log table (future)
- **Access**: ADMIN role only

---

## Technical Implementation

### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
            );
        return http.build();
    }
}
```

### Password Encoding

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // Strength 10
}
```

---

## Migration Path to OAuth 2.0 / Keycloak (Future)

### Planned Migration
- **From**: Embedded user management
- **To**: External identity provider (Keycloak)
- **Benefits**:
  - Single Sign-On (SSO)
  - Multi-factor Authentication (MFA)
  - Centralized user management
  - Integration with enterprise LDAP/AD
  - Social login (optional)

### Migration Strategy
1. Deploy Keycloak alongside application
2. Migrate existing users to Keycloak
3. Update Spring Security to use OAuth 2.0 / OIDC
4. Maintain role mappings
5. Deprecate embedded authentication

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: Draft for Review