# UC003 — Manage Persons

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC003 |
| **Name** | Manage Persons |
| **Actor** | ADMIN |
| **Epic** | Person Management |
| **Flyway** | V002 (`person`) · V016 (`person_bank_account`) |
| **Status** | ✅ Implemented |
| **Branch** | develop |

A person record represents any natural person involved in the real estate portfolio — whether as the owner of a building or housing unit, or as a tenant on a lease. Person records are created and maintained independently of their role; the relationship to buildings, units, and leases is tracked via foreign keys on those entities.

Each person can hold one or more bank accounts (IBANs). These IBANs are used during transaction import to automatically suggest the related lease when a counterparty account matches a known person IBAN (see UC015 — Import Parser Strategies).

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US043 | List Persons | MUST HAVE | 2 |
| US044 | Get Person Details | MUST HAVE | 2 |
| US045 | Create Person | MUST HAVE | 3 |
| US046 | Edit Person | MUST HAVE | 2 |
| US047 | Delete Person | SHOULD HAVE | 2 |
| US048 | Person Picker (Autocomplete) | MUST HAVE | 1 |
| US049 | Assign Person as Owner | MUST HAVE | 3 |
| US030 | Manage Person Bank Accounts (IBAN) | MUST HAVE | 3 |

---

## US043 — List Persons

**As an** ADMIN **I want to** view a paginated list of persons **so that** I can browse the contact registry.

**Acceptance Criteria:**
- AC1: Returns paginated list with: last name, first name, email, GSM, isOwner badge, isTenant badge.
- AC2: Filterable by `search` (partial match on lastName, firstName, email — case-insensitive).
- AC3: Default sort: lastName ASC.
- AC4: Clicking a row navigates to person details.

**Endpoint:** `GET /api/v1/persons?search=&page=&size=&sort=`

---

## US044 — Get Person Details

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

## US045 — Create Person

**As an** ADMIN **I want to** create a person record **so that** I can register owners and tenants.

**Acceptance Criteria:**
- AC1: Required fields: `lastName`, `firstName`.
- AC2: `nationalId`, if provided, must be globally unique (case-insensitive) → HTTP 409 if duplicate.
- AC3: `country` defaults to `Belgium` if not provided.
- AC4: Returns HTTP 201 on success.

**Endpoint:** `POST /api/v1/persons`

---

## US046 — Edit Person

**As an** ADMIN **I want to** update a person's information **so that** I can keep the contact registry accurate.

**Acceptance Criteria:**
- AC1: All fields from US045 can be updated.
- AC2: `nationalId` uniqueness enforced excluding the current person.
- AC3: Returns HTTP 404 if the person does not exist.

**Endpoint:** `PUT /api/v1/persons/{id}`

---

## US047 — Delete Person

**As an** ADMIN **I want to** delete a person **so that** I can remove records created by mistake.

**Acceptance Criteria:**
- AC1: Blocked if person is referenced as owner of any building or housing unit → HTTP 409 with lists of `ownedBuildings` and `ownedUnits`.
- AC2: Blocked if person is an active tenant on any lease → HTTP 409.
- AC3: On deletion, all associated `person_bank_account` records are cascade-deleted (ON DELETE CASCADE).
- AC4: Returns HTTP 204 on success.

**Endpoint:** `DELETE /api/v1/persons/{id}`

---

## US048 — Person Picker (Autocomplete)

**As an** ADMIN **I want to** search for a person by name **so that** I can link them as owner or tenant in a form.

**Acceptance Criteria:**
- AC1: Accepts `q` query parameter (min 2 characters).
- AC2: Returns up to 10 matches ordered by lastName ASC.
- AC3: Returns empty list (not 404) if fewer than 2 characters.
- AC4: Used in: building form (owner picker), housing unit form (owner picker), lease form (tenant picker).

**Endpoint:** `GET /api/v1/persons/picker?q={query}`

---

## US049 — Assign Person as Owner

**As an** ADMIN **I want to** assign a person as owner of a building or housing unit **so that** ownership is tracked with a real person record.

**Acceptance Criteria:**
- AC1: Building and housing unit edit forms each have an "Owner" field with a person picker.
- AC2: Type 2+ chars in picker → suggestions shown (max 10, case-insensitive match on name/email/nationalId, <300ms).
- AC3: Select a person → their name shown in the owner field.
- AC4: Save → building/unit linked to the person via `owner_id` FK.
- AC5: Person details page shows "Owned Buildings" and "Owned Units" sections reflecting all linked entities.
- AC6: Clear owner → `owner_id` = NULL, displayed as "Owner: Not specified."

**Implemented via:** `PUT /api/v1/buildings/{id}` and `PUT /api/v1/units/{id}` (ownerId field).

---

## US030 — Manage Person Bank Accounts (IBAN)

**As an** ADMIN **I want to** register one or more IBANs for a person **so that** transaction imports can automatically suggest the related lease when a counterparty account matches.

**Acceptance Criteria:**
- AC1: Person details page shows a "Bank Accounts (IBAN)" section listing all registered IBANs: IBAN (formatted with spaces), label (or "—"), primary badge (★ Primary).
- AC2: "Add IBAN" button opens an inline form with fields: IBAN* (required), label (optional), Primary checkbox. Save → IBAN normalised (uppercase, spaces removed) and stored. Section refreshed.
- AC3: Duplicate IBAN (same IBAN already registered on any person) → error "This IBAN is already registered."
- AC4: At most one IBAN per person can be flagged as primary. Setting a new primary automatically clears the previous one.
- AC5: Edit button per row → inline form pre-filled. All fields editable. Same uniqueness rules apply.
- AC6: Delete button per row → confirmation dialog → HTTP 204. No cascade restriction on the IBAN itself.
- AC7: During transaction import (UC015), `counterparty_account` is matched case-insensitively against all known IBANs. If a match is found, the system suggests the most suitable lease of the matched person (see Lease Suggestion Algorithm below).

**Endpoints:**
- `GET /api/v1/persons/{personId}/bank-accounts` — HTTP 200
- `POST /api/v1/persons/{personId}/bank-accounts` — HTTP 201
- `PUT /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 200
- `DELETE /api/v1/persons/{personId}/bank-accounts/{id}` — HTTP 204

---

## Lease Suggestion Algorithm

Used by `TransactionImportService` during both preview (`previewFile`) and import (`importFile`).

### Input
- `counterpartyAccount` — raw IBAN from the parsed transaction row (may contain spaces, mixed case)
- `transactionDate` — date of the transaction

### Steps
1. Normalise `counterpartyAccount` (uppercase, strip spaces).
2. Look up `person_bank_account` by IBAN (case-insensitive). If no match → no suggestion.
3. Load all leases where this person is a tenant (`lease_tenant.person_id`), ordered by `start_date DESC`.
4. **Pick best lease** using `pickBestLease(leases, transactionDate)`:
   - First pass: leases whose date range contains `transactionDate` (`start_date ≤ date ≤ end_date`). If multiple → prefer status ACTIVE → then closest `end_date`.
   - Second pass (fallback — historical import): all leases regardless of date. Prefer closest `end_date` to `transactionDate`.
5. Return `SuggestedLeaseDTO` with `leaseId`, `unitId`, `unitNumber`, `buildingId`, `buildingName`, `personId`, `personFullName`.

### Preview vs Import
- **Preview** (`suggestLeaseForPreview`): pure computation, returns a DTO, no entity mutation.
- **Import** (`suggestLease`): sets `suggestedLease`, `housingUnit`, and `building` on the `FinancialTransaction` entity being persisted.

---

## Data Model

### Table: `person` (V002)

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
| `person_id` | BIGINT | NOT NULL, FK → `person(id)` ON DELETE CASCADE |
| `iban` | VARCHAR(50) | NOT NULL, UNIQUE (globally) |
| `label` | VARCHAR(100) | nullable |
| `is_primary` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

**Indexes:**
- `idx_pba_person` ON `person_id`
- `idx_pba_iban` ON `iban`

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
leases:         [{ id, startDate, endDate, status, unitNumber, buildingName }],
bankAccounts:   [{ id, iban, label, primary, createdAt }]
```

### `PersonBankAccountDTO`
```
id, personId, iban, label, primary, createdAt
```

### `SavePersonBankAccountRequest`
```
iban*    VARCHAR(50)   required; normalised before save (uppercase, no spaces)
label    VARCHAR(100)  optional
primary  boolean       default false
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC003-01 | `lastName` and `firstName` are required |
| BR-UC003-02 | `nationalId`, if provided, must be globally unique (case-insensitive) |
| BR-UC003-03 | `country` defaults to `Belgium` when null or blank |
| BR-UC003-04 | Cannot delete a person who is owner of buildings or units, or active tenant on a lease |
| BR-UC003-05 | `isTenant` is `true` when at least one ACTIVE or FINISHED lease references this person as tenant |
| BR-UC003-06 | IBAN stored normalised: uppercased, spaces removed |
| BR-UC003-07 | IBAN must be globally unique across all persons |
| BR-UC003-08 | Only one IBAN per person can be flagged as primary; setting a new primary clears the previous one |
| BR-UC003-09 | `person_bank_account` rows are cascade-deleted when the person is deleted |
| BR-UC003-10 | Lease suggestion loads leases of all statuses (DRAFT / ACTIVE / FINISHED / CANCELLED) to support historical transaction imports |
| BR-UC003-11 | Best lease selection: exact date match preferred; within matches, ACTIVE preferred; fallback to closest end_date |
| BR-UC003-12 | If no lease is found for the matched person, suggestion fields are null (no suggestion, no error) |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Person not found | 404 | `Person not found` |
| Duplicate nationalId | 409 | `This national ID is already assigned to another person` |
| Referenced as owner or tenant on delete | 409 | `PERSON_REFERENCED` + ownedBuildings, ownedUnits, activeLeases |
| Duplicate IBAN | 409 | `IBAN already registered: {iban}` |
| Bank account not found | 404 | `Person bank account not found: {id}` |
| Bank account does not belong to person | 400 | `Account does not belong to person: {personId}` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | `/api/v1/persons` | US043 |
| GET | `/api/v1/persons/{id}` | US044 |
| POST | `/api/v1/persons` | US045 |
| PUT | `/api/v1/persons/{id}` | US046 |
| DELETE | `/api/v1/persons/{id}` | US047 |
| GET | `/api/v1/persons/picker` | US048 |
| GET | `/api/v1/persons/{personId}/bank-accounts` | US030 |
| POST | `/api/v1/persons/{personId}/bank-accounts` | US030 |
| PUT | `/api/v1/persons/{personId}/bank-accounts/{id}` | US030 |
| DELETE | `/api/v1/persons/{personId}/bank-accounts/{id}` | US030 |

---

## Cross-UC Dependencies

| UC | Dependency |
|---|---|
| UC004 | Buildings use `owner_id → person` |
| UC005 | Housing units use `owner_id → person` |
| UC012 | Leases reference persons as tenants via `lease_tenant` |
| UC014 | Import flow calls lease suggestion algorithm (IBAN matching) |
| UC015 | Parser produces `counterpartyAccount`; IBAN lookup triggers lease suggestion |

---

**Last Updated:** 2026-04-12
**Branch:** develop
**Status:** ✅ Implemented