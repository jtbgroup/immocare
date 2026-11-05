---
title: Account and Estate Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Account and Estate Data Dictionary

This page carries forward the account, estate, and estate-membership entities from the legacy [data dictionary](../../docs/analysis/data-dictionary.md), with fields verified against the current schema. Other entities and constraints from that document remain excluded until verified against the current implementation.

## Entity: `app_user`

**Purpose:** Stores the identity and account-level access status used for authentication.

| Attribute           | Data type      | Persisted constraints           | Nullable | Default           | Description                                               |
| ------------------- | -------------- | ------------------------------- | -------- | ----------------- | --------------------------------------------------------- |
| `id`                | `BIGINT`       | Primary key, generated identity | No       | Generated         | Account identifier                                        |
| `username`          | `VARCHAR(50)`  | Unique                          | No       | —                 | Login name                                                |
| `password_hash`     | `VARCHAR(255)` | —                               | No       | —                 | BCrypt-encoded password; excluded from user API responses |
| `email`             | `VARCHAR(100)` | Unique                          | No       | —                 | Account email address                                     |
| `is_platform_admin` | `BOOLEAN`      | —                               | No       | `FALSE`           | Global platform-administrator status                      |
| `created_at`        | `TIMESTAMP`    | —                               | No       | Current timestamp | Account creation time                                     |
| `updated_at`        | `TIMESTAMP`    | —                               | No       | Current timestamp | Most recent account update time                           |

## Validation and encoding references

Application-level account validation is defined once in the [user account rules](../business/rules/users.md): see BR-USER-001 and BR-USER-003 through BR-USER-008. The unique database constraints shown above apply to stored values and are distinct from the application's case-insensitive duplicate checks.

The application encodes passwords with BCrypt strength 12 before storage. This is an implementation detail; the password acceptance policy is defined by BR-USER-003 and BR-USER-006.

The entity fields and database defaults are defined by `AppUser` and migration `V001__uc001_authentication.sql`; request validation and uniqueness checks are defined by the user request DTOs and `UserService`. See [User Account Management](./user-management.md) for API behavior and [Authentication](./authentication.md) for sign-in and session behavior.

## Entity: `estate`

**Purpose:** Defines an estate scope for property-management data.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `UUID` | Primary key | No | Generated UUID | Estate identifier |
| `name` | `VARCHAR(100)` | Unique index on `LOWER(name)` | No | — | Estate name |
| `description` | `TEXT` | — | Yes | — | Optional description |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Creation time |
| `created_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | User account that created the estate |

## Entity: `estate_member`

**Purpose:** Associates a user account with an estate and its estate-scoped access role.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `estate_id` | `UUID` | Part of primary key; foreign key to `estate(id)`, `ON DELETE CASCADE` | No | — | Estate in which the user has membership |
| `user_id` | `BIGINT` | Part of primary key; foreign key to `app_user(id)`, `ON DELETE CASCADE` | No | — | Member user account |
| `role` | `VARCHAR(20)` | Check constraint: `MANAGER` or `VIEWER` | No | — | Estate-scoped access role |
| `added_at` | `TIMESTAMP` | — | No | Current timestamp | Membership creation time |

The composite primary key `(estate_id, user_id)` permits at most one membership for a given user in an estate. Application-level access and lifecycle constraints are defined in the [estate rules](../business/rules/estate.md); endpoint and deletion behavior is described in [Estate Management](./estate-management.md).
