# UC004_ESTATE_PLACEHOLDER — Manage Estates — Phase 1 Generation Prompt

## Context

This prompt generates the **Phase 1** implementation of UC004_ESTATE_PLACEHOLDER — Manage Estates for the ImmoCare application.
It is the first of 6 phases in the multi-tenant estate migration. No existing data migration is required — the
application is starting fresh.

For reference, the full migration plan is:
- **Phase 1 (this prompt)** — Estate CRUD, membership, dashboard skeleton, EstateAccessInterceptor
- Phase 2 — Buildings & Housing Units scoped to estate
- Phase 3 — Persons & Leases scoped to estate
- Phase 4 — Transactions & Financial scoped to estate
- Phase 5 — Per-estate config (boiler rules, platform config)
- Phase 6 — Dashboard enriched, VIEWER enforcement, cross-estate tests

---

## Stack

- **Backend**: Spring Boot 3, Java 21, Spring Data JPA, PostgreSQL, Lombok, MapStruct, Spring Security (session-based)
- **Frontend**: Angular 19+, Angular Material, standalone components, lazy-loaded feature modules, signals
- **Database**: Flyway — new migration `V017__introduce_estates.sql`
- **Package**: `com.immocare`
- **Branch**: `develop`

---

## What Changes in This Phase

### On `app_user`
- Remove column: `role VARCHAR(20)`
- Add column: `is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE`
- The existing `admin` user seed gets `is_platform_admin = TRUE`
- Spring Security: replace `hasRole('ADMIN')` checks with a combination of:
  - `@PreAuthorize("@security.isPlatformAdmin()")` for platform-level endpoints
  - `@PreAuthorize("@security.isManagerOf(#estateId)")` for estate CRUD endpoints
  - `@PreAuthorize("@security.isMemberOf(#estateId)")` for estate read endpoints

### New tables
- `estate`
- `estate_member`

### No changes yet to
- `building`, `housing_unit`, `person`, `financial_transaction`, or any other existing entity
- Existing controllers and services remain untouched in this phase

---

## Flyway Migration: `V017__introduce_estates.sql`

```sql
-- Use case: UC004_ESTATE_PLACEHOLDER

-- 1. Modify app_user: replace role with is_platform_admin
ALTER TABLE app_user DROP COLUMN role;
ALTER TABLE app_user ADD COLUMN is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE app_user SET is_platform_admin = TRUE WHERE username = 'admin';

-- 2. Create estate table (UUID primary key)
CREATE TABLE estate (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by  BIGINT      REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_estate_name_ci ON estate (LOWER(name));

-- 3. Create estate_member table
CREATE TABLE estate_member (
    estate_id  UUID        NOT NULL REFERENCES estate(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'VIEWER')),
    added_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (estate_id, user_id)
);
```

---

## Backend Classes to Generate

### 1. Enum: `EstateRole`
```
MANAGER, VIEWER
```

### 2. Entity: `Estate`
- Table: `estate`
- Fields: `id (UUID)`, `name`, `description`, `createdAt (@PrePersist)`, `createdBy (ManyToOne → AppUser)`
- No `@PreUpdate` (no `updatedAt` column)

### 3. Entity: `EstateMember`
- Table: `estate_member`
- Composite PK via `@IdClass EstateMemberId` (estateId: UUID, userId: Long)
- Fields: `estate (ManyToOne → Estate)`, `user (ManyToOne → AppUser)`, `role (EstateRole enum stored as String)`, `addedAt (@PrePersist)`

### 4. Modified Entity: `AppUser`
- Remove field `role` (and all references to `ROLE_ADMIN`)
- Add field `isPlatformAdmin (boolean)`
- `getAuthorities()` returns `ROLE_PLATFORM_ADMIN` when `isPlatformAdmin = true`, else empty list
- Keep `implements UserDetails`

### 5. DTOs

**`EstateDTO`** (full response)
```
id (UUID), name, description,
memberCount, buildingCount,
createdAt, createdByUsername
```

**`EstateSummaryDTO`** (list / selector)
```
id (UUID), name, description,
myRole (EstateRole, nullable — null for PLATFORM_ADMIN transversal access),
buildingCount, unitCount
```

**`EstateDashboardDTO`**
```
estateId (UUID), estateName,
totalBuildings, totalUnits,
activeLeases,
pendingAlerts: { boiler (int), fireExtinguisher (int), leaseEnd (int), indexation (int) }
```
> In Phase 1, all counts return 0 — they will be populated in later phases.

**`EstateMemberDTO`**
```
userId, username, email, role (EstateRole), addedAt
```

**`CreateEstateRequest`**
```
name*           VARCHAR(100)   required, unique (case-insensitive)
description     TEXT           optional
firstManagerId  Long           optional — if provided, assigns MANAGER role to this user at creation
```

**`UpdateEstateRequest`**
```
name*        VARCHAR(100)
description  TEXT
```

**`AddEstateMemberRequest`**
```
userId*  Long
role*    EstateRole (MANAGER | VIEWER)
```

**`UpdateEstateMemberRoleRequest`**
```
role*  EstateRole (MANAGER | VIEWER)
```

### 6. Repositories

**`EstateRepository`** extends `JpaRepository<Estate, UUID>`:
- `existsByNameIgnoreCase(String name)`
- `existsByNameIgnoreCaseAndIdNot(String name, UUID id)`
- `findAllByOrderByNameAsc(Pageable pageable)`
- `searchByName(String term, Pageable pageable)` — `@Query` ILIKE

**`EstateMemberRepository`** extends `JpaRepository<EstateMember, EstateMemberId>`:
- `findByEstateId(UUID estateId)`
- `findByUserId(Long userId)` — for "my estates"
- `countByEstateId(UUID estateId)`
- `countByEstateIdAndRole(UUID estateId, String role)`
- `existsByEstateIdAndUserId(UUID estateId, Long userId)`
- `findByEstateIdAndUserId(UUID estateId, Long userId)`

### 7. Exceptions

- `EstateNotFoundException`
- `EstateNameTakenException`
- `EstateHasBuildingsException(int buildingCount)`
- `EstateMemberNotFoundException`
- `EstateMemberAlreadyExistsException`
- `EstateLastManagerException`
- `EstateSelfOperationException`
- `EstateAccessDeniedException`

### 8. Security Helper: `EstateSecurityService`

Spring `@Service("security")` — used in `@PreAuthorize` expressions:

```java
public boolean isPlatformAdmin()
public boolean isManagerOf(UUID estateId)
public boolean isMemberOf(UUID estateId)   // MANAGER or VIEWER
public boolean isMemberOf(UUID estateId, EstateRole... roles)
```

Resolves the current user from `SecurityContextHolder`.

### 9. Service: `EstateService`

Methods:
- `getAllEstates(String search, Pageable pageable)` — PLATFORM_ADMIN only
- `getMyEstates(Long currentUserId)` — returns `List<EstateSummaryDTO>` with `myRole`
- `getEstateById(UUID id)` — returns `EstateDTO` (memberCount, buildingCount = 0 in Phase 1)
- `createEstate(CreateEstateRequest req, Long createdByUserId)` — validates name uniqueness, creates estate, optionally assigns firstManager
- `updateEstate(UUID id, UpdateEstateRequest req)` — validates name uniqueness excluding self
- `deleteEstate(UUID id)` — blocks if buildingCount > 0 or memberCount > 1
- `getDashboard(UUID estateId)` — returns `EstateDashboardDTO` with all counts = 0 in Phase 1
- `getMembers(UUID estateId)` — returns `List<EstateMemberDTO>`
- `addMember(UUID estateId, AddEstateMemberRequest req, Long currentUserId)` — validates user exists, not already member
- `updateMemberRole(UUID estateId, Long userId, UpdateEstateMemberRoleRequest req, Long currentUserId)` — validates not self, not last manager
- `removeMember(UUID estateId, Long userId, Long currentUserId)` — validates not self, not last manager

### 10. Controller: `EstateAdminController`

`@RequestMapping("/api/v1/admin/estates")` — PLATFORM_ADMIN only:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | UC003.004 — list all estates |
| POST | `/` | UC003.001 — create estate |
| PUT | `/{id}` | UC003.002 — edit estate |
| DELETE | `/{id}` | UC003.003 — delete estate |

### 11. Controller: `EstateController`

`@RequestMapping("/api/v1/estates")`:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/mine` | any authenticated | UC003.012 — my estates |
| GET | `/{id}/dashboard` | MEMBER | UC003.011 — dashboard |
| GET | `/{id}/members` | MANAGER or PLATFORM_ADMIN | UC003.006 |
| POST | `/{id}/members` | MANAGER or PLATFORM_ADMIN | UC003.007 |
| PATCH | `/{id}/members/{userId}` | MANAGER or PLATFORM_ADMIN | UC003.008 |
| DELETE | `/{id}/members/{userId}` | MANAGER or PLATFORM_ADMIN | UC003.009 |

### 12. Updated `SecurityConfig`

- Replace `hasRole('ADMIN')` with appropriate expressions
- All `/api/v1/admin/**` require `isPlatformAdmin()`
- All `/api/v1/estates/**` require authentication (fine-grained control via `@PreAuthorize` on methods)
- All other existing `/api/v1/**` endpoints: keep `authenticated()` for now (estate scoping comes in later phases)

### 13. GlobalExceptionHandler entries

Add handlers for all new exceptions with appropriate HTTP codes:
- `EstateNotFoundException` → 404
- `EstateNameTakenException` → 409
- `EstateHasBuildingsException` → 409 (include `buildingCount`)
- `EstateMemberNotFoundException` → 404
- `EstateMemberAlreadyExistsException` → 409
- `EstateLastManagerException` → 409
- `EstateSelfOperationException` → 409
- `EstateAccessDeniedException` → 403

---

## Frontend Classes to Generate

### 1. Model: `estate.model.ts`

```typescript
export type EstateRole = 'MANAGER' | 'VIEWER';

export interface Estate {
  id: string;           // UUID
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
  myRole: EstateRole | null;
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
```

### 2. Service: `EstateService`

`HttpClient`-based Angular service with methods:

```typescript
// Admin endpoints
getAllEstates(page, size, search?): Observable<Page<Estate>>
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

### 3. Service: `ActiveEstateService`

Angular service using **signals** to store the currently active estate:

```typescript
@Injectable({ providedIn: 'root' })
export class ActiveEstateService {
  private _activeEstate = signal<EstateSummary | null>(null);

  readonly activeEstate = this._activeEstate.asReadonly();
  readonly activeEstateId = computed(() => this._activeEstate()?.id ?? null);
  readonly isManager = computed(() => this._activeEstate()?.myRole === 'MANAGER');
  readonly isViewer = computed(() => this._activeEstate()?.myRole === 'VIEWER');

  setActiveEstate(estate: EstateSummary): void
  clearActiveEstate(): void
}
```

### 4. Components (standalone, lazy-loaded)

#### `EstateSelectorComponent`
- Route: `/select-estate`
- Shown after login when user has multiple estates
- Lists estates as cards with name, role badge, building count
- Click → sets active estate via `ActiveEstateService`, navigates to `/estates/{id}/dashboard`
- If user has exactly one estate → auto-select and redirect (handled in auth guard)
- If PLATFORM_ADMIN with no estates → redirect to `/admin/estates`

#### `EstateDashboardComponent`
- Route: `/estates/:estateId/dashboard`
- Shows `EstateDashboardDTO` data in summary cards
- Quick-access navigation cards: Buildings, Transactions, Persons, Alerts
- Estate name in header
- In Phase 1: all counts show 0 with a subtle "coming soon" indication

#### `EstateHeaderComponent`
- Displayed in the app shell header
- Shows active estate name
- "Switch Estate" button → navigates to `/select-estate`
- Only shown when an active estate is set

#### `AdminEstateListComponent`
- Route: `/admin/estates`
- Paginated table: name, description, member count, building count, created at
- Search by name
- "Create Estate" button
- Row click → navigate to `/admin/estates/{id}`
- Delete button with confirmation dialog (show error if buildings exist)

#### `AdminEstateFormComponent`
- Route: `/admin/estates/new` and `/admin/estates/:id/edit`
- Reactive form: name (required), description (optional)
- On create: optional "First Manager" user picker (autocomplete on username/email)
- Cancel with unsaved changes → confirmation dialog

#### `EstateMemberListComponent`
- Route: `/estates/:estateId/members`
- Table: username, email, role badge (MANAGER green / VIEWER grey), added at
- "Add Member" button → opens inline form (user picker + role selector)
- Per-row: edit role (dropdown), remove (with confirmation)
- Cannot edit/remove self (buttons disabled with tooltip)
- Cannot remove last manager (button disabled with tooltip)

### 5. Auth Guard: `EstateGuard`

Angular route guard:
- Checks `ActiveEstateService.activeEstate()`
- If null and user has estates → redirect to `/select-estate`
- If null and PLATFORM_ADMIN → redirect to `/admin/estates`
- Checks role requirement (MANAGER vs VIEWER) per route

### 6. HTTP Interceptor: `EstateInterceptor`

> Not needed in Phase 1 since `estateId` is explicit in URLs.
> Will be added in Phase 2 if needed for convenience.

### 7. Routing Updates

```typescript
// App routes additions
{ path: 'select-estate', component: EstateSelectorComponent },
{ path: 'admin/estates', component: AdminEstateListComponent, canActivate: [PlatformAdminGuard] },
{ path: 'admin/estates/new', component: AdminEstateFormComponent, canActivate: [PlatformAdminGuard] },
{ path: 'admin/estates/:id/edit', component: AdminEstateFormComponent, canActivate: [PlatformAdminGuard] },
{ path: 'estates/:estateId/dashboard', component: EstateDashboardComponent, canActivate: [EstateGuard] },
{ path: 'estates/:estateId/members', component: EstateMemberListComponent, canActivate: [EstateGuard] },
```

---

## Business Rules to Enforce

### Backend
- BR-UC004_ESTATE_PLACEHOLDER-01: Estate name unique case-insensitively
- BR-UC004_ESTATE_PLACEHOLDER-02: At least one MANAGER per estate at all times
- BR-UC004_ESTATE_PLACEHOLDER-03: User cannot remove themselves
- BR-UC004_ESTATE_PLACEHOLDER-04: User cannot change their own role
- BR-UC004_ESTATE_PLACEHOLDER-05: PLATFORM_ADMIN accesses all estates without being a member (no `estate_member` row needed)
- BR-UC004_ESTATE_PLACEHOLDER-09: Cannot delete estate with buildings (buildingCount > 0 → 409)

### Frontend
- Disable "Remove" and "Edit Role" buttons on own row (tooltip: "You cannot modify your own membership")
- Disable "Remove" on last MANAGER row (tooltip: "Cannot remove the last manager")
- Show role badge color: MANAGER = green, VIEWER = grey
- Hide Create/Edit/Delete buttons for VIEWER role (check `ActiveEstateService.isManager()`)

---

## What NOT to Generate in This Phase

- Do NOT modify `BuildingController`, `HousingUnitController`, or any other existing controller
- Do NOT add `estate_id` to any existing table other than what is in V017
- Do NOT generate the `EstateInterceptor` for HTTP — not needed yet
- Do NOT populate real data in `EstateDashboardDTO` counts — return 0 for all

---

## Error Response Format

All error responses follow the existing `GlobalExceptionHandler` pattern already in place in the project.
Add the new exception handlers consistently with the existing ones.

---

## Notes

- UUID type in Java: `java.util.UUID`
- UUID type in TypeScript: `string`
- PostgreSQL `gen_random_uuid()` requires the `pgcrypto` extension or PostgreSQL 13+ (already available)
- Use `@org.springframework.data.annotation.Id` carefully — estate PK is UUID, not Long
- `EstateMemberId` implements `Serializable` (required for `@IdClass`)
- All Angular components use `@if` / `@for` syntax (not `*ngIf` / `*ngFor`)
- Use Angular Material components consistently with the rest of the application