# ImmoCare — UC004_ESTATE_PLACEHOLDER Manage Estates — Phase 1 Implementation Prompt

I want to implement Use Case UC004_ESTATE_PLACEHOLDER - Manage Estates (Phase 1 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Boilers, Platform Config, Fire Extinguishers, Financial Transactions, Import Parsers
- **Flyway**: last migration is V016. Use **V017** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 1 of 6** of the multi-tenant estate migration. Only the estate infrastructure is introduced here. No existing controllers, services, or entities are modified in this phase — they will be migrated in subsequent phases.

| Phase | Flyway | Scope |
|---|---|---|
| **Phase 1 (this prompt)** | V017 | Estate CRUD, membership, dashboard skeleton, `app_user` migration, `EstateSecurityService` |
| Phase 2 | V018 | `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V019 | `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V020 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| Phase 5 | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

---

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| UC003.001 | Create Estate | MUST HAVE | 3 |
| UC003.002 | Edit Estate | MUST HAVE | 2 |
| UC003.003 | Delete Estate | MUST HAVE | 2 |
| UC003.004 | List All Estates | MUST HAVE | 2 |
| UC003.005 | Assign First Manager to Estate | MUST HAVE | 1 |
| UC003.006 | View Estate Members | MUST HAVE | 1 |
| UC003.007 | Add Member to Estate | MUST HAVE | 2 |
| UC003.008 | Edit Member Role | MUST HAVE | 2 |
| UC003.009 | Remove Member from Estate | MUST HAVE | 2 |
| UC003.010 | Select Active Estate | MUST HAVE | 3 |
| UC003.011 | View Estate Dashboard | MUST HAVE | 2 |
| UC003.012 | View My Estates | MUST HAVE | 1 |
| UC003.013 | Enforce Estate-scoped Data Access | MUST HAVE | 3 |

---

## DATABASE MIGRATION — `V017__introduce_estates.sql`

```sql
-- Use case: UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 1)

-- 1. Modify app_user: replace role column with is_platform_admin boolean
ALTER TABLE app_user DROP COLUMN role;
ALTER TABLE app_user ADD COLUMN is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE app_user SET is_platform_admin = TRUE WHERE username = 'admin';

-- 2. Create estate table (UUID primary key — non-guessable, multi-tenant safe)
CREATE TABLE estate (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_estate_name_ci ON estate (LOWER(name));

-- 3. Create estate_member join table
CREATE TABLE estate_member (
    estate_id  UUID        NOT NULL REFERENCES estate(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'VIEWER')),
    added_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (estate_id, user_id)
);

CREATE INDEX idx_estate_member_estate ON estate_member(estate_id);
CREATE INDEX idx_estate_member_user   ON estate_member(user_id);
```

---

## BACKEND

### Enums

```java
public enum EstateRole { MANAGER, VIEWER }
```

### Entities

**`Estate`** — table `estate`:
- Fields: `id` (UUID, @Id), `name`, `description`, `createdAt` (@PrePersist), `createdBy` (@ManyToOne AppUser, nullable).
- No @PreUpdate (no `updated_at` column).

**`EstateMember`** — table `estate_member`:
- Composite PK via `@IdClass EstateMemberId` — fields: `estateId` (UUID), `userId` (Long). `EstateMemberId` implements `Serializable`.
- Fields: `estate` (@ManyToOne Estate, @Id mapped via `estateId`), `user` (@ManyToOne AppUser, @Id mapped via `userId`), `role` (EstateRole, stored as String), `addedAt` (@PrePersist).
- No @PreUpdate.

**`AppUser`** — modify existing entity:
- Remove field `role` (String) and all references to `ROLE_ADMIN`.
- Add field `isPlatformAdmin` (boolean).
- Update `getAuthorities()`: return `List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))` when `isPlatformAdmin = true`, else `List.of()`.
- Keep `implements UserDetails`.

### DTOs (all Java records)

**`EstateDTO`**:
```java
record EstateDTO(
    UUID id, String name, String description,
    int memberCount, int buildingCount,
    LocalDateTime createdAt, String createdByUsername
) {}
```

**`EstateSummaryDTO`**:
```java
record EstateSummaryDTO(
    UUID id, String name, String description,
    EstateRole myRole,   // null for PLATFORM_ADMIN transversal access
    int buildingCount, int unitCount
) {}
```

**`EstateDashboardDTO`**:
```java
record EstateDashboardDTO(
    UUID estateId, String estateName,
    int totalBuildings, int totalUnits, int activeLeases,
    EstatePendingAlertsDTO pendingAlerts
) {}

record EstatePendingAlertsDTO(
    int boiler, int fireExtinguisher, int leaseEnd, int indexation
) {}
```
> In Phase 1, all counts return 0. They will be populated in Phase 6.

**`EstateMemberDTO`**:
```java
record EstateMemberDTO(
    Long userId, String username, String email,
    EstateRole role, LocalDateTime addedAt
) {}
```

**`CreateEstateRequest`**:
```java
record CreateEstateRequest(
    @NotBlank @Size(max = 100) String name,
    String description,
    Long firstManagerId   // optional
) {}
```

**`UpdateEstateRequest`**:
```java
record UpdateEstateRequest(
    @NotBlank @Size(max = 100) String name,
    String description
) {}
```

**`AddEstateMemberRequest`**:
```java
record AddEstateMemberRequest(
    @NotNull Long userId,
    @NotNull EstateRole role
) {}
```

**`UpdateEstateMemberRoleRequest`**:
```java
record UpdateEstateMemberRoleRequest(
    @NotNull EstateRole role
) {}
```

### Repositories

**`EstateRepository`** extends `JpaRepository<Estate, UUID>`:
```java
boolean existsByNameIgnoreCase(String name);
boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

@Query("SELECT e FROM Estate e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY e.name ASC")
Page<Estate> searchByName(@Param("search") String search, Pageable pageable);

Page<Estate> findAllByOrderByNameAsc(Pageable pageable);
```

**`EstateMemberRepository`** extends `JpaRepository<EstateMember, EstateMemberId>`:
```java
List<EstateMember> findByEstateIdOrderByUserUsernameAsc(UUID estateId);
List<EstateMember> findByUserId(Long userId);
boolean existsByEstateIdAndUserId(UUID estateId, Long userId);
Optional<EstateMember> findByEstateIdAndUserId(UUID estateId, Long userId);
long countByEstateId(UUID estateId);
long countByEstateIdAndRole(UUID estateId, String role);
```

### Exceptions

- `EstateNotFoundException` → 404
- `EstateNameTakenException` → 409
- `EstateHasBuildingsException(int buildingCount)` → 409
- `EstateMemberNotFoundException` → 404
- `EstateMemberAlreadyExistsException` → 409
- `EstateLastManagerException` → 409
- `EstateSelfOperationException` → 409
- `EstateAccessDeniedException` → 403

### Security Helper: `EstateSecurityService`

`@Service("security")` — used in `@PreAuthorize` SpEL expressions:

```java
@Service("security")
public class EstateSecurityService {

    // Returns true if current user has is_platform_admin = true
    public boolean isPlatformAdmin() { ... }

    // Returns true if current user is PLATFORM_ADMIN or has MANAGER role in estateId
    public boolean isManagerOf(UUID estateId) { ... }

    // Returns true if current user is PLATFORM_ADMIN or has any role (MANAGER or VIEWER) in estateId
    public boolean isMemberOf(UUID estateId) { ... }
}
```

All methods resolve the current user from `SecurityContextHolder.getContext().getAuthentication()`.

### Service: `EstateService`

`@Service`, `@Transactional(readOnly = true)`

```java
Page<EstateDTO> getAllEstates(String search, Pageable pageable);
// PLATFORM_ADMIN only — returns all estates with memberCount and buildingCount (0 in Phase 1)

List<EstateSummaryDTO> getMyEstates(Long currentUserId, boolean isPlatformAdmin);
// If isPlatformAdmin: return all estates with myRole = null
// Else: return estates where user is a member, with their role

EstateDTO getEstateById(UUID id);
// memberCount from EstateMemberRepository.countByEstateId
// buildingCount = 0 in Phase 1

EstateDashboardDTO getDashboard(UUID estateId);
// All counts = 0 in Phase 1 — will be enriched in Phase 6

@Transactional
EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId);
// 1. Check name uniqueness → EstateNameTakenException
// 2. Create and save Estate
// 3. If firstManagerId provided: validate user exists → addMember(estateId, firstManagerId, MANAGER)

@Transactional
EstateDTO updateEstate(UUID id, UpdateEstateRequest req);
// Check name uniqueness excluding self → EstateNameTakenException

@Transactional
void deleteEstate(UUID id);
// buildingCount = 0 in Phase 1; check will be activated in Phase 2
// Delete estate (cascade handles estate_member)

List<EstateMemberDTO> getMembers(UUID estateId);

@Transactional
EstateMemberDTO addMember(UUID estateId, AddEstateMemberRequest req, Long currentUserId);
// Validate user exists in app_user
// Check not already member → EstateMemberAlreadyExistsException
// Save EstateMember

@Transactional
EstateMemberDTO updateMemberRole(UUID estateId, Long userId, UpdateEstateMemberRoleRequest req, Long currentUserId);
// Self-check → EstateSelfOperationException
// If demoting from MANAGER: check countByEstateIdAndRole('MANAGER') > 1 → EstateLastManagerException
// Update role

@Transactional
void removeMember(UUID estateId, Long userId, Long currentUserId);
// Self-check → EstateSelfOperationException
// Check member exists → EstateMemberNotFoundException
// If member is MANAGER: check countByEstateIdAndRole('MANAGER') > 1 → EstateLastManagerException
// Delete member
```

### Controllers

**`EstateAdminController`** — `@RequestMapping("/api/v1/admin/estates")`, PLATFORM_ADMIN only:

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| GET | `/` | — | `Page<EstateDTO>` 200 | UC003.004 |
| POST | `/` | `CreateEstateRequest` | `EstateDTO` 201 | UC003.001 |
| PUT | `/{id}` | `UpdateEstateRequest` | `EstateDTO` 200 | UC003.002 |
| DELETE | `/{id}` | — | 204 | UC003.003 |

All methods annotated `@PreAuthorize("@security.isPlatformAdmin()")`.

**`EstateController`** — no `@RequestMapping` prefix; full path per method:

| Method | Path | PreAuthorize | Response | Story |
|--------|------|------|----------|-------|
| GET | `/api/v1/estates/mine` | `isAuthenticated()` | `List<EstateSummaryDTO>` 200 | UC003.012 |
| GET | `/api/v1/estates/{id}/dashboard` | `@security.isMemberOf(#id)` | `EstateDashboardDTO` 200 | UC003.011 |
| GET | `/api/v1/estates/{id}/members` | `@security.isManagerOf(#id)` | `List<EstateMemberDTO>` 200 | UC003.006 |
| POST | `/api/v1/estates/{id}/members` | `@security.isManagerOf(#id)` | `EstateMemberDTO` 201 | UC003.007 |
| PATCH | `/api/v1/estates/{id}/members/{userId}` | `@security.isManagerOf(#id)` | `EstateMemberDTO` 200 | UC003.008 |
| DELETE | `/api/v1/estates/{id}/members/{userId}` | `@security.isManagerOf(#id)` | 204 | UC003.009 |

For `GET /api/v1/estates/mine`: inject `@AuthenticationPrincipal AppUser currentUser` to pass `currentUser.getId()` and `currentUser.isPlatformAdmin()` to the service.

### Updated `SecurityConfig`

- Replace all `hasRole('ADMIN')` with `isAuthenticated()` globally — fine-grained control is now handled per method via `@PreAuthorize` and `EstateSecurityService`.
- Keep existing session config (timeout, concurrent sessions, CSRF disabled).
- Ensure `/api/v1/admin/**` is accessible (fine-grained control handled by `@PreAuthorize`).
- Keep `UserDetailsServiceImpl` loading by username — now checks `isPlatformAdmin` instead of `role`.

### GlobalExceptionHandler — add these handlers

```java
@ExceptionHandler(EstateNotFoundException.class)
// → 404 "Estate not found"

@ExceptionHandler(EstateNameTakenException.class)
// → 409 "An estate with this name already exists"

@ExceptionHandler(EstateHasBuildingsException.class)
// → 409 "Cannot delete estate: {buildingCount} building(s) exist"
// Include buildingCount in response body

@ExceptionHandler(EstateMemberNotFoundException.class)
// → 404 "User is not a member of this estate"

@ExceptionHandler(EstateMemberAlreadyExistsException.class)
// → 409 "This user is already a member of this estate"

@ExceptionHandler(EstateLastManagerException.class)
// → 409 "Cannot remove the last manager of an estate"

@ExceptionHandler(EstateSelfOperationException.class)
// → 409 message from exception

@ExceptionHandler(EstateAccessDeniedException.class)
// → 403 "Access denied to this estate"
```

---

## FRONTEND

### Models — `estate.model.ts`

```typescript
export type EstateRole = 'MANAGER' | 'VIEWER';

export interface Estate {
  id: string;              // UUID as string
  name: string;
  description?: string;
  memberCount: number;
  buildingCount: number;
  createdAt: string;
  createdByUsername?: string;
}

export interface EstateSummary {
  id: string;
  name: string;
  description?: string;
  myRole: EstateRole | null;   // null = PLATFORM_ADMIN transversal
  buildingCount: number;
  unitCount: number;
}

export interface EstateDashboard {
  estateId: string;
  estateName: string;
  totalBuildings: number;
  totalUnits: number;
  activeLeases: number;
  pendingAlerts: {
    boiler: number;
    fireExtinguisher: number;
    leaseEnd: number;
    indexation: number;
  };
}

export interface EstateMember {
  userId: number;
  username: string;
  email: string;
  role: EstateRole;
  addedAt: string;
}

export interface CreateEstateRequest {
  name: string;
  description?: string;
  firstManagerId?: number;
}

export interface UpdateEstateRequest {
  name: string;
  description?: string;
}

export interface AddEstateMemberRequest {
  userId: number;
  role: EstateRole;
}

export interface UpdateEstateMemberRoleRequest {
  role: EstateRole;
}

export const ESTATE_ROLE_LABELS: Record<EstateRole, string> = {
  MANAGER: 'Manager',
  VIEWER: 'Viewer'
};
```

### Service — `estate.service.ts`

```typescript
// Admin endpoints (PLATFORM_ADMIN)
getAllEstates(page: number, size: number, search?: string): Observable<Page<Estate>>
createEstate(req: CreateEstateRequest): Observable<Estate>
updateEstate(id: string, req: UpdateEstateRequest): Observable<Estate>
deleteEstate(id: string): Observable<void>

// User endpoints
getMyEstates(): Observable<EstateSummary[]>
getDashboard(estateId: string): Observable<EstateDashboard>

// Member endpoints
getMembers(estateId: string): Observable<EstateMember[]>
addMember(estateId: string, req: AddEstateMemberRequest): Observable<EstateMember>
updateMemberRole(estateId: string, userId: number, req: UpdateEstateMemberRoleRequest): Observable<EstateMember>
removeMember(estateId: string, userId: number): Observable<void>
```

### Service — `active-estate.service.ts`

Angular service using **signals** to store the active estate context:

```typescript
@Injectable({ providedIn: 'root' })
export class ActiveEstateService {
  private readonly _activeEstate = signal<EstateSummary | null>(null);

  readonly activeEstate  = this._activeEstate.asReadonly();
  readonly activeEstateId = computed(() => this._activeEstate()?.id ?? null);
  readonly isManager      = computed(() => this._activeEstate()?.myRole === 'MANAGER');
  readonly isViewer       = computed(() => this._activeEstate()?.myRole === 'VIEWER');
  readonly isPlatformAdmin = computed(() => this._activeEstate()?.myRole === null);
  readonly canEdit        = computed(() => this.isManager() || this.isPlatformAdmin());

  setActiveEstate(estate: EstateSummary): void { this._activeEstate.set(estate); }
  clearActiveEstate(): void { this._activeEstate.set(null); }
}
```

### Components (all standalone, use @if / @for syntax — never *ngIf / *ngFor)

#### `EstateSelectorComponent`
- Route: `/select-estate`
- Shown after login when user belongs to multiple estates.
- Calls `estateService.getMyEstates()` on init.
- Displays estates as Material cards: estate name (h2), description, role badge (MANAGER green / VIEWER grey), building count.
- Click on a card → `activeEstateService.setActiveEstate(estate)` → navigate to `/estates/{id}/dashboard`.
- If user has exactly one estate → auto-select on init and redirect (no interaction needed).
- If PLATFORM_ADMIN with no estate membership → redirect to `/admin/estates`.
- If user has no estates and is not PLATFORM_ADMIN → show "No estate assigned. Contact your administrator."

#### `EstateDashboardComponent`
- Route: `/estates/:estateId/dashboard`
- Calls `estateService.getDashboard(estateId)` on init.
- Summary cards row: Total Buildings, Total Units, Active Leases, Pending Alerts (sum of all alert types).
- Quick-access navigation cards: Buildings (`/estates/{id}/buildings`), Transactions (`/estates/{id}/transactions`), Persons (`/estates/{id}/persons`), Alerts (`/estates/{id}/alerts`).
- In Phase 1: all counts show 0 — no "coming soon" text, just the number.
- Estate name displayed in page title and in the shared app header via `ActiveEstateService`.

#### `EstateHeaderComponent`
- Displayed in the app shell header alongside existing navigation.
- Shows active estate name from `activeEstateService.activeEstate()?.name`.
- "Switch" button → navigates to `/select-estate`.
- Hidden when no active estate is set (e.g. on admin pages).

#### `AdminEstateListComponent`
- Route: `/admin/estates`
- Paginated Material table: name, description, member count, building count, created at, created by.
- Search input with debounce (300ms).
- "Create Estate" button → navigates to `/admin/estates/new`.
- Row click → navigates to `/admin/estates/:id/edit`.
- Delete button per row → confirmation dialog showing estate name in bold + "This action cannot be undone." Cancel / Delete buttons.
- Error on delete: show snackbar "Cannot delete: {n} building(s) exist."

#### `AdminEstateFormComponent`
- Routes: `/admin/estates/new` and `/admin/estates/:id/edit`.
- Reactive form: `name` (required, maxlength 100), `description` (optional textarea).
- On create only: optional "First Manager" field — autocomplete input searching existing users by username or email (`GET /api/v1/users?search=`). Shows selected username with a clear button.
- "Save" / "Cancel" buttons. Cancel with unsaved changes → confirmation dialog.
- On success: navigate back to `/admin/estates`.

#### `EstateMemberListComponent`
- Route: `/estates/:estateId/members`
- Material table: username, email, role badge, added at.
- "Add Member" button → opens inline panel above the table.
  - Inline panel: user autocomplete (search by username/email via `GET /api/v1/users?search=`), role selector (MANAGER / VIEWER), Save / Cancel.
- Per row:
  - Role dropdown (inline edit) — disabled on own row.
  - Remove button — disabled on own row and on last MANAGER row.
  - Disabled buttons show Angular Material tooltip explaining the reason.
- Changes take effect immediately (optimistic UI not required — reload list after each operation).

### Guards

**`PlatformAdminGuard`** — `CanActivate`:
- Calls `authService.getCurrentUser()`.
- If `isPlatformAdmin = true` → allow.
- Else → redirect to `/`.

**`EstateGuard`** — `CanActivate`:
- Checks `activeEstateService.activeEstate()`.
- If null → call `estateService.getMyEstates()`:
  - 1 estate → auto-set and continue.
  - Multiple estates → redirect to `/select-estate`.
  - 0 estates + PLATFORM_ADMIN → redirect to `/admin/estates`.
  - 0 estates → redirect to `/select-estate` (shows "no estate" message).
- Checks that the `estateId` in the route matches `activeEstateService.activeEstateId()`.
  - Mismatch → redirect to `/select-estate`.

### Routing updates — `app.routes.ts`

```typescript
{ path: 'select-estate',
  component: EstateSelectorComponent },

{ path: 'admin/estates',
  component: AdminEstateListComponent,
  canActivate: [PlatformAdminGuard] },

{ path: 'admin/estates/new',
  component: AdminEstateFormComponent,
  canActivate: [PlatformAdminGuard] },

{ path: 'admin/estates/:id/edit',
  component: AdminEstateFormComponent,
  canActivate: [PlatformAdminGuard] },

{ path: 'estates/:estateId/dashboard',
  component: EstateDashboardComponent,
  canActivate: [EstateGuard] },

{ path: 'estates/:estateId/members',
  component: EstateMemberListComponent,
  canActivate: [EstateGuard] },
```

Update the existing `AuthGuard` post-login redirect:
- After successful login → call `getMyEstates()`:
  - 1 estate → `/estates/{id}/dashboard`
  - Multiple → `/select-estate`
  - 0 + PLATFORM_ADMIN → `/admin/estates`

### Sidebar navigation updates

- Add **Estate** section at the top of the sidebar (above Buildings):
  - "Dashboard" → `/estates/{estateId}/dashboard`
  - "Members" → `/estates/{estateId}/members` (hidden for VIEWER)
- Add **Administration** section at the bottom:
  - "Users" → `/admin/users` (existing)
  - "Estates" → `/admin/estates` (PLATFORM_ADMIN only — hidden otherwise)
- Show active estate name as a subtitle below the app logo.
- "Switch Estate" link in the sidebar footer.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| BR-UC004_ESTATE_PLACEHOLDER-01: Estate name unique case-insensitively | Backend: `existsByNameIgnoreCase` check |
| BR-UC004_ESTATE_PLACEHOLDER-02: At least one MANAGER at all times | Backend: `countByEstateIdAndRole('MANAGER') > 1` before remove/demote |
| BR-UC004_ESTATE_PLACEHOLDER-03: Cannot remove self | Backend: `EstateSelfOperationException`; Frontend: disable button on own row |
| BR-UC004_ESTATE_PLACEHOLDER-04: Cannot change own role | Backend: `EstateSelfOperationException`; Frontend: disable dropdown on own row |
| BR-UC004_ESTATE_PLACEHOLDER-05: PLATFORM_ADMIN accesses all estates without membership | Backend: `EstateSecurityService.isPlatformAdmin()` bypasses membership check |
| BR-UC004_ESTATE_PLACEHOLDER-06: estateId always explicit in URL | Backend: path variable `{id}` on all estate-scoped endpoints |
| BR-UC004_ESTATE_PLACEHOLDER-08: VIEWER cannot mutate | Frontend: `canEdit()` signal hides action buttons |
| BR-UC004_ESTATE_PLACEHOLDER-09: Cannot delete estate with buildings | Backend: buildingCount check (= 0 in Phase 1; activated in Phase 2) |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify `BuildingController`, `HousingUnitController`, or any other existing controller
- Do NOT add `estate_id` to any existing table other than what is in V017
- Do NOT populate real data in `EstateDashboardDTO` counts — all return 0
- Do NOT implement the VIEWER read-only enforcement via aspect — that is Phase 6

---

## ACCEPTANCE CRITERIA

- [ ] `app_user.role` column removed; `app_user.is_platform_admin` column added; existing admin user has `is_platform_admin = TRUE`
- [ ] `estate` and `estate_member` tables created with correct constraints and indexes
- [ ] PLATFORM_ADMIN can create, edit, delete, and list all estates (UC003.001–UC003.004)
- [ ] Creating an estate with `firstManagerId` automatically assigns MANAGER role (UC003.005)
- [ ] Duplicate estate name (case-insensitive) → HTTP 409 (UC003.001 AC3, UC003.002 AC4)
- [ ] Cannot delete estate with buildings → HTTP 409 with buildingCount (UC003.003 AC1) — returns 0 in Phase 1
- [ ] MANAGER can view, add, edit role, and remove members (UC003.006–UC003.009)
- [ ] Cannot remove self or last MANAGER → HTTP 409 (UC003.008 AC2–AC3, UC003.009 AC1–AC2)
- [ ] VIEWER cannot access members list → HTTP 403 (UC003.006 AC3)
- [ ] `GET /api/v1/estates/mine` returns correct estates and roles (UC003.012)
- [ ] Dashboard endpoint returns estate name with all counts = 0 (UC003.011)
- [ ] Estate selector shown on login when user has multiple estates (UC003.010 AC2)
- [ ] Auto-redirect when user has exactly one estate (UC003.010 AC1)
- [ ] `ActiveEstateService` signals update correctly on estate selection and switch (UC003.010)
- [ ] Estate name visible in app header with "Switch" button (UC003.010 AC4)
- [ ] `EstateSecurityService` correctly enforces PLATFORM_ADMIN bypass and membership checks (UC003.013)

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
