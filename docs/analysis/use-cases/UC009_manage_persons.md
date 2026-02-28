# UC009 — Manage Persons

## Overview

| Attribute | Value |
|---|---|
| ID | UC009 |
| Name | Manage Persons |
| Actor | Admin |
| Module | Persons |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to create, read, update, and delete persons. A person can be an owner of buildings or housing units, and a tenant in leases. The detail view aggregates all related buildings, units, and leases. A quick-search "picker" endpoint supports autocomplete in forms.

> **Known limitation:** `isTenant` and `leases` are stubs (always `false` / empty list) pending full lease-person integration. The delete check only validates ownership; active lease tenant check is stubbed.

---

## User Stories

### US043 — View Persons List

**As an** admin, **I want to** view a paginated list of persons **so that** I can find and manage owners and tenants.

**Acceptance Criteria:**
- AC1: Paginated list shows: Last Name, First Name, Email, GSM, City, role badges (Owner / Tenant).
- AC2: Sorted by lastName ASC by default.
- AC3: Search by `search` parameter (partial match on name, email, nationalId — case-insensitive).
- AC4: Empty state shows "No persons yet" message and "Add Person" button.
- AC5: A blue "Owner" badge shown when `isOwner = true`; a green "Tenant" badge when `isTenant = true`.

**Endpoint:** `GET /api/v1/persons?search=&page=&size=&sort=`

---

### US044 — Create Person

**As an** admin, **I want to** create a new person record **so that** I can assign them as owner or tenant.

**Acceptance Criteria:**
- AC1: Required fields: `lastName`, `firstName`.
- AC2: `nationalId`, if provided, must be unique (case-insensitive). Returns HTTP 409 if duplicate.
- AC3: `country` defaults to `Belgium` if not provided.
- AC4: Returns HTTP 201 on success with full person details.

**Endpoint:** `POST /api/v1/persons`

---

### US045 — Edit Person

**As an** admin, **I want to** update a person's information **so that** I can keep the contact registry accurate.

**Acceptance Criteria:**
- AC1: All fields from US044 can be updated.
- AC2: `nationalId` uniqueness is enforced excluding the current person.
- AC3: Person details page shows all info: `ownedBuildings`, `ownedUnits`, `isOwner`, `isTenant` (stub: false), `leases` (stub: []).
- AC4: Returns HTTP 404 if the person does not exist.

**Endpoints:**
- `GET /api/v1/persons/{id}` — details
- `PUT /api/v1/persons/{id}` — update

---

### US046 — Delete Person

**As an** admin, **I want to** delete a person **so that** I can remove obsolete contacts.

**Acceptance Criteria:**
- AC1: Deletion is blocked if the person is referenced as owner of any building or housing unit.
- AC2: Error response includes lists of `ownedBuildings` and `ownedUnits`.
- AC3: Returns HTTP 204 on successful deletion.

**Endpoint:** `DELETE /api/v1/persons/{id}`

---

### US047 — Assign Person as Owner

**As an** admin, **I want to** assign a person as owner of a building or housing unit **so that** ownership is formally tracked.

**Acceptance Criteria:**
- AC1: When editing a building or unit, the owner field uses the person picker (US048).
- AC2: Selecting a person sets `ownerId` on the building or unit.
- AC3: Clearing the owner field sets `ownerId = null`.
- AC4: The building/unit details page shows the owner's full name.

**Note:** Implemented via US002 (Edit Building) and US007 (Edit Housing Unit) — no dedicated endpoint.

---

### US048 — Person Picker (Search)

**As an** admin, **I want to** search for a person by name **so that** I can link them as owner or tenant in a form.

**Acceptance Criteria:**
- AC1: Accepts a `q` query parameter (min 2 characters).
- AC2: Returns up to 10 matches ordered by lastName ASC.
- AC3: Returns an empty list (not 404) if fewer than 2 characters.
- AC4: Picker must respond in < 300ms for good UX.

**Endpoint:** `GET /api/v1/persons/picker?q={query}`

---

## Data Model

### Table: `person`

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

---

## DTOs

### `PersonSummaryDTO` (list/picker response)
```
id, lastName, firstName, email, gsm, isOwner, isTenant
```

### `PersonDTO` (detail response)
```
id, lastName, firstName, birthDate, birthPlace, nationalId,
gsm, email, streetAddress, postalCode, city, country,
createdAt, updatedAt,
isOwner, isTenant,
ownedBuildings: [{ id, name, city }]
ownedUnits: [{ id, unitNumber, buildingId, buildingName }]
leases: []   ← stub
```

### `CreatePersonRequest` (POST body)
```
lastName*       VARCHAR(100)
firstName*      VARCHAR(100)
birthDate       LocalDate
birthPlace      VARCHAR(100)
nationalId      VARCHAR(20)   unique
gsm             VARCHAR(20)
email           VARCHAR(100)
streetAddress   VARCHAR(200)
postalCode      VARCHAR(20)
city            VARCHAR(100)
country         VARCHAR(100)  default "Belgium"
```

### `UpdatePersonRequest` (PUT body)
Same fields as `CreatePersonRequest`.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC006-01 | `lastName` and `firstName` are required |
| BR-UC006-02 | `nationalId`, if provided, must be globally unique (case-insensitive) |
| BR-UC006-03 | `country` defaults to `Belgium` when null or blank |
| BR-UC006-04 | Cannot delete a person who is owner of buildings or units |
| BR-UC006-05 | `isTenant` is a stub (always false until lease integration) |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Person not found | 404 | `NOT_FOUND` |
| Duplicate nationalId | 409 | `IllegalArgumentException` message |
| Referenced as owner on delete | 409 | `PERSON_REFERENCED` + ownedBuildings, ownedUnits, activeLeases |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC006 — Manage Persons in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS
- Database: Flyway V001 already contains `person` table — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Entity: `Person` — table `person`, all columns. @PrePersist/@PreUpdate on createdAt/updatedAt.
2. DTOs: `PersonSummaryDTO` (id, lastName, firstName, email, gsm, isOwner, isTenant — isOwner/isTenant set in service), `PersonDTO` (full detail with nested OwnedBuildingDTO and OwnedUnitDTO inner classes), `CreatePersonRequest` (validated), `UpdatePersonRequest`
3. Mapper: `PersonMapper` (MapStruct) — toDTO ignores isOwner, isTenant, ownedBuildings, ownedUnits, leases (set in service). toSummaryDTO ignores isOwner, isTenant.
4. Repository: `PersonRepository` — findAll(Pageable), searchForPicker(query, PageRequest) with @Query (LIKE on lastName+firstName+email, case-insensitive), existsByNationalIdIgnoreCase, existsByNationalIdIgnoreCaseAndIdNot
5. Exceptions: `PersonNotFoundException`, `PersonReferencedException` (with ownedBuildings, ownedUnits, activeLeases lists)
6. Service: `PersonService` — getAll(search, Pageable), searchForPicker(q), getById(id), create(req), update(id, req), delete(id). `buildFullDTO`: sets isOwner (building OR unit owner), isTenant=false (stub), ownedBuildings, ownedUnits, leases=[]. `enrichSummaryFlags`: sets isOwner, isTenant=false.
7. Controller: `PersonController` — @RequestMapping("/api/v1/persons"). GET /, GET /{id}, POST /, PUT /{id}, DELETE /{id}, GET /picker?q=

Frontend classes to generate:
1. Model: `person.model.ts` — PersonSummary, Person, CreatePersonRequest, UpdatePersonRequest, OwnedBuilding, OwnedUnit
2. Service: `PersonService` — getAll(search?, page, size), getById(id), create(req), update(id, req), delete(id), searchForPicker(q)
3. Components (standalone):
   - `PersonListComponent` — paginated table with search bar, isOwner/isTenant badge columns, navigate to detail
   - `PersonFormComponent` — reactive form create/edit, all fields, country default Belgium
   - `PersonDetailsComponent` — full detail card, ownedBuildings list, ownedUnits list, edit/delete buttons
   - `PersonPickerComponent` — reusable autocomplete component (input: [label], output: (personSelected)). Used in BuildingFormComponent and LeaseFormComponent as owner/tenant picker. Debounces input, min 2 chars, shows lastName + firstName + email.

Business rules to enforce in frontend:
- BR-UC006-01: required lastName, firstName
- BR-UC006-02: uniqueness error displayed from backend message
- BR-UC006-04: show ownedBuildings and ownedUnits in delete error dialog
- PersonPickerComponent: disable submit until a person is selected
```
