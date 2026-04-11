# UC006 — Manage Persons

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC006 |
| **Name** | Manage Persons |
| **Actor** | ADMIN |
| **Epic** | Person Management |
| **Flyway** | V006 (person table — baseline) · V016 (person_bank_account) |
| **Status** | ✅ Implemented |
| **Branch** | develop |

A person record represents any natural person involved in the real estate portfolio — whether as the owner of a building or housing unit, or as a tenant on a lease. Person records are created and maintained independently of their role; the relationship to buildings, units, and leases is tracked via foreign keys on those entities.

Each person can hold one or more bank accounts (IBANs). These IBANs are used during transaction import to automatically suggest the related lease when a counterparty account matches a known person IBAN (see UC015 — Import Parser Strategies).

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US024 | List Persons | MUST HAVE | 2 |
| US025 | Get Person Details | MUST HAVE | 2 |
| US026 | Create Person | MUST HAVE | 3 |
| US027 | Edit Person | MUST HAVE | 2 |
| US028 | Delete Person | SHOULD HAVE | 2 |
| US029 | Person Picker (Autocomplete) | MUST HAVE | 1 |
| US030 | Manage Person Bank Accounts (IBAN) | MUST HAVE | 3 |

---

## US024 — List Persons

**As an** ADMIN **I want to** view a paginated list of persons **so that** I can browse the contact registry.

**Acceptance Criteria:**
- AC1: Returns paginated list with: last name, first name, email, GSM, isOwner badge, isTenant badge.
- AC2: Filterable by `search` (partial match on lastName, firstName, email — case-insensitive).
- AC3: Default sort: lastName ASC.
- AC4: Clicking a row navigates to person details.

**Endpoint:** `GET /api/v1/persons?search=&page=&size=&sort=`

---

## US025 — Get Person Details

**As an** ADMIN **I want to** view the full details of a person **so that** I can see all their relationships and registered IBANs.

**Acceptance Criteria:**
- AC1: Returns all personal fields.
- AC2: Returns `ownedBuildings` (buildings where this person is owner).
- AC3: Returns `ownedUnits` (housing units where this person is direct owner).
- AC4: Returns `leases` (active and past leases as tenant).
- AC5: Returns `bankAccounts` (list of IBANs registered for this person).
- AC6: Returns computed flags `isOwner` and `isTenant`.

**Endpoint:** `GET /api/v1/persons/{id}`

---

## US026 — Create Person

**As an** ADMIN **I want to** create a person record **so that** I can register owners and tenants.

**Acceptance Criteria:**
- AC1: Required fields: `lastName`, `firstName`.
- AC2: `nationalId`, if provided, must be globally unique (case-insensitive) → HTTP 409 if duplicate.
- AC3: `country` defaults to `Belgium` if not provided.
- AC4: Returns HTTP 201 on success.

**Endpoint:** `POST /api/v1/persons`

---

## US027 — Edit Person

**As an** ADMIN **I want to** update a person's information **so that** I can keep the contact registry accurate.

**Acceptance Criteria:**
- AC1: All fields from US026 can be updated.
- AC2: `nationalId` uniqueness enforced excluding the current person.
- AC3: Returns HTTP 404 if the person does not exist.

**Endpoint:** `PUT /api/v1/persons/{id}`

---

## US028 — Delete Person

**As an** ADMIN **I want to** delete a person **so that** I can remove records created by mistake.

**Acceptance Criteria:**
- AC1: Blocked if person is referenced as owner of any building or housing unit → HTTP 409 with lists of `ownedBuildings` and `ownedUnits`.
- AC2: Blocked if person is an active tenant on any lease → HTTP 409.
- AC3: On deletion, all associated `person_bank_account` records are cascade-deleted (ON DELETE CASCADE).
- AC4: Returns HTTP 204 on success.

**Endpoint:** `DELETE /api/v1/persons/{id}`

---

## US029 — Person Picker (Autocomplete)

**As an** ADMIN **I want to** search for a person by name **so that** I can link them as owner or tenant in a form.

**Acceptance Criteria:**
- AC1: Accepts `q` query parameter (min 2 characters).
- AC2: Returns up to 10 matches ordered by lastName ASC.
- AC3: Returns empty list (not 404) if fewer than 2 characters.
- AC4: Used in: building form (owner picker), housing unit form (owner picker), lease form (tenant picker).

**Endpoint:** `GET /api/v1/persons/picker?q={query}`

---

## US030 — Manage Person Bank Accounts (IBAN)

**As an** ADMIN **I want to** register one or more IBANs for a person **so that** transaction imports can automatically suggest the related lease when a counterparty account matches.

**Acceptance Criteria:**
- AC1: Person details page shows a "Bank Accounts (IBAN)" section listing all registered IBANs: IBAN (formatted), label (optional), primary flag.
- AC2: "Add IBAN" opens an inline form: IBAN* (required, valid format, globally unique), label (optional), primary (checkbox).
- AC3: IBAN stored normalised: uppercased, spaces removed.
- AC4: Duplicate IBAN across any person → error "This IBAN is already registered."
- AC5: At most one IBAN per person can be flagged as primary. Setting a new primary automatically clears the previous one.
- AC6: Edit button → inline form pre-filled. All fields editable.
- AC7: Delete button → confirmation dialog → HTTP 204. No cascade restriction.
- AC8: IBANs are matched (case-insensitive) against `counterparty_account` on imported transactions to auto-suggest the lease linked to this person (see UC015).

**Endpoints:**
- `GET /api/v1/persons/{personId}/bank-accounts` — HTTP 200
- `POST /api/v1/persons/{personId}/bank-accounts` — HTTP 201
- `PUT /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 200
- `DELETE /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 204

---

## Data Model

### Table: `person` (V006)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `last_name` | VARCHAR(100) | NOT NULL |
| `first_name` | VARCHAR(100) | NOT NULL |
| `birth_date` | DATE | nullable |
| `birth_place` | VARCHAR(100) | nullable |
| `national_id` | VARCHAR(20) | nullable, UNIQUE |
| `gsm` | VARCHAR(20) | nullable |
| `email` | VARCHAR(100) | nullable |
| `street_address` | VARCHAR(200) | nullable |
| `postal_code` | VARCHAR(20) | nullable |
| `city` | VARCHAR(100) | nullable |
| `country` | VARCHAR(100) | nullable, DEFAULT `'Belgium'` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### Table: `person_bank_account` (V016)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `person_id` | BIGINT | NOT NULL, FK → person(id) ON DELETE CASCADE |
| `iban` | VARCHAR(50) | NOT NULL, UNIQUE |
| `label` | VARCHAR(100) | nullable |
| `is_primary` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `PersonSummaryDTO` (list / picker)
```
id, lastName, firstName, email, gsm, isOwner, isTenant
```

### `PersonDTO` (detail)
```
id, lastName, firstName, birthDate, birthPlace, nationalId,
gsm, email, streetAddress, postalCode, city, country,
createdAt, updatedAt,
isOwner, isTenant,
ownedBuildings: [{ id, name, city }],
ownedUnits:     [{ id, unitNumber, buildingId, buildingName }],
leases:         [{ id, reference, startDate, endDate, status, unitNumber, buildingName }],
bankAccounts:   [{ id, iban, label, primary, createdAt }]
```

### `PersonBankAccountDTO`
```
id, personId, iban, label, primary, createdAt
```

### `SavePersonBankAccountRequest`
```
iban*    VARCHAR(50)   required, normalized before save
label    VARCHAR(100)  optional
primary  boolean       default false
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC006-01 | `lastName` and `firstName` are required |
| BR-UC006-02 | `nationalId`, if provided, must be globally unique (case-insensitive) |
| BR-UC006-03 | `country` defaults to `Belgium` when null or blank |
| BR-UC006-04 | Cannot delete a person who is owner of buildings or units, or active tenant on a lease |
| BR-UC006-05 | `isTenant` is `true` when at least one ACTIVE or FINISHED lease references this person as tenant |
| BR-UC006-06 | IBAN stored normalised: uppercased, spaces removed |
| BR-UC006-07 | IBAN must be globally unique across all persons |
| BR-UC006-08 | Only one IBAN per person can be flagged as primary; setting a new primary clears the previous one |
| BR-UC006-09 | `person_bank_account` rows are cascade-deleted when the person is deleted |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Person not found | 404 | `Person not found` |
| Duplicate nationalId | 409 | `This national ID is already assigned to another person` |
| Referenced as owner or tenant on delete | 409 | `PERSON_REFERENCED` + ownedBuildings, ownedUnits, activeLeases |
| Duplicate IBAN | 409 | `This IBAN is already registered` |
| Bank account not found | 404 | `Person bank account not found` |
| Bank account does not belong to person | 400 | `Account does not belong to person` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | `/api/v1/persons` | US024 |
| GET | `/api/v1/persons/{id}` | US025 |
| POST | `/api/v1/persons` | US026 |
| PUT | `/api/v1/persons/{id}` | US027 |
| DELETE | `/api/v1/persons/{id}` | US028 |
| GET | `/api/v1/persons/picker` | US029 |
| GET | `/api/v1/persons/{personId}/bank-accounts` | US030 |
| POST | `/api/v1/persons/{personId}/bank-accounts` | US030 |
| PUT | `/api/v1/persons/{personId}/bank-accounts/{id}` | US030 |
| DELETE | `/api/v1/persons/{personId}/bank-accounts/{id}` | US030 |

---

**Last Updated:** 2026-03-10
**Branch:** develop
**Status:** ✅ Implemented