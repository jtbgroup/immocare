---
title: User, Estate, Configuration, Person, Room, Rent, PEB, Equipment, and Property Data Model
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "Room, PebScoreHistory, RentHistory, Lease, LeaseRentAdjustment, Boiler, BoilerServiceRecord, BoilerServiceValidityRule, PlatformConfig, EstateConfig, FireExtinguisher, FireExtinguisherRevision, Meter, V007__uc007_manage_rooms.sql, V008__uc008_manage_peb_scores.sql, V009__uc009_manage_meters.sql, V010__uc010_manage_rents.sql, V011__uc011_manage_boilers.sql, V012__uc012_manage_fire_extinguishers.sql, V013__uc013_manage_config.sql, V014__uc014_manage_leases.sql, V017__generalize_fire_extinguisher_ownership.sql"
---

# User, Estate, Configuration, Person, Room, Rent, PEB, Equipment, and Property Data Model

This page shows the current account, estate, and configuration relationships, together with person, property, room, rent-history, PEB-history, ownership, lease-membership, lease rent-adjustment, and person-bank-account relationships.

## Account, estate, person, room, and property entities

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
    PLATFORM_CONFIG {
        string config_key PK
        string config_value
        string value_type
        text description
        timestamp updated_at
        bigint updated_by FK
    }
    ESTATE_CONFIG {
        uuid estate_id PK, FK
        string config_key PK
        string config_value
        string value_type
        text description
        timestamp updated_at
        bigint updated_by FK
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
    ROOM {
        bigint id PK
        bigint housing_unit_id FK
        string room_type
        decimal approximate_surface
    }
    RENT_HISTORY {
        bigint id PK
        bigint housing_unit_id FK
        decimal monthly_rent
        date effective_from
        date effective_to
    }
    PEB_SCORE_HISTORY {
        bigint id PK
        bigint housing_unit_id FK
        string peb_score
        date score_date
        string certificate_number
        date valid_until
        timestamp created_at
    }
    METER {
        bigint id PK
        string type
        string owner_type
        bigint owner_id
        date start_date
        date end_date
    }
    BOILER {
        bigint id PK
        string owner_type
        bigint owner_id
        string fuel_type
        date installation_date
        date out_of_service_date
    }
    BOILER_SERVICE {
        bigint id PK
        bigint boiler_id FK
        date service_date
        date valid_until
    }
    BOILER_SERVICE_VALIDITY_RULE {
        bigint id PK
        uuid estate_id FK
        date valid_from
        integer validity_duration_months
    }
    FIRE_EXTINGUISHER {
        bigint id PK
        bigint building_id FK
        bigint unit_id FK
        string identification_number
    }
    FIRE_EXTINGUISHER_REVISION {
        bigint id PK
        bigint fire_extinguisher_id FK
        date revision_date
    }
    LEASE {
        bigint id PK
        bigint housing_unit_id FK
    }
    LEASE_RENT_ADJUSTMENT {
        bigint id PK
        bigint lease_id FK
        string field
        decimal old_value
        decimal new_value
        text reason
        date effective_date
    }
    LEASE_TENANT {
        bigint lease_id PK, FK
        bigint person_id PK, FK
        string role
    }
    APP_USER ||--o{ ESTATE_MEMBER : has
    ESTATE ||--o{ ESTATE_MEMBER : includes
    ESTATE ||--o{ ESTATE_CONFIG : configures
    APP_USER o|--o{ ESTATE : creates
    APP_USER o|--o{ PLATFORM_CONFIG : updates
    APP_USER o|--o{ ESTATE_CONFIG : updates
    ESTATE ||--o{ PERSON : scopes
    PERSON ||--o{ PERSON_BANK_ACCOUNT : has
    ESTATE ||--o{ PERSON_BANK_ACCOUNT : scopes
    PERSON o|--o{ BUILDING : owns
    PERSON o|--o{ HOUSING_UNIT : owns
    ESTATE ||--o{ BUILDING : contains
    BUILDING ||--o{ HOUSING_UNIT : contains
    HOUSING_UNIT ||--o{ ROOM : contains
    HOUSING_UNIT ||--o{ RENT_HISTORY : has
    HOUSING_UNIT ||--o{ PEB_SCORE_HISTORY : has
    HOUSING_UNIT ||--o{ LEASE : has
    LEASE ||--o{ LEASE_RENT_ADJUSTMENT : records
    BOILER ||--o{ BOILER_SERVICE : has
    ESTATE ||--o{ BOILER_SERVICE_VALIDITY_RULE : configures
    BUILDING o|--o{ FIRE_EXTINGUISHER : manages
    HOUSING_UNIT o|--o{ FIRE_EXTINGUISHER : manages
    FIRE_EXTINGUISHER ||--o{ FIRE_EXTINGUISHER_REVISION : has
    PERSON ||--o{ LEASE_TENANT : participates
    LEASE ||--o{ LEASE_TENANT : includes
```

`APP_USER`, `ESTATE`, `ESTATE_MEMBER`, `PLATFORM_CONFIG`, `ESTATE_CONFIG`, `PERSON`, `PERSON_BANK_ACCOUNT`, `BUILDING`, `HOUSING_UNIT`, `ROOM`, `RENT_HISTORY`, `PEB_SCORE_HISTORY`, `METER`, `BOILER`, `BOILER_SERVICE`, `BOILER_SERVICE_VALIDITY_RULE`, `FIRE_EXTINGUISHER`, `FIRE_EXTINGUISHER_REVISION`, `LEASE`, `LEASE_TENANT`, and `LEASE_RENT_ADJUSTMENT` are persisted as `app_user`, `estate`, `estate_member`, `platform_config`, `estate_config`, `person`, `person_bank_account`, `building`, `housing_unit`, `room`, `rent_history`, `peb_score_history`, `meter`, `boiler`, `boiler_service`, `boiler_service_validity_rule`, `fire_extinguisher`, `fire_extinguisher_revision`, `lease`, `lease_tenant`, and `lease_rent_adjustment`. `platform_config` is keyed by `config_key`; `estate_config` has the composite key `(estate_id, config_key)`. An estate membership has the composite key `(estate_id, user_id)`. Lease membership has the composite key `(lease_id, person_id)`. Deleting a user account cascades to its membership rows; deleting a person cascades to person bank accounts, while lease membership references restrict person deletion. A room, rent-history record, and PEB-history record each reference one housing unit; the database cascades their deletion when the housing unit is deleted, although the housing-unit service currently blocks deletion if rooms remain. Lease rent adjustments reference one lease and cascade when that lease is deleted.

The domain model does not associate a regulated reference rent directly with a lease; external regulation may govern lease terms without creating that relationship ([BR-LEASE-001](../../business/leases/rules/leases.md)). The current application does not persist regulated reference-rent data.

Platform-wide configuration has no estate relationship; estate key/value configuration belongs to an estate. See [Cross-Cutting Configuration Management](./configuration-management.md). Boiler-service validity rules are a separate dated history documented in [Boiler Management](../equipment/boiler-management.md).

Meters store an `owner_type` and `owner_id` pair to refer to either a building or a housing unit. This polymorphic association is not represented by a foreign key or an entity relationship in the diagram. There is no separate meter-event table: replacement preserves the previous meter as a closed meter record, while readings and maintenance events are not stored. See the [meter data dictionary](../equipment/meter-data-dictionary.md) and [meter management reference](../equipment/meter-management.md).

Boilers use the same polymorphic `owner_type` and `owner_id` pattern, without an owner foreign key. A boiler has many service records; an estate has its own dated boiler-service validity rules. Fire extinguishers reference either a building or a housing unit, and a database check requires exactly one owner context. Their revision records cascade when the extinguisher is deleted; deleting a unit-owned extinguisher is restricted by the housing-unit foreign key. See the [boiler data dictionary](../equipment/boiler-data-dictionary.md), [fire-extinguisher data dictionary](../equipment/fire-extinguisher-data-dictionary.md), and their [management references](../equipment/README.md).

The account has a global platform-administrator flag, while estate-specific roles are stored on membership rows. A platform administrator may also have estate memberships. The model does not directly link a user account to a `Person` record, although domain records may store user audit references; assessing whether such a link would be useful is tracked in the [backlog](../../../backlog/backlog.md).

The [person data dictionary](../people/person-data-dictionary.md) documents the `person` and `person_bank_account` fields and constraints. The [user account data dictionary](../user-accounts-and-authentication/user-account-data-dictionary.md) documents account fields; the [estate data dictionary](../estates-and-access/estate-data-dictionary.md) documents estate and membership fields; the [housing-unit data dictionary](../buildings-and-housing-units/housing-unit-data-dictionary.md) documents housing-unit, room, PEB-history, and rent-history fields; and the [equipment data dictionaries](../equipment/README.md) document meter, boiler, and fire-extinguisher fields. See [Person Management](../people/person-management.md) for person operations, [Building Management](../buildings-and-housing-units/building-management.md) for building operations, [Housing Unit Management](../buildings-and-housing-units/housing-unit-management.md) for housing-unit operations, [Room Management](../buildings-and-housing-units/room-management.md) for room operations, [PEB Score Management](../buildings-and-housing-units/peb-score-management.md) for PEB rating and certificate behavior, [Housing-Unit Rent History Management](../buildings-and-housing-units/rent-history-management.md) for housing-unit rent history, [Lease Technical References](../leases/README.md) for lease rent and charge adjustments, [Boiler Management](../equipment/boiler-management.md), [Fire Extinguisher Management](../equipment/fire-extinguisher-management.md), [Meter Management](../equipment/meter-management.md), [User Account Management](../user-accounts-and-authentication/user-management.md), and [Estate Management](../estates-and-access/estate-management.md).
