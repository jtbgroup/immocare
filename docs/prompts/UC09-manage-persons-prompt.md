# ImmoCare — UC009 Manage Persons — Implementation Prompt

I want to implement Use Case UC009 - Manage Persons for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US043 | View Persons List | MUST HAVE | 2 |
| US044 | Create Person | MUST HAVE | 3 |
| US045 | Edit Person | MUST HAVE | 2 |
| US046 | Delete Person | SHOULD HAVE | 2 |
| US047 | Assign Person as Owner | MUST HAVE | 3 |
| US048 | Person Picker (Search) | MUST HAVE | 2 |

## PERSON ENTITY

```
person {
  id             BIGINT PK
  last_name      VARCHAR(100) NOT NULL
  first_name     VARCHAR(100) NOT NULL
  birth_date     DATE         NULL
  birth_place    VARCHAR(100) NULL
  national_id    VARCHAR(20)  NULL UNIQUE
  gsm            VARCHAR(20)  NULL
  email          VARCHAR(100) NULL
  street_address VARCHAR(200) NULL
  postal_code    VARCHAR(20)  NULL
  city           VARCHAR(100) NULL
  country        VARCHAR(100) NULL DEFAULT 'Belgium'
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
  updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
}
```

## MIGRATION: owner_name → owner_id

This UC migrates `building.owner_name` (VARCHAR) and `housing_unit.owner_name` (VARCHAR) to FK `owner_id → person`.

### Step 1 — `V009__create_person_and_migrate_owner.sql`
1. Create `person` table
2. Insert one PERSON per distinct non-null `owner_name` in `building`
3. Insert one PERSON per distinct non-null `owner_name` in `housing_unit` (skip duplicates)
4. Add `owner_id BIGINT NULL FK → person` to both `building` and `housing_unit`
5. Populate `owner_id` by matching inserted persons by name string
6. Keep `owner_name` for now (removed in Step 2)

### Step 2 — `V010__drop_owner_name_columns.sql`
1. `ALTER TABLE building DROP COLUMN owner_name`
2. `ALTER TABLE housing_unit DROP COLUMN owner_name`

> **Validate Step 1 in production before running Step 2.**

## BACKEND

1. `Person` entity — `@PrePersist` + `@PreUpdate`
2. `PersonDTO` — `{ id, lastName, firstName, birthDate, birthPlace, nationalId, gsm, email, streetAddress, postalCode, city, country, createdAt, updatedAt }`
3. `PersonSummaryDTO` — `{ id, fullName, city, nationalIdSuffix }` — used by person picker (US048)
4. `CreatePersonRequest` — `{ @NotBlank lastName, @NotBlank firstName, birthDate?, birthPlace?, nationalId?, gsm?, email?, streetAddress?, postalCode?, city?, country? }`
5. `UpdatePersonRequest` — same
6. `PersonMapper`
7. `PersonRepository`:
   - `Page<Person> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrNationalIdContainingIgnoreCase(String, String, String, Pageable)` — search
   - `List<Person> findTop10ByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrNationalIdContainingIgnoreCase(String, String, String)` — picker (US048)
   - `existsByNationalIdAndIdNot(String, Long)` — uniqueness check on edit
8. `PersonService`:
   - `getPersons(search, pageable)` → `Page<PersonDTO>`
   - `getPersonById(id)` → `PersonDTO`
   - `searchForPicker(q)` → `List<PersonSummaryDTO>` (min 2 chars, max 10 results, <300ms)
   - `createPerson(req)`, `updatePerson(id, req)`, `deletePerson(id)`
   - On delete: check FK constraints; throw `PersonIsOwnerException` if person is owner of buildings/units
9. `PersonController`:

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/persons | US043 |
| GET | /api/v1/persons/{id} | US045 |
| GET | /api/v1/persons/search?q= | US048 (picker) |
| POST | /api/v1/persons | US044 |
| PUT | /api/v1/persons/{id} | US045 |
| DELETE | /api/v1/persons/{id} | US046 |

10. Update `BuildingController`/`HousingUnitController` — `ownerId` field now used (already in DTOs after US009 migration)
11. Exceptions: `PersonNotFoundException`, `NationalIdTakenException`, `PersonIsOwnerException`

## FRONTEND

12. `person.model.ts` — `Person`, `PersonSummary`, `CreatePersonRequest`
13. `person.service.ts` — including `search(q: string): Observable<PersonSummary[]>` for picker
14. `PersonListComponent` — table: Last Name, First Name, Email, GSM, City, Role badges (Owner=blue, Tenant=green); search bar; pagination (US043)
15. `PersonFormComponent` — all fields; country defaults to Belgium (US044, US045)
16. `PersonDetailsComponent` — all fields; "Owned Buildings" section; "Owned Units" section; Edit + Delete buttons
17. `PersonPickerComponent` — autocomplete; min 2 chars; ≤300ms; "Create new person" shortcut (US048)
18. Integrate `PersonPickerComponent` into `BuildingFormComponent` and `HousingUnitFormComponent` (replace old owner_name text input) — US047

## ACCEPTANCE CRITERIA

- [ ] V009 migration: all existing owner_name values preserved as Person records
- [ ] V010: owner_name columns dropped cleanly
- [ ] Picker: ≤300ms for 2+ char queries
- [ ] Person with owned buildings/units: delete blocked with list
- [ ] Role badges computed dynamically (owner of building/unit → blue, active lease tenant → green)
- [ ] All US043–US048 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: 📋 Ready for Implementation
