# UC016 — Person Bank Accounts

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC016 |
| **Name** | Person Bank Accounts |
| **Actor** | ADMIN |
| **Epic** | Person Management / Financial Management |
| **Flyway** | V016 |
| **Status** | ✅ Implemented |
| **Branch** | develop |

A person (tenant or owner) can hold one or more bank accounts identified by their IBAN. These IBANs are used during transaction import (UC015) to automatically suggest the lease linked to the person: when a `counterparty_account` on an imported row matches a known IBAN, the service looks up the person, finds their most relevant active lease, and pre-fills the suggestion on the preview row.

This UC covers the data model, CRUD API, and the lease suggestion algorithm. The UI for managing IBANs is embedded in the person details page (UC006 US030).

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US030 | Manage Person Bank Accounts (IBAN) | MUST HAVE | 3 |

> US030 is documented in full in UC006 — Manage Persons. It is listed here for cross-reference.

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

## Data Model (V016)

### Table: `person_bank_account`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `person_id` | BIGINT | NOT NULL, FK → person(id) ON DELETE CASCADE |
| `iban` | VARCHAR(50) | NOT NULL, UNIQUE (globally) |
| `label` | VARCHAR(100) | nullable — free text note (e.g. "Personal account", "Joint account with partner") |
| `is_primary` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

**Indexes:**
- `idx_pba_person` ON `person_id`
- `idx_pba_iban` ON `iban`

---

## DTOs

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

## Service — `PersonBankAccountService`

```
getByPerson(personId): List<PersonBankAccountDTO>
create(personId, req):  PersonBankAccountDTO
  → normalise IBAN
  → check global uniqueness
  → save; if primary → clearPrimaryExcept(personId, newId)
update(personId, id, req): PersonBankAccountDTO
  → verify account belongs to person
  → normalise IBAN; check uniqueness excluding self
  → save; if primary → clearPrimaryExcept(personId, id)
delete(personId, id): void
  → verify account belongs to person
  → delete
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC016-01 | IBAN stored normalised: uppercased, spaces removed |
| BR-UC016-02 | IBAN must be globally unique across all persons |
| BR-UC016-03 | Only one IBAN per person can be flagged as primary; setting a new primary clears the previous one |
| BR-UC016-04 | `person_bank_account` rows are cascade-deleted when the person is deleted |
| BR-UC016-05 | Lease suggestion loads leases of all statuses (DRAFT / ACTIVE / FINISHED / CANCELLED) to support historical transaction imports |
| BR-UC016-06 | Best lease selection: exact date match preferred; within matches, ACTIVE preferred; fallback to closest end_date |
| BR-UC016-07 | If no lease is found for the matched person, suggestion fields are null (no suggestion, no error) |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Person not found | 404 | `Person not found` |
| Bank account not found | 404 | `Person bank account not found: {id}` |
| Account does not belong to person | 400 | `Account does not belong to person: {personId}` |
| Duplicate IBAN | 409 | `IBAN already registered: {iban}` |

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/persons/{personId}/bank-accounts` | List IBANs for a person |
| POST | `/api/v1/persons/{personId}/bank-accounts` | Add an IBAN |
| PUT | `/api/v1/persons/{personId}/bank-accounts/{id}` | Update an IBAN |
| DELETE | `/api/v1/persons/{personId}/bank-accounts/{id}` | Delete an IBAN |

---

## Cross-UC Dependencies

| UC | Dependency |
|---|---|
| UC006 | US030 — UI for managing IBANs is in person details |
| UC014 | Import flow calls lease suggestion algorithm |
| UC015 | Parser produces `counterpartyAccount`; fingerprint triggers IBAN lookup |

---

**Last Updated:** 2026-03-10
**Branch:** develop
**Status:** ✅ Implemented
