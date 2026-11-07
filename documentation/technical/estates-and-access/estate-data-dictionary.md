---
title: Estate Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Estate Data Dictionary

This page documents persisted estate and membership fields. The account, estate, and membership relationships are shown in the cross-cutting [data model](../cross-cutting/data-model.md).

## Entity: `estate`

**Purpose:** Defines an estate scope for property-management data.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `UUID` | Primary key | No | Generated UUID | Estate identifier |
| `name` | `VARCHAR(100)` | Unique index on `LOWER(name)` | No | — | Estate name |
| `description` | `TEXT` | — | Yes | — | Optional description |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Estate creation time |
| `created_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | User account that created the estate |

## Entity: `estate_member`

**Purpose:** Associates a user account with an estate and its estate-scoped access role.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `estate_id` | `UUID` | Part of primary key; foreign key to `estate(id)`, `ON DELETE CASCADE` | No | — | Estate in which the user has membership |
| `user_id` | `BIGINT` | Part of primary key; foreign key to `app_user(id)`, `ON DELETE CASCADE` | No | — | Member user account |
| `role` | `VARCHAR(20)` | Check constraint: `MANAGER` or `VIEWER` | No | — | Estate-scoped access role |
| `added_at` | `TIMESTAMP` | — | No | Current timestamp | Membership creation time |

The composite primary key `(estate_id, user_id)` permits at most one membership for a given user in an estate. Application-level access and lifecycle constraints are defined in the [estate rules](../../business/estates-and-access/rules/estate.md); endpoint and deletion behavior is described in [Estate Management](./estate-management.md).

The persisted fields and constraints above are based on `Estate`, `EstateMember`, and migration `V003__uc003_manage_estates.sql`.
