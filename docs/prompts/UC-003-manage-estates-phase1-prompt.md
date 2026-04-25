# ImmoCare — UC003 Manage Estates — Phase 1 Implementation Prompt

I want to implement UC003 - Manage Estates (Phase 1 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Boilers, Platform Config, Fire Extinguishers, Financial Transactions, Import Parsers
- **Flyway**: last migration is V002. Use **V003** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 1 of 6** of the multi-tenant estate migration. Only the estate infrastructure is introduced here.

| Phase | Flyway | Scope |
|---|---|---|
| **Phase 1 (this prompt)** | V003 | Estate CRUD, membership, dashboard skeleton, `app_user` migration, `EstateSecurityService` |
| Phase 2 | V004 | `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V005 | `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V006 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| Phase 5 | V007 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

---

## USER STORIES

| Story | Title | Priority |
|-------|-------|----------|
| UC003.001 | Create Estate (with members list) | MUST HAVE |
| UC003.002 | Edit Estate (MANAGER + PLATFORM_ADMIN) | MUST HAVE |
| UC003.003 | Delete Estate | MUST HAVE |
| UC003.004 | List All Estates | MUST HAVE |
| UC003.005 | Assign Members at Estate Creation | MUST HAVE |
| UC003.006 | View Estate Members | MUST HAVE |
| UC003.007 | Add Member to Estate | MUST HAVE |
| UC003.008 | Edit Member Role | MUST HAVE |
| UC003.009 | Remove Member from Estate | MUST HAVE |
| UC003.010 | Select Active Estate | MUST HAVE |
| UC003.011 | View Estate Dashboard (with Edit button) | MUST HAVE |
| UC003.012 | View My Estates | MUST HAVE |
| UC003.013 | Enforce Estate-scoped Data Access | MUST HAVE |

---

## DATABASE MIGRATION — `V003__uc003_manage_estates.sql`

```sql
-- Use case: UC003 — Manage Estates (Phase 1)

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

**`EstateMember`** — table `estate_member`:
- Composite PK via `@IdClass EstateMemberId` — fields: `estateId` (UUID), `userId` (Long).
- Fields: `estate` (@ManyToOne Estate), `user` (@ManyToOne AppUser), `role` (EstateRole, stored as String), `addedAt` (@PrePersist).

**`AppUser`** — modify existing entity:
- Remove field `role` (String).
- Add field `isPlatformAdmin` (boolean).
- Update `getAuthorities()`: return `ROLE_PLATFORM_ADMIN` when `isPlatformAdmin = true`, else empty list.

### DTOs (all Java records, in `EstateDTOs.java`)

**`EstateMemberInput`** (new — used in CreateEstateRequest):
```java
record EstateMemberInput(
    @NotNull Long userId,
    @NotNull EstateRole role
) {}
```

**`CreateEstateRequest`** — replaces the old `firstManagerId` field:
```java
record CreateEstateRequest(
    @NotBlank @Size(max = 100) String name,
    String description,
    @Valid List<EstateMemberInput> members   // optional; if non-empty → ≥1 MANAGER required
) {}
```

**`UpdateEstateRequest`**, **`EstateDTO`**, **`EstateSummaryDTO`**, **`EstateDashboardDTO`**,
**`EstatePendingAlertsDTO`**, **`EstateMemberDTO`**, **`AddEstateMemberRequest`**,
**`UpdateEstateMemberRoleRequest`** — unchanged from original design.

### Exceptions

- `EstateNotFoundException` → 404
- `EstateNameTakenException` → 409
- `EstateHasBuildingsException(int buildingCount)` → 409
- `EstateMemberNotFoundException` → 404
- `EstateMemberAlreadyExistsException` → 409
- `EstateLastManagerException` → 409
- `EstateSelfOperationException` → 409
- `EstateAccessDeniedException` → 403
- `NoManagerInMembersException` → 422 ("The members list must contain at least one entry with role MANAGER")

### Security Helper: `EstateSecurityService`

`@Service("security")` — same as original design. No changes.

### Service: `EstateService`

All methods same as original design with these changes in `createEstate()`:

```java
@Transactional
EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId) {
    // 1. Name uniqueness check → EstateNameTakenException
    // 2. BR-UC003-02: if members non-empty, verify ≥1 MANAGER → NoManagerInMembersException
    // 3. Save estate
    // 4. Seed default config + default boiler validity rule (Phase 5 compatibility)
    // 5. Bulk-add members (deduplicate by userId, validate each exists → UserNotFoundException)
}
```

Add `updateEstate(UUID id, UpdateEstateRequest req)` — shared logic used by both controllers.

### Controllers

**`EstateAdminController`** — `@RequestMapping("/api/v1/admin/estates")`, PLATFORM_ADMIN only:

| Method | Path | Story |
|--------|------|-------|
| GET | `/` | UC003.004 |
| GET | `/{id}` | — |
| POST | `/` | UC003.001 |
| PUT | `/{id}` | UC003.002 (admin route) |
| DELETE | `/{id}` | UC003.003 |

**`EstateController`** — no class-level `@RequestMapping`:

| Method | Path | PreAuthorize | Story |
|--------|------|------|-------|
| GET | `/api/v1/estates/mine` | `isAuthenticated()` | UC003.012 |
| GET | `/api/v1/estates/{id}/dashboard` | `@security.isMemberOf(#id)` | UC003.011 |
| PUT | `/api/v1/estates/{id}` | `@security.isManagerOf(#id)` | UC003.002 (manager route) |
| GET | `/api/v1/estates/{id}/members` | `@security.isManagerOf(#id)` | UC003.006 |
| POST | `/api/v1/estates/{id}/members` | `@security.isManagerOf(#id)` | UC003.007 |
| PATCH | `/api/v1/estates/{id}/members/{userId}` | `@security.isManagerOf(#id)` | UC003.008 |
| DELETE | `/api/v1/estates/{id}/members/{userId}` | `@security.isManagerOf(#id)` | UC003.009 |

### GlobalExceptionHandler — add these handlers

```java
@ExceptionHandler(EstateNotFoundException.class)          // 404
@ExceptionHandler(EstateNameTakenException.class)         // 409
@ExceptionHandler(EstateHasBuildingsException.class)      // 409 + buildingCount in body
@ExceptionHandler(EstateMemberNotFoundException.class)    // 404
@ExceptionHandler(EstateMemberAlreadyExistsException.class) // 409
@ExceptionHandler(EstateLastManagerException.class)       // 409
@ExceptionHandler(EstateSelfOperationException.class)     // 409
@ExceptionHandler(EstateAccessDeniedException.class)      // 403
@ExceptionHandler(NoManagerInMembersException.class)      // 422
```

---

## FRONTEND

### Models — `estate.model.ts`

Add `EstateMemberInput` interface:
```typescript
export interface EstateMemberInput {
  userId: number;
  role: EstateRole;
}
```

Update `CreateEstateRequest`:
```typescript
export interface CreateEstateRequest {
  name: string;
  description?: string;
  members?: EstateMemberInput[];   // replaces firstManagerId
}
```

All other models unchanged.

### Service — `estate.service.ts`

Update `createEstate()` signature to use new `CreateEstateRequest`.

Add `updateEstate()` to call `PUT /api/v1/estates/{id}` (estate-scoped endpoint,
works for both MANAGER and PLATFORM_ADMIN since isPlatformAdmin bypasses the membership check):

```typescript
updateEstate(id: string, req: UpdateEstateRequest): Observable<Estate> {
  return this.http.put<Estate>(`/api/v1/estates/${id}`, req);
}
```

### Service — `active-estate.service.ts`

No changes. `canEdit()` computed signal already returns `isManager() || isPlatformAdmin()`.

### Components

#### `AdminEstateFormComponent` (updated)

- **Create mode**: Replace the single "First Manager" picker with a **Members panel**:
  - Searchable user picker (search by username/email, debounced 300ms).
  - Role selector (MANAGER / VIEWER) beside the picker.
  - "+ Add" button adds the user to a local `pendingMembers` list (not yet persisted).
  - Pending list shows each member with their role (editable via inline dropdown) and a remove button.
  - Warning displayed when members are present but none has role MANAGER.
  - On submit: `members` array sent in `CreateEstateRequest`.

- **Edit mode**: Two-section layout via tab navigation:
  - **General Info** tab: name + description form (same as before).
  - **Members** tab: full member management (same UX as `EstateMemberListComponent`):
    - Live table of current members.
    - Inline "Add Member" panel.
    - Per-row role edit and remove with confirmation.
    - All BR-UC003-02/03/04 constraints enforced (last manager, self-operation).

- Route param resolution: read `id` OR `estateId` from route params (supports both
  `/admin/estates/:id/edit` and `/estates/:estateId/edit`).

#### `EstateDashboardComponent` (updated)

- Add **"Edit Estate" button** in the dashboard header, visible to MANAGER and PLATFORM_ADMIN.
- `canManageEstate` getter: `activeEstateService.isManager() || activeEstateService.isPlatformAdmin()`.
- `goToEstateEdit()` method:
  - PLATFORM_ADMIN → navigates to `/admin/estates/:id/edit`
  - MANAGER → navigates to `/estates/:estateId/edit`

#### All other components unchanged.

### Routing updates — `app.routes.ts`

Add the estate-manager edit route:
```typescript
{ path: 'estates/:estateId/edit',
  component: AdminEstateFormComponent,
  canActivate: [EstateGuard],
  data: { requiresManager: true } },
```

Existing routes unchanged:
```typescript
{ path: 'admin/estates/:id/edit',
  component: AdminEstateFormComponent,
  canActivate: [PlatformAdminGuard] },
```

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| BR-UC003-01: Estate name unique case-insensitively | Backend: `existsByNameIgnoreCase` |
| BR-UC003-02: ≥1 MANAGER in members list at creation | Backend: `NoManagerInMembersException`; Frontend: warning + block submit |
| BR-UC003-02: Last manager cannot be removed or demoted | Backend: `EstateLastManagerException`; Frontend: disable button |
| BR-UC003-03: Cannot remove self | Backend + Frontend |
| BR-UC003-04: Cannot change own role | Backend + Frontend |
| BR-UC003-05: PLATFORM_ADMIN accesses all estates without membership | Backend: `EstateSecurityService` |
| BR-UC003-09: Cannot delete estate with buildings | Backend |
| BR-UC003-14: MANAGER can edit estate metadata | Backend: `PUT /api/v1/estates/{id}` with `isManagerOf` |
| BR-UC003-15: Duplicate userIds in members list are deduplicated | Backend: silent dedup |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify any existing controller other than what is listed above
- Do NOT add `estate_id` to any existing table other than what is in V003
- Do NOT populate real data in `EstateDashboardDTO` counts — all return 0 (Phase 6)
- Do NOT implement VIEWER read-only enforcement via aspect — that is Phase 6

---

## ACCEPTANCE CRITERIA

- [ ] `app_user.role` removed; `app_user.is_platform_admin` added; existing admin has `is_platform_admin = TRUE`
- [ ] `estate` and `estate_member` tables created with correct constraints and indexes
- [ ] PLATFORM_ADMIN can create, edit, delete, and list all estates (UC003.001–UC003.004)
- [ ] Creating an estate with a `members` list atomically assigns all roles (UC003.005)
- [ ] Creating with a non-empty members list but no MANAGER → HTTP 422 (UC003.005 AC3)
- [ ] Creating with an unknown `userId` in members → HTTP 404 (UC003.005 AC2)
- [ ] Duplicate estate name → HTTP 409 (UC003.001 AC3, UC003.002 AC4)
- [ ] Cannot delete estate with buildings → HTTP 409 with buildingCount (UC003.003 AC1)
- [ ] MANAGER can edit estate name/description via `PUT /api/v1/estates/{id}` (UC003.002 AC7)
- [ ] MANAGER and PLATFORM_ADMIN see "Edit Estate" button on dashboard (UC003.011 AC5)
- [ ] VIEWER does not see "Edit Estate" button (UC003.011 AC3)
- [ ] Edit form Members tab allows adding, editing role, and removing members (UC003.002 AC5–AC6)
- [ ] Last manager cannot be removed or demoted → HTTP 409 (UC003.008, UC003.009)
- [ ] Cannot remove self or change own role → HTTP 409 (UC003.008, UC003.009)
- [ ] `GET /api/v1/estates/mine` returns correct estates and roles (UC003.012)
- [ ] Dashboard endpoint returns estate name with all counts = 0 in Phase 1 (UC003.011)
- [ ] `EstateSecurityService` correctly enforces PLATFORM_ADMIN bypass and membership checks (UC003.013)

**Last Updated:** 2026-04-24 | **Branch:** `develop` | **Status:** ✅ Ready for Implementation
