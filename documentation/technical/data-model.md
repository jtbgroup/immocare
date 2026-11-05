---
title: User and Estate Data Model
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# User and Estate Data Model

This page carries forward the user-account, estate, and estate-membership entities from the legacy [data model](../../docs/analysis/data-model.md), verified against the current implementation. Other entities and relationships in that document remain excluded until verified.

## Account, estate, and membership entities

```mermaid
erDiagram
    APP_USER {
        bigint id PK
        string username UK
        string password_hash
        string email UK
        boolean is_platform_admin
        timestamp created_at
        timestamp updated_at
    }
    ESTATE {
        uuid id PK
        string name
        text description
        timestamp created_at
        bigint created_by FK
    }
    ESTATE_MEMBER {
        uuid estate_id PK, FK
        bigint user_id PK, FK
        string role
        timestamp added_at
    }
    APP_USER ||--o{ ESTATE_MEMBER : has
    ESTATE ||--o{ ESTATE_MEMBER : includes
    APP_USER o|--o{ ESTATE : creates
```

`APP_USER`, `ESTATE`, and `ESTATE_MEMBER` are persisted as `app_user`, `estate`, and `estate_member`. An estate membership has the composite key `(estate_id, user_id)`. Deleting a user account cascades to its membership rows; deleting an estate cascades to membership rows, but not all estate-scoped data references use cascade deletion.

The account has a global platform-administrator flag, while estate-specific roles are stored on membership rows. The model does not directly link a user account to a `Person` record, although domain records may store user audit references; whether an account-to-person identity link is needed is an [open question](../../backlog/open_questions.md#user-accounts).

See the [account and estate data dictionary](./data-dictionary.md) for persisted field constraints, [User Account Management](./user-management.md) for account operations, and [Estate Management](./estate-management.md) for estate operations.
