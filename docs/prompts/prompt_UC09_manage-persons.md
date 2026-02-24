# ImmoCare — UC009 Manage Persons — Implementation Prompt

I want to implement Use Case UC009 - Manage Persons for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US043 | View persons list | MUST HAVE | 2 |
| US044 | Create person | MUST HAVE | 3 |
| US045 | Edit person | MUST HAVE | 2 |
| US046 | Delete person | SHOULD HAVE | 2 |
| US047 | Assign person as owner to building/unit | MUST HAVE | 3 |
| US048 | Search person (picker) | MUST HAVE | 2 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC009-manage-persons.md` — flows, business rules, test scenarios
- `docs/analysis/user-stories/US043-048` — acceptance criteria per story
- `docs/analysis/data-model.md` — data model reference
- `docs/analysis/data-dictionary.md` — constraints and validation rules

---

## NEW ENTITY: PERSON

```
person {
  id               BIGINT PK AUTO_INCREMENT
  last_name        VARCHAR(100) NOT NULL
  first_name       VARCHAR(100) NOT NULL
  birth_date       DATE         NULL
  birth_place      VARCHAR(100) NULL
  national_id      VARCHAR(20)  NULL UNIQUE   -- unique if provided
  gsm              VARCHAR(20)  NULL
  email            VARCHAR(100) NULL           -- valid email format if provided
  street_address   VARCHAR(200) NULL
  postal_code      VARCHAR(20)  NULL
  city             VARCHAR(100) NULL
  country          VARCHAR(100) NULL DEFAULT 'Belgium'
  created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
  updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
}
```

---

## MIGRATION STRATEGY: owner_name → owner_id

This UC also migrates `building.owner_name` and `housing_unit.owner_name` (VARCHAR strings) to
`building.owner_id` and `housing_unit.owner_id` (FK → person). This is done in **two Flyway steps**:

### Step 1 — `V009__create_person_and_migrate_owner.sql`
1. Create the `person` table
2. Insert one PERSON record per distinct non-null `owner_name` value found in `building`
3. Insert one PERSON record per distinct non-null `owner_name` value found in `housing_unit` (that doesn't already exist)
4. Add columns `owner_id BIGINT NULL FK → person` to both `building` and `housing_unit`
5. Populate `owner_id` from the newly inserted person records (match by name string)
6. Keep `owner_name` columns for now (remove in Step 2)

### Step 2 — `V010__drop_owner_name_columns.sql`
1. Drop `building.owner_name`
2. Drop `housing_unit.owner_name`

> **Important**: Run and validate Step 1 before executing Step 2.

---

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/
│   └── PersonController.java           ← new
├── service/
│   └── PersonService.java              ← new
├── repository/
│   └── PersonRepository.java           ← new
├── model/
│   ├── entity/
│   │   └── Person.java                 ← new
│   └── dto/
│       ├── PersonDTO.java              ← new
│       ├── PersonSummaryDTO.java       ← new (for lists and picker)
│       ├── CreatePersonRequest.java    ← new
│       └── UpdatePersonRequest.java    ← new
├── mapper/
│   └── PersonMapper.java               ← new (MapStruct)
└── exception/
    └── PersonNotFoundException.java    ← new
    └── PersonReferencedException.java  ← new (blocked delete)

frontend/src/app/
├── core/services/
│   └── person.service.ts               ← new
├── models/
│   └── person.model.ts                 ← new
├── shared/components/
│   └── person-picker/                  ← new, reusable across forms
│       ├── person-picker.component.ts
│       ├── person-picker.component.html
│       └── person-picker.component.scss
└── features/
    └── person/                         ← new feature module
        ├── person.module.ts
        ├── person-routing.module.ts
        └── components/
            ├── person-list/
            ├── person-form/
            └── person-details/

-- Also modify:
features/building/components/building-form/   ← replace owner_name text with PersonPicker
features/housing-unit/components/unit-form/   ← replace owner_name text with PersonPicker
```

---

## BACKEND

### 1. Flyway Migrations

#### `V009__create_person_and_migrate_owner.sql`
```sql
-- Create person table
CREATE TABLE person (
    id             BIGSERIAL     PRIMARY KEY,
    last_name      VARCHAR(100)  NOT NULL,
    first_name     VARCHAR(100)  NOT NULL,
    birth_date     DATE          NULL,
    birth_place    VARCHAR(100)  NULL,
    national_id    VARCHAR(20)   NULL,
    gsm            VARCHAR(20)   NULL,
    email          VARCHAR(100)  NULL,
    street_address VARCHAR(200)  NULL,
    postal_code    VARCHAR(20)   NULL,
    city           VARCHAR(100)  NULL,
    country        VARCHAR(100)  NULL DEFAULT 'Belgium',
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_person_national_id UNIQUE (national_id)
);

CREATE INDEX idx_person_last_name ON person(last_name);
CREATE INDEX idx_person_national_id ON person(national_id);

-- Insert persons from building owner_name (split "Last First" is risky — store full name in last_name)
INSERT INTO person (last_name, first_name, created_at, updated_at)
SELECT DISTINCT owner_name, '', NOW(), NOW()
FROM building
WHERE owner_name IS NOT NULL AND owner_name <> '';

-- Insert persons from housing_unit owner_name (only if not already in person)
INSERT INTO person (last_name, first_name, created_at, updated_at)
SELECT DISTINCT hu.owner_name, '', NOW(), NOW()
FROM housing_unit hu
WHERE hu.owner_name IS NOT NULL AND hu.owner_name <> ''
  AND NOT EXISTS (
    SELECT 1 FROM person p WHERE p.last_name = hu.owner_name AND p.first_name = ''
  );

-- Add owner_id FK columns
ALTER TABLE building ADD COLUMN owner_id BIGINT NULL
    REFERENCES person(id) ON DELETE SET NULL;

ALTER TABLE housing_unit ADD COLUMN owner_id BIGINT NULL
    REFERENCES person(id) ON DELETE SET NULL;

-- Populate owner_id from existing owner_name
UPDATE building b
SET owner_id = p.id
FROM person p
WHERE b.owner_name IS NOT NULL AND b.owner_name = p.last_name AND p.first_name = '';

UPDATE housing_unit hu
SET owner_id = p.id
FROM person p
WHERE hu.owner_name IS NOT NULL AND hu.owner_name = p.last_name AND p.first_name = '';

CREATE INDEX idx_building_owner_id     ON building(owner_id);
CREATE INDEX idx_housing_unit_owner_id ON housing_unit(owner_id);
```

#### `V010__drop_owner_name_columns.sql`
```sql
ALTER TABLE building     DROP COLUMN owner_name;
ALTER TABLE housing_unit DROP COLUMN owner_name;
```

---

### 2. `model/entity/Person.java`

- `@Entity`, `@Table(name = "person")`
- All fields as defined in the schema above
- `@PrePersist` → sets `createdAt`; `@PreUpdate` → sets `updatedAt`
- No relationships defined here (relationships are owned by the other side)

---

### 3. `model/entity/Building.java` — UPDATE

- Remove `ownerName` field
- Add `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") Person owner;`

---

### 4. `model/entity/HousingUnit.java` — UPDATE

- Remove `ownerName` field
- Add `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") Person owner;`

---

### 5. DTOs

#### `PersonDTO.java` (full response)
```java
{
  Long id;
  String lastName;
  String firstName;
  LocalDate birthDate;
  String birthPlace;
  String nationalId;
  String gsm;
  String email;
  String streetAddress;
  String postalCode;
  String city;
  String country;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
  // Derived (computed by service):
  boolean isOwner;   // true if referenced as owner of any building or unit
  boolean isTenant;  // true if referenced as tenant on any lease
}
```

#### `PersonSummaryDTO.java` (for lists and picker)
```java
{
  Long id;
  String lastName;
  String firstName;
  String city;
  String nationalId;  // may be null
  boolean isOwner;
  boolean isTenant;
}
```

#### `CreatePersonRequest.java`
```java
@NotBlank @Size(max=100) String lastName;
@NotBlank @Size(max=100) String firstName;
LocalDate birthDate;              // optional, past or present
@Size(max=100) String birthPlace;
@Size(max=20)  String nationalId; // optional, unique
@Size(max=20)  String gsm;
@Email @Size(max=100) String email;
@Size(max=200) String streetAddress;
@Size(max=20)  String postalCode;
@Size(max=100) String city;
@Size(max=100) String country;    // default "Belgium" if null
```

#### `UpdatePersonRequest.java`
Same fields as `CreatePersonRequest`.

---

### 6. `PersonRepository.java`

```java
Page<Person> findAll(Specification<Person> spec, Pageable pageable);
Optional<Person> findByNationalIdIgnoreCase(String nationalId);
boolean existsByNationalIdIgnoreCaseAndIdNot(String nationalId, Long id);
List<PersonSummaryDTO> searchForPicker(String query, Pageable pageable); // @Query
```

Picker query (search by last name, first name, or national ID, case-insensitive):
```sql
SELECT new com.immocare.model.dto.PersonSummaryDTO(...)
FROM Person p
WHERE LOWER(CONCAT(p.lastName, ' ', p.firstName)) LIKE LOWER(CONCAT('%', :query, '%'))
   OR LOWER(p.nationalId) LIKE LOWER(CONCAT('%', :query, '%'))
```

---

### 7. `PersonService.java`

Key methods:
- `getAll(String search, Pageable)` → `Page<PersonSummaryDTO>` with derived isOwner/isTenant flags
- `getById(Long id)` → `PersonDTO` with relations
- `create(CreatePersonRequest)` → validates uniqueness of nationalId → saves → returns `PersonDTO`
- `update(Long id, UpdatePersonRequest)` → same validations (excluding self on nationalId) → saves
- `delete(Long id)` → checks if referenced as owner or tenant → throws `PersonReferencedException` with detail if so
- `searchForPicker(String query)` → `List<PersonSummaryDTO>` (max 10 results, min 2 chars query)

**Reference check for delete**:
```java
boolean usedAsOwner = buildingRepository.existsByOwnerId(id) 
                   || housingUnitRepository.existsByOwnerId(id);
boolean usedAsTenant = leaseTenantRepository.existsByPersonId(id);
if (usedAsOwner || usedAsTenant) throw new PersonReferencedException(id, ...details);
```

---

### 8. `PersonController.java`

```
GET    /api/v1/persons?page=0&size=20&sort=lastName,asc&search=text
GET    /api/v1/persons/search?q=dupont          ← picker endpoint (returns max 10)
GET    /api/v1/persons/{id}
POST   /api/v1/persons
PUT    /api/v1/persons/{id}
DELETE /api/v1/persons/{id}
```

---

### 9. Exception Handling — update `GlobalExceptionHandler`

- `PersonNotFoundException` → 404 with message "Person not found: {id}"
- `PersonReferencedException` → 409 with body:
```json
{
  "error": "PERSON_REFERENCED",
  "message": "This person cannot be deleted because they are still referenced.",
  "ownedBuildings": ["Résidence Soleil"],
  "ownedUnits": ["Building X - Unit A101"],
  "activeLeases": ["Lease #12 (Unit A101, ACTIVE)"]
}
```

---

### 10. Update `BuildingService` and `HousingUnitService`

- Replace `owner_name` string logic with `owner_id` FK logic
- `BuildingDTO` and `HousingUnitDTO` now include `ownerId` (Long, nullable) and `ownerName` (String, computed: `person.lastName + " " + person.firstName`, or inherited)
- Owner inheritance: if `housingUnit.owner` is null → display `building.owner` (computed in service, not stored)

---

### 11. Tests

- `PersonServiceTest.java`: create, update, delete (free), delete (referenced → exception), search picker
- `PersonControllerTest.java`: all endpoints, including 409 on referenced delete
- Update `BuildingServiceTest` and `HousingUnitServiceTest` for owner_id changes

---

## FRONTEND

### 12. `person.model.ts`

```typescript
export interface Person {
  id: number;
  lastName: string;
  firstName: string;
  birthDate?: string;
  birthPlace?: string;
  nationalId?: string;
  gsm?: string;
  email?: string;
  streetAddress?: string;
  postalCode?: string;
  city?: string;
  country?: string;
  isOwner: boolean;
  isTenant: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PersonSummary {
  id: number;
  lastName: string;
  firstName: string;
  city?: string;
  nationalId?: string;
  isOwner: boolean;
  isTenant: boolean;
}
```

---

### 13. `person.service.ts`

Methods:
- `getAll(page, size, sort, search)` → `Observable<Page<PersonSummary>>`
- `getById(id)` → `Observable<Person>`
- `create(request)` → `Observable<Person>`
- `update(id, request)` → `Observable<Person>`
- `delete(id)` → `Observable<void>`
- `searchForPicker(query)` → `Observable<PersonSummary[]>`

---

### 14. `features/person/` components

#### `person-list`
- Table: Last Name | First Name | Email | GSM | City | Roles
- Role badges: "Owner" (blue chip), "Tenant" (green chip)
- Search bar (debounce 300ms)
- Sort on columns: last name, first name, city
- Pagination
- "Add Person" button → navigates to `/persons/new`
- Click row → `/persons/{id}`

#### `person-form` (create and edit mode)
- Grouped sections with `<fieldset>`:
  - **Identity**: last_name*, first_name*, birth_date, birth_place, national_id
  - **Contact**: gsm, email
  - **Address**: street_address, postal_code, city, country (default Belgium)
- Real-time nationalId uniqueness check (debounce 500ms → GET /persons/search?q=)
- "Save" and "Cancel" buttons
- Show existing person link if duplicate national ID detected

#### `person-details`
- Full info card
- "As Owner" section: list of buildings and units (links)
- "As Tenant" section: list of leases (links, with status badge)
- "Edit" button → `/persons/{id}/edit`
- "Delete" button → confirmation dialog → handles 409 with detail message

---

### 15. `shared/components/person-picker/`

Reusable component used in building-form, unit-form, and later lease-form.

**Inputs**:
- `label: string` — e.g., "Owner"
- `selectedPerson: PersonSummary | null`
- `required: boolean`

**Outputs**:
- `personSelected: EventEmitter<PersonSummary | null>`

**Behavior**:
- Displays selected person's name (or placeholder "Search person...")
- On click: opens inline dropdown with search input
- Calls `person.service.searchForPicker(query)` on input (debounce 300ms, min 2 chars)
- Dropdown shows: "Dupont Jean — Brussels (85.01.15-123.45)"
- "Clear" button (X) if a person is selected
- "Create new person" link → opens `/persons/new` in new tab or modal (Phase 2)
- Emits `null` when cleared

---

### 16. Update `building-form` and `unit-form`

- Replace the `owner_name` text input with `<app-person-picker label="Owner" ...>`
- On form save: send `ownerId: number | null` instead of `ownerName: string`
- On form load: pre-fill picker from `building.ownerId` / `housingUnit.ownerId`

---

### 17. `person-routing.module.ts`

```
/persons          → PersonListComponent
/persons/new      → PersonFormComponent (create mode)
/persons/:id      → PersonDetailsComponent
/persons/:id/edit → PersonFormComponent (edit mode)
```

Add "Persons" link to the main navigation menu.

---

## API ENDPOINTS SUMMARY

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/persons` | Paginated list with optional search |
| GET | `/api/v1/persons/search?q=text` | Picker search (max 10 results) |
| GET | `/api/v1/persons/{id}` | Full person details with relations |
| POST | `/api/v1/persons` | Create person |
| PUT | `/api/v1/persons/{id}` | Update person |
| DELETE | `/api/v1/persons/{id}` | Delete (409 if referenced) |

---

## BUSINESS RULES SUMMARY

| Rule | Enforcement |
|------|-------------|
| last_name and first_name required | @NotBlank + frontend |
| national_id unique if provided | DB UNIQUE + service check + real-time frontend |
| birth_date not in future | @PastOrPresent + frontend |
| email valid format | @Email + frontend |
| Cannot delete if referenced as owner or tenant | Service → PersonReferencedException → 409 |
| Owner inheritance: unit → building | Service layer (computed, not stored) |
| country defaults to Belgium | Service sets default if null |

---

## IMPLEMENTATION ORDER

1. `V009__create_person_and_migrate_owner.sql`
2. `V010__drop_owner_name_columns.sql`
3. `model/entity/Person.java`
4. Update `model/entity/Building.java` (owner_name → owner FK)
5. Update `model/entity/HousingUnit.java` (owner_name → owner FK)
6. `model/dto/PersonDTO.java`, `PersonSummaryDTO.java`, `CreatePersonRequest.java`, `UpdatePersonRequest.java`
7. `mapper/PersonMapper.java`
8. `repository/PersonRepository.java`
9. `service/PersonService.java` + `PersonServiceTest.java`
10. Update `BuildingService` + `HousingUnitService` (owner logic)
11. `exception/PersonNotFoundException.java` + `PersonReferencedException.java`
12. Update `GlobalExceptionHandler`
13. `controller/PersonController.java` + `PersonControllerTest.java`
14. Update `BuildingController` + `HousingUnitController` DTOs
15. `models/person.model.ts`
16. `core/services/person.service.ts`
17. `shared/components/person-picker/` component
18. Update `building-form` and `unit-form` (replace owner_name with PersonPicker)
19. `features/person/` components (list, form, details)
20. `person-routing.module.ts` + navigation menu update

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] Person list displays with role badges (Owner / Tenant)
- [ ] Search filters by name and national ID
- [ ] Create person with name only works
- [ ] Create with all fields works
- [ ] Duplicate national ID blocked with link to existing person
- [ ] Edit updates all references
- [ ] Delete unreferenced person works
- [ ] Delete referenced person returns 409 with detail
- [ ] Building form uses PersonPicker (no more free text owner)
- [ ] Unit form uses PersonPicker
- [ ] Owner inheritance works (unit shows building owner when no specific owner)
- [ ] Person picker search responds in < 300ms
- [ ] "Create new person" available from picker
- [ ] All existing tests still pass after migration

---

## DEFINITION OF DONE

- [ ] Both Flyway migrations executed and validated
- [ ] All backend files implemented
- [ ] Unit tests: PersonService (> 80% coverage)
- [ ] Integration tests: PersonController (all endpoints)
- [ ] All existing service/controller tests updated for owner_id change
- [ ] PersonPicker component works in building and unit forms
- [ ] Person CRUD UI implemented
- [ ] All US043–US048 acceptance criteria verified manually
- [ ] Code reviewed and merged to `develop`

---

**Last Updated**: 2026-02-24
**Branch**: `develop`
**Status**: Ready for Implementation
