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
- ✅ Secure password reset (future)
- ❌ Multi-factor authentication (future - Phase 2)
- ❌ Account lockout (future)
- ❌ Password history (future)

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

## Future Roles (Backlog - Phase 2+)

### PROPERTY_MANAGER
**Purpose**: Manage properties but not users or system config

**Potential Capabilities**:
- Full access to buildings, units, rooms
- Manage PEB scores, rents, meters
- View tenants and leases (future feature)
- Create maintenance requests (future feature)
- Generate reports
- ❌ Cannot manage users
- ❌ Cannot access system configuration

---

### ACCOUNTANT
**Purpose**: Financial oversight and reporting

**Potential Capabilities**:
- View all properties and units (read-only)
- Full access to rent history
- View payment records (future feature)
- Generate financial reports
- Export financial data
- ❌ Cannot modify property data
- ❌ Cannot manage users

---

### MAINTENANCE_STAFF
**Purpose**: Handle maintenance requests and work orders

**Potential Capabilities**:
- View buildings and units (read-only)
- View maintenance requests (future feature)
- Update maintenance status (future feature)
- Add maintenance notes
- ❌ Cannot modify property details
- ❌ Cannot access financial data
- ❌ Cannot manage users

---

### TENANT (PORTAL)
**Purpose**: Self-service tenant portal (future)

**Potential Capabilities**:
- View own unit details (read-only)
- View own lease information
- Submit maintenance requests
- View payment history
- Update contact information
- ❌ Cannot view other units
- ❌ Cannot access financial details beyond own payments

---

### VIEWER
**Purpose**: Read-only access for stakeholders

**Potential Capabilities**:
- View all properties and units (read-only)
- View reports
- Export data (limited)
- ❌ Cannot create, edit, or delete anything

---

## Role Evolution Strategy

### Phase 1 (Current)
- **Single Role**: ADMIN
- **Focus**: Core functionality
- **Users**: Internal property managers only

### Phase 2 (Planned)
- **Add Roles**: PROPERTY_MANAGER, ACCOUNTANT
- **Focus**: Role segregation for larger teams
- **Authorization Model**: Expand Spring Security configuration

### Phase 3 (Future)
- **Add Roles**: MAINTENANCE_STAFF, VIEWER
- **Focus**: Operational roles
- **Fine-grained Permissions**: Resource-level permissions

### Phase 4 (Future)
- **Add Role**: TENANT (Portal)
- **Focus**: Customer-facing portal
- **Multi-tenant**: Data isolation by tenant
- **Self-registration**: Tenant invitation system

---

## Permission Inheritance

**Future Consideration**: Role hierarchy for permission inheritance

```
ADMIN
  └─ PROPERTY_MANAGER
       ├─ ACCOUNTANT
       └─ MAINTENANCE_STAFF
            └─ VIEWER
```

Higher roles inherit permissions from lower roles.

---

## Data Access Rules

### Phase 1 (ADMIN)
- **Scope**: All data
- **Filter**: None
- **Restriction**: None

### Future Phases
- **Property Manager**: All properties assigned to them
- **Accountant**: All properties (read-only)
- **Maintenance Staff**: Only units with active maintenance requests
- **Tenant**: Only their own unit
- **Viewer**: All properties (read-only)

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

### Future Enhancements (Phase 2+)
- Password history (prevent reuse of last 5 passwords)
- Password expiration (90 days)
- Account lockout after 5 failed attempts
- Password complexity scoring

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

## Migration Path to OAuth 2.0 / Keycloak (Phase 2+)

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
**Next Review**: Before Phase 2 implementation
