# UC004_ESTATE_PLACEHOLDER — Manage Estates

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC004_ESTATE_PLACEHOLDER |
| **Name** | Manage Estates |
| **Actors** | PLATFORM_ADMIN, MANAGER, VIEWER |
| **Epic** | Multi-tenant Estate Management |
| **Flyway** | V017 |
| **Status** | 📋 Ready for Implementation |
| **Branch** | develop |

An **Estate** is the root container for all business data in ImmoCare. Every building, housing unit, person, transaction, lease, meter, and configuration entry belongs to exactly one estate. A user can be a member of multiple estates with independent roles. The platform itself (parsers) remains global and is managed exclusively by the PLATFORM_ADMIN.

---

## Actors

| Actor | Description |
|---|---|
| **PLATFORM_ADMIN** | Global platform administrator. Boolean flag on `app_user`. Can create estates, assign first managers, and access all estates. Manages global config (parsers). Does not need to be a member of an estate to access it. |
| **MANAGER** | Estate-scoped role. Full CRUD access to all data within the estate. Can manage estate membership. |
| **VIEWER** | Estate-scoped role. Read-only access to all data within the estate. Cannot create, edit, or delete anything. |

A user can cumulate roles freely: `PLATFORM_ADMIN` globally, `MANAGER` in estate A, `MANAGER` in estate B, `VIEWER` in estate C — any combination is valid.

---

## User Story Index

| Story | Title |
|---|---|
| [UC003.001](../user-stories/UC003.001-create-estate.md) | Create Estate |
| [UC003.002](../user-stories/UC003.002-edit-estate.md) | Edit Estate |
| [UC003.003](../user-stories/UC003.003-delete-estate.md) | Delete Estate |
| [UC003.004](../user-stories/UC003.004-list-all-estates.md) | List All Estates |
| [UC003.005](../user-stories/UC003.005-assign-first-manager.md) | Assign First Manager to Estate |
| [UC003.006](../user-stories/UC003.006-view-estate-members.md) | View Estate Members |
| [UC003.007](../user-stories/UC003.007-add-member.md) | Add Member to Estate |
| [UC003.008](../user-stories/UC003.008-edit-member-role.md) | Edit Member Role |
| [UC003.009](../user-stories/UC003.009-remove-member.md) | Remove Member from Estate |
| [UC003.010](../user-stories/UC003.010-select-active-estate.md) | Select Active Estate |
| [UC003.011](../user-stories/UC003.011-view-estate-dashboard.md) | View Estate Dashboard |
| [UC003.012](../user-stories/UC003.012-view-my-estates.md) | View My Estates |
| [UC003.013](../user-stories/UC003.013-enforce-estate-scoped-access.md) | Enforce Estate-scoped Data Access |

---

## Data Model

### Table: `estate`

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() |
| `name` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | nullable |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `created_by` | BIGINT | FK → `app_user(id)` ON DELETE SET NULL, nullable |

**Index:** `UNIQUE INDEX idx_estate_name_ci ON estate (LOWER(name))`

### Table: `estate_member`

| Column | Type | Constraints |
|---|---|---|
| `estate_id` | UUID | NOT NULL, FK → `estate(id)` ON DELETE CASCADE |
| `user_id` | BIGINT | NOT NULL, FK → `app_user(id)` ON DELETE CASCADE |
| `role` | VARCHAR(20) | NOT NULL, CHECK IN ('MANAGER', 'VIEWER') |
| `added_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| PRIMARY KEY | `(estate_id, user_id)` | |

### Modified: `app_user`

| Change | Detail |
|---|---|
| Remove | `role VARCHAR(20)` |
| Add | `is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE` |

---

## DTOs

### `EstateDTO` (full response)
```
id (UUID), name, description,
memberCount, buildingCount,
createdAt, createdByUsername
```

### `EstateSummaryDTO` (list / selector)
```
id (UUID), name, description,
myRole (EstateRole — null for PLATFORM_ADMIN transversal access),
buildingCount, unitCount
```

### `EstateDashboardDTO`
```
estateId (UUID), estateName,
totalBuildings, totalUnits, activeLeases,
pendingAlerts: {
  boiler (int), fireExtinguisher (int),
  leaseEnd (int), indexation (int)
}
```

### `EstateMemberDTO`
```
userId, username, email, role (EstateRole), addedAt
```

### `CreateEstateRequest`
```
name*           VARCHAR(100)   required, unique (case-insensitive)
description     TEXT           optional
firstManagerId  Long           optional
```

### `UpdateEstateRequest`
```
name*        VARCHAR(100)
description  TEXT
```

### `AddEstateMemberRequest`
```
userId*  Long
role*    EstateRole (MANAGER | VIEWER)
```

### `UpdateEstateMemberRoleRequest`
```
role*  EstateRole (MANAGER | VIEWER)
```

---

## Enums

### `EstateRole`
`MANAGER`, `VIEWER`

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC004_ESTATE_PLACEHOLDER-01 | Estate `name` must be unique case-insensitively across the platform |
| BR-UC004_ESTATE_PLACEHOLDER-02 | An estate must always have at least one MANAGER |
| BR-UC004_ESTATE_PLACEHOLDER-03 | A user cannot remove themselves from an estate |
| BR-UC004_ESTATE_PLACEHOLDER-04 | A user cannot change their own role |
| BR-UC004_ESTATE_PLACEHOLDER-05 | PLATFORM_ADMIN accesses all estates without being a member |
| BR-UC004_ESTATE_PLACEHOLDER-06 | `estateId` is always explicit in the URL path — never stored in server-side session |
| BR-UC004_ESTATE_PLACEHOLDER-07 | All estate-scoped queries are filtered by `estate_id` — never bypassed |
| BR-UC004_ESTATE_PLACEHOLDER-08 | A VIEWER cannot create, edit, or delete any resource within an estate |
| BR-UC004_ESTATE_PLACEHOLDER-09 | Cannot delete an estate that contains buildings |
| BR-UC004_ESTATE_PLACEHOLDER-10 | `import_parser` is global — no `estate_id`, managed by PLATFORM_ADMIN only |
| BR-UC004_ESTATE_PLACEHOLDER-11 | `boiler_service_validity_rule` and `platform_config` are per-estate (scoped in later phases) |
| BR-UC004_ESTATE_PLACEHOLDER-12 | A user can be MANAGER in multiple estates simultaneously |
| BR-UC004_ESTATE_PLACEHOLDER-13 | PLATFORM_ADMIN flag and estate membership roles are independent and cumulative |

---

## Security Matrix

| Resource | PLATFORM_ADMIN | MANAGER | VIEWER |
|---|---|---|---|
| Create / Edit / Delete estate | ✅ | ❌ | ❌ |
| List all estates | ✅ | ❌ | ❌ |
| View own estates | ✅ | ✅ | ✅ |
| View estate dashboard | ✅ | ✅ | ✅ |
| Manage members | ✅ | ✅ | ❌ |
| View members list | ✅ | ✅ | ❌ |
| CRUD buildings / units / persons / etc. | ✅ | ✅ | ❌ |
| Read buildings / units / persons / etc. | ✅ | ✅ | ✅ |
| Global config (parsers) | ✅ | ❌ | ❌ |
| Per-estate config | ✅ | ✅ | ❌ |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Estate not found | 404 | `Estate not found` |
| Duplicate estate name | 409 | `An estate with this name already exists` |
| Estate has buildings on delete | 409 | `Cannot delete estate: {n} building(s) exist` |
| Member not found | 404 | `User is not a member of this estate` |
| User already member | 409 | `This user is already a member of this estate` |
| Last manager removal or demotion | 409 | `Cannot remove the last manager of an estate` |
| Self-removal | 409 | `You cannot remove yourself from an estate` |
| Self-role change | 409 | `You cannot change your own role` |
| Access to wrong estate | 403 | `Access denied to this estate` |
| firstManagerId user not found | 409 | `User not found` |

---

## API Endpoints

| Method | Endpoint | Actor | Story |
|---|---|---|---|
| GET | `/api/v1/admin/estates` | PLATFORM_ADMIN | UC003.004 |
| POST | `/api/v1/admin/estates` | PLATFORM_ADMIN | UC003.001 |
| PUT | `/api/v1/admin/estates/{id}` | PLATFORM_ADMIN | UC003.002 |
| DELETE | `/api/v1/admin/estates/{id}` | PLATFORM_ADMIN | UC003.003 |
| GET | `/api/v1/estates/mine` | All authenticated | UC003.012 |
| GET | `/api/v1/estates/{id}/dashboard` | MEMBER, PLATFORM_ADMIN | UC003.011 |
| GET | `/api/v1/estates/{id}/members` | MANAGER, PLATFORM_ADMIN | UC003.006 |
| POST | `/api/v1/estates/{id}/members` | MANAGER, PLATFORM_ADMIN | UC003.007 |
| PATCH | `/api/v1/estates/{id}/members/{userId}` | MANAGER, PLATFORM_ADMIN | UC003.008 |
| DELETE | `/api/v1/estates/{id}/members/{userId}` | MANAGER, PLATFORM_ADMIN | UC003.009 |

---

## Estate-scope Cascade Table

| Entity | Scoped via |
|---|---|
| `building` | `estate_id` direct FK (Phase 2) |
| `housing_unit` | `building.estate_id` |
| `room` | `housing_unit → building.estate_id` |
| `lease` | `housing_unit → building.estate_id` |
| `meter` | `owner_id + owner_type → building.estate_id` |
| `peb_score_history` | `housing_unit → building.estate_id` |
| `rent_history` | `housing_unit → building.estate_id` |
| `boiler` | `housing_unit → building.estate_id` |
| `boiler_service` | `boiler → housing_unit → building.estate_id` |
| `fire_extinguisher` | `building.estate_id` |
| `person` | `estate_id` direct FK (Phase 3) |
| `financial_transaction` | `estate_id` direct FK (Phase 4) |
| `bank_account` (catalog) | `estate_id` direct FK (Phase 4) |
| `tag_category` | `estate_id` direct FK (Phase 4) |
| `tag_subcategory` | `tag_category.estate_id` |
| `boiler_service_validity_rule` | `estate_id` direct FK (Phase 5) |
| `platform_config` | `estate_id` direct FK (Phase 5) |
| `import_parser` | **Global** — no `estate_id` |

---

## Migration Plan

| Phase | Flyway | Scope |
|---|---|---|
| **Phase 1** | V017 | Estate CRUD, membership, dashboard skeleton, `app_user` migration |
| **Phase 2** | V018 | `estate_id` on `building`; Buildings & Housing Units scoped |
| **Phase 3** | V019 | `estate_id` on `person`; Persons & Leases scoped |
| **Phase 4** | V020 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| **Phase 5** | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| **Phase 6** | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

---

**Last Updated:** 2026-04-12
**Status:** 📋 Ready for Implementation
