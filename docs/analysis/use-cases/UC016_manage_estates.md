# UC016 — Manage Estates

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC016 |
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
| [US092](../user-stories/US092-create-estate.md) | Create Estate |
| [US093](../user-stories/US093-edit-estate.md) | Edit Estate |
| [US094](../user-stories/US094-delete-estate.md) | Delete Estate |
| [US095](../user-stories/US095-list-all-estates.md) | List All Estates |
| [US096](../user-stories/US096-assign-first-manager.md) | Assign First Manager to Estate |
| [US097](../user-stories/US097-view-estate-members.md) | View Estate Members |
| [US098](../user-stories/US098-add-member.md) | Add Member to Estate |
| [US099](../user-stories/US099-edit-member-role.md) | Edit Member Role |
| [US100](../user-stories/US100-remove-member.md) | Remove Member from Estate |
| [US101](../user-stories/US101-select-active-estate.md) | Select Active Estate |
| [US102](../user-stories/US102-view-estate-dashboard.md) | View Estate Dashboard |
| [US103](../user-stories/US103-view-my-estates.md) | View My Estates |
| [US104](../user-stories/US104-enforce-estate-scoped-access.md) | Enforce Estate-scoped Data Access |

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
| BR-UC016-01 | Estate `name` must be unique case-insensitively across the platform |
| BR-UC016-02 | An estate must always have at least one MANAGER |
| BR-UC016-03 | A user cannot remove themselves from an estate |
| BR-UC016-04 | A user cannot change their own role |
| BR-UC016-05 | PLATFORM_ADMIN accesses all estates without being a member |
| BR-UC016-06 | `estateId` is always explicit in the URL path — never stored in server-side session |
| BR-UC016-07 | All estate-scoped queries are filtered by `estate_id` — never bypassed |
| BR-UC016-08 | A VIEWER cannot create, edit, or delete any resource within an estate |
| BR-UC016-09 | Cannot delete an estate that contains buildings |
| BR-UC016-10 | `import_parser` is global — no `estate_id`, managed by PLATFORM_ADMIN only |
| BR-UC016-11 | `boiler_service_validity_rule` and `platform_config` are per-estate (scoped in later phases) |
| BR-UC016-12 | A user can be MANAGER in multiple estates simultaneously |
| BR-UC016-13 | PLATFORM_ADMIN flag and estate membership roles are independent and cumulative |

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
| GET | `/api/v1/admin/estates` | PLATFORM_ADMIN | US095 |
| POST | `/api/v1/admin/estates` | PLATFORM_ADMIN | US092 |
| PUT | `/api/v1/admin/estates/{id}` | PLATFORM_ADMIN | US093 |
| DELETE | `/api/v1/admin/estates/{id}` | PLATFORM_ADMIN | US094 |
| GET | `/api/v1/estates/mine` | All authenticated | US103 |
| GET | `/api/v1/estates/{id}/dashboard` | MEMBER, PLATFORM_ADMIN | US102 |
| GET | `/api/v1/estates/{id}/members` | MANAGER, PLATFORM_ADMIN | US097 |
| POST | `/api/v1/estates/{id}/members` | MANAGER, PLATFORM_ADMIN | US098 |
| PATCH | `/api/v1/estates/{id}/members/{userId}` | MANAGER, PLATFORM_ADMIN | US099 |
| DELETE | `/api/v1/estates/{id}/members/{userId}` | MANAGER, PLATFORM_ADMIN | US100 |

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
