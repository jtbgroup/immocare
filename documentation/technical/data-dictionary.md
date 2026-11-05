---
title: User Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# User Data Dictionary

This page carries forward only the user-account entity from the legacy [data dictionary](../../docs/analysis/data-dictionary.md). Other entities and constraints from that document are excluded until verified against the current implementation.

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
