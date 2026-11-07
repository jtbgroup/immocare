---
title: User, Estate, Person, and Property Data Model
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# User, Estate, Person, and Property Data Model

This page shows the current account and estate relationships, together with person, property, ownership, lease-membership, and person-bank-account relationships.

## Account, estate, person, and property entities

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
    PERSON {
        bigint id PK
        uuid estate_id FK
        bigint created_by FK
        bigint updated_by FK
    }
    PERSON_BANK_ACCOUNT {
        bigint id PK
        bigint person_id FK
        uuid estate_id FK
        string iban
        boolean is_primary
    }
    BUILDING {
        bigint id PK
        uuid estate_id FK
        bigint owner_id FK
    }
    HOUSING_UNIT {
        bigint id PK
        bigint building_id FK
        bigint owner_id FK
    }
    LEASE {
        bigint id PK
        bigint housing_unit_id FK
    }
    LEASE_TENANT {
        bigint lease_id PK, FK
        bigint person_id PK, FK
        string role
    }
    APP_USER ||--o{ ESTATE_MEMBER : has
    ESTATE ||--o{ ESTATE_MEMBER : includes
    APP_USER o|--o{ ESTATE : creates
    ESTATE ||--o{ PERSON : scopes
    PERSON ||--o{ PERSON_BANK_ACCOUNT : has
    ESTATE ||--o{ PERSON_BANK_ACCOUNT : scopes
    PERSON o|--o{ BUILDING : owns
    PERSON o|--o{ HOUSING_UNIT : owns
    ESTATE ||--o{ BUILDING : contains
    BUILDING ||--o{ HOUSING_UNIT : contains
    HOUSING_UNIT ||--o{ LEASE : has
    PERSON ||--o{ LEASE_TENANT : participates
    LEASE ||--o{ LEASE_TENANT : includes
```

`APP_USER`, `ESTATE`, `ESTATE_MEMBER`, `PERSON`, `PERSON_BANK_ACCOUNT`, `BUILDING`, `HOUSING_UNIT`, and `LEASE_TENANT` are persisted as `app_user`, `estate`, `estate_member`, `person`, `person_bank_account`, `building`, `housing_unit`, and `lease_tenant`. An estate membership has the composite key `(estate_id, user_id)`. Lease membership has the composite key `(lease_id, person_id)`. Deleting a user account cascades to its membership rows; deleting a person cascades to person bank accounts, while lease membership references restrict person deletion.

The account has a global platform-administrator flag, while estate-specific roles are stored on membership rows. A platform administrator may also have estate memberships. The model does not directly link a user account to a `Person` record, although domain records may store user audit references; assessing whether such a link would be useful is tracked in the [backlog](../../../backlog/backlog.md).

The [person data dictionary](../people/person-data-dictionary.md) documents the `person` and `person_bank_account` fields and constraints. The [user account data dictionary](../user-accounts-and-authentication/user-account-data-dictionary.md) documents account fields; the [estate data dictionary](../estates-and-access/estate-data-dictionary.md) documents estate and membership fields. See [Person Management](../people/person-management.md) for person operations, [Building Management](../buildings-and-housing-units/building-management.md) for building operations, [User Account Management](../user-accounts-and-authentication/user-management.md) for account operations, and [Estate Management](../estates-and-access/estate-management.md) for estate operations.
