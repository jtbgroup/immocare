---
title: Person Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Person Data Dictionary

This page records persisted fields for people and their bank accounts. Account fields are in the [User Account Data Dictionary](../user-accounts-and-authentication/user-account-data-dictionary.md); estate and membership fields are in the [Estate Data Dictionary](../estates-and-access/estate-data-dictionary.md). Relationships are shown in the [User, Estate, Person, and Property Data Model](../cross-cutting/data-model.md).

## Entity: `person`

**Purpose:** Stores an estate-scoped natural person who may be an owner or be associated with leases.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | Primary key, generated identity | No | Generated | Person identifier |
| `last_name` | `VARCHAR(100)` | — | No | — | Last name |
| `first_name` | `VARCHAR(100)` | — | No | — | First name |
| `birth_date` | `DATE` | — | Yes | — | Date of birth |
| `birth_place` | `VARCHAR(100)` | — | Yes | — | Place of birth |
| `national_id` | `VARCHAR(20)` | No unique constraint in the Flyway migration | Yes | — | Optional national identifier |
| `gsm` | `VARCHAR(20)` | — | Yes | — | Telephone number |
| `email` | `VARCHAR(100)` | — | Yes | — | Email address |
| `street_address` | `VARCHAR(200)` | — | Yes | — | Street address |
| `postal_code` | `VARCHAR(20)` | — | Yes | — | Postal code |
| `city` | `VARCHAR(100)` | — | Yes | — | City |
| `country` | `VARCHAR(100)` | — | Yes | `Belgium` | Country |
| `estate_id` | `UUID` | Foreign key to `estate(id)` | No | — | Estate that scopes this person record |
| `created_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as creator |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Person creation time |
| `updated_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as last updater |
| `updated_at` | `TIMESTAMP` | — | No | Current timestamp | Most recent person update time |

## Entity: `person_bank_account`

**Purpose:** Stores an IBAN associated with a person and estate; transaction import can use it to suggest a related lease.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | Primary key, generated identity | No | Generated | Person bank-account identifier |
| `person_id` | `BIGINT` | Foreign key to `person(id)`, `ON DELETE CASCADE` | No | — | Associated person |
| `iban` | `VARCHAR(50)` | Unique with `estate_id` | No | — | IBAN |
| `label` | `VARCHAR(100)` | — | Yes | — | Optional account label |
| `is_primary` | `BOOLEAN` | — | No | `FALSE` | Whether the person has designated this as the primary account |
| `estate_id` | `UUID` | Foreign key to `estate(id)` | No | — | Estate scope for this account record |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Account-record creation time |

The Flyway schema defines the IBAN uniqueness constraint on `(iban, estate_id)`. The JPA entity marks `iban` unique by itself, which does not match that schema declaration. The person entity also marks `national_id` unique in its JPA mapping, while the Flyway migration defines no corresponding unique constraint. Hibernate is configured with `ddl-auto: validate`; application-level duplicate checks are described in [Person Management](./person-management.md) and their canonical statuses are in [Person Rules](../../business/people/rules/persons.md).

## Source

Fields and persisted constraints above are based on Flyway migration `V004__uc004_manage_persons.sql`, with application validation and mappings in `Person`, `PersonBankAccount`, their request DTOs, and services.
