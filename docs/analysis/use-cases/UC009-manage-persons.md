# Use Case UC009: Manage Persons

## Use Case Information

| Attribute | Value |
|-----------|-------|
| **Use Case ID** | UC009 |
| **Use Case Name** | Manage Persons |
| **Version** | 1.0 |
| **Status** | 📋 Ready for Implementation |
| **Priority** | HIGH - Foundation for Leases |
| **Actor(s)** | ADMIN |
| **Preconditions** | User authenticated as ADMIN |
| **Postconditions** | Person data is created, updated, or deleted in the system |
| **Related Use Cases** | UC010 (Manage Leases), UC001 (Manage Buildings), UC002 (Manage Housing Units) |

---

## Description

This use case describes how an administrator manages persons in the ImmoCare system. A Person is a central entity representing any physical individual who interacts with the property management system — whether as a building/unit owner or as a tenant on a lease. Centralizing person data avoids duplication and allows the same individual to appear in multiple roles.

This use case also covers the migration of existing `owner_name` string fields on Building and HousingUnit to a proper `owner_id` foreign key reference to the Person entity.

---

## Actors

### Primary Actor: ADMIN
- **Role**: System administrator with full access
- **Goal**: Create, view, edit, and delete person records; assign persons as owners
- **Characteristics**:
  - Has identity documents for tenants
  - Knows ownership structure of buildings and units

---

## Preconditions

1. User is logged in with ADMIN role
2. System is operational and database is accessible

---

## Basic Flow

### 1. View All Persons

**Trigger**: ADMIN navigates to the Persons page

1. System displays a paginated list of all persons
2. Each person shows:
   - Full name (last name, first name)
   - Email
   - GSM
   - City (from address)
   - Roles (Owner / Tenant — badges, derived from relations)
3. ADMIN can sort by last name, first name, or city
4. ADMIN can search by name, email, or national ID
5. Pagination: 20 items per page

**Result**: ADMIN sees all persons in the system

---

### 2. View Person Details

**Trigger**: ADMIN clicks on a person from the list

1. System displays full person details:
   - Last name, first name
   - Date of birth, place of birth
   - National ID number
   - GSM, email
   - Full address (street, postal code, city, country)
   - Created at, updated at
2. System displays related data sections:
   - **As owner**: list of buildings and/or units owned (if any)
   - **As tenant**: list of leases (active or historical) linked to this person (if any)
3. ADMIN can edit or delete the person

**Result**: ADMIN views complete person information with context

---

### 3. Create New Person

**Trigger**: ADMIN clicks "Add Person" button

1. System displays creation form:
   - **Last Name** (required, max 100 chars)
   - **First Name** (required, max 100 chars)
   - **Date of Birth** (optional, date)
   - **Place of Birth** (optional, max 100 chars)
   - **National ID** (optional, max 20 chars, unique if provided)
   - **GSM** (optional, max 20 chars)
   - **Email** (optional, valid email format, max 100 chars)
   - **Street Address** (optional, max 200 chars)
   - **Postal Code** (optional, max 20 chars)
   - **City** (optional, max 100 chars)
   - **Country** (optional, max 100 chars, default: "Belgium")

2. ADMIN fills form and clicks "Save"
3. System validates all fields
4. System saves person, generates ID and timestamps
5. System redirects to person details page
6. System shows success message

**Result**: New person is created and available for assignment

---

### 4. Edit Person

**Trigger**: ADMIN clicks "Edit" on person details

1. System displays pre-filled edit form
2. ADMIN modifies fields and clicks "Save"
3. System validates and updates record
4. System shows success message

**Result**: Person information is updated everywhere it is referenced

---

### 5. Delete Person

**Trigger**: ADMIN clicks "Delete" on person details

1. System checks if person is referenced:
   - As owner of a building or housing unit
   - As tenant on any lease (active or historical)
2. If referenced: system blocks deletion and shows which entities reference this person
3. If not referenced: system displays confirmation dialog
4. ADMIN confirms deletion
5. System permanently deletes the person

**Result**: Person is removed from the system (only if not referenced)

---

### 6. Assign Person as Owner to Building or Housing Unit

**Trigger**: ADMIN edits a building or housing unit and selects an owner

1. On the Building or HousingUnit edit form, the "Owner" field is now a person picker (search by name)
2. ADMIN searches and selects a person from the list
3. ADMIN can also clear the owner (set to null = no owner)
4. System saves the `owner_id` reference
5. Owner inheritance rule: if `housing_unit.owner_id` is null, the building's owner is displayed

**Result**: Building or unit is linked to a Person as owner

---

## Exception Flows

### Exception Flow 1: Duplicate National ID
**Trigger**: ADMIN saves a person with a national ID that already exists

1. System detects duplicate
2. System displays error: "A person with this national ID already exists"
3. System shows a link to the existing person
4. ADMIN must either correct the ID or use the existing record

---

### Exception Flow 2: Cannot Delete Referenced Person
**Trigger**: ADMIN attempts to delete a person linked to a building, unit, or lease

1. System detects references
2. System displays error with detail:
   - "This person is owner of: [Building X], [Unit Y]"
   - "This person is tenant on: [Lease #Z (active)]"
3. ADMIN must remove references before deletion

---

## Business Rules

### BR-UC009-01: Minimum Required Fields
Only last name and first name are required to create a person.
**Rationale**: A person may be created quickly during lease creation; details can be completed later.

### BR-UC009-02: National ID Uniqueness
If provided, the national ID must be unique across all persons.
**Rationale**: Prevents duplicate person records for the same individual.

### BR-UC009-03: No Deletion if Referenced
A person cannot be deleted if they are referenced as owner or tenant.
**Rationale**: Prevents orphaned references and data integrity violations.

### BR-UC009-04: Owner Inheritance
If a housing unit has no owner (`owner_id` is null), it inherits the owner of its building.
**Rationale**: Simplifies data entry for buildings with a single owner.

### BR-UC009-05: Person is Role-Agnostic
The Person entity has no role field. The role (owner, tenant) is determined by the relationships (building.owner_id, housing_unit.owner_id, lease_tenant.person_id).
**Rationale**: A person may be owner and tenant simultaneously (different properties).

---

## Data Elements

### Input Data
| Field | Type | Required | Max Length | Notes |
|-------|------|----------|------------|-------|
| last_name | String | YES | 100 | |
| first_name | String | YES | 100 | |
| birth_date | Date | NO | — | Cannot be in the future |
| birth_place | String | NO | 100 | |
| national_id | String | NO | 20 | Unique if provided |
| gsm | String | NO | 20 | |
| email | String | NO | 100 | Valid email format |
| street_address | String | NO | 200 | |
| postal_code | String | NO | 20 | |
| city | String | NO | 100 | |
| country | String | NO | 100 | Default: Belgium |

### Output Data
- Person ID (generated)
- All input data
- Derived roles (Owner / Tenant — not stored, computed)
- Created at, updated at

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/persons` | Paginated list with search |
| GET | `/api/v1/persons/{id}` | Person details with relations |
| POST | `/api/v1/persons` | Create person |
| PUT | `/api/v1/persons/{id}` | Update person |
| DELETE | `/api/v1/persons/{id}` | Delete person (if not referenced) |
| GET | `/api/v1/persons/search?q=name` | Quick search for person picker |

---

## UI Requirements

### Persons List Page
- Search bar (searches name, email, national ID)
- Table: Last Name / First Name / Email / GSM / City / Roles
- Role badges: "Owner" (blue), "Tenant" (green)
- "Add Person" button
- Pagination

### Person Details Page
- Full info card (all fields)
- "As Owner" section: list of buildings/units
- "As Tenant" section: list of leases with status badge
- "Edit" and "Delete" buttons

### Person Form
- Grouped sections: Identity / Contact / Address
- National ID field with uniqueness check (real-time)
- Country defaults to "Belgium"
- "Save" and "Cancel"

### Person Picker (used in Building/Unit/Lease forms)
- Inline search input: "Search person by name..."
- Dropdown results showing: Full name + city + national ID (if available)
- "Create new person" shortcut link
- Clear/remove button

---

## Test Scenarios

| ID | Scenario | Expected |
|----|----------|----------|
| TS-UC009-01 | Create person with name only | Saved, details displayed |
| TS-UC009-02 | Create person with all fields | All data stored correctly |
| TS-UC009-03 | Create with duplicate national ID | Error shown, link to existing |
| TS-UC009-04 | Edit person's email | Updated everywhere referenced |
| TS-UC009-05 | Delete unreferenced person | Deleted successfully |
| TS-UC009-06 | Delete person who is owner of building | Blocked, building listed |
| TS-UC009-07 | Delete person who is tenant on active lease | Blocked, lease listed |
| TS-UC009-08 | Assign person as owner to building | Owner displayed on building details |
| TS-UC009-09 | Unit with no owner inherits building owner | Building owner shown on unit |
| TS-UC009-10 | Search person by name in picker | Dropdown shows matching results |

---

## Related User Stories

- **US043**: View persons list
- **US044**: Create person
- **US045**: Edit person
- **US046**: Delete person
- **US047**: Assign person as owner to building/unit
- **US048**: Search person (picker)

---

## Notes

- Migration required: `building.owner_name VARCHAR` → `building.owner_id BIGINT FK → person`
- Migration required: `housing_unit.owner_name VARCHAR` → `housing_unit.owner_id BIGINT FK → person`
- Migration strategy: create PERSON records from existing name strings, then switch FK; done in 2 Flyway steps
- Person picker must be fast (< 300ms) for good UX during lease creation

---

**Last Updated**: 2026-02-24
**Version**: 1.0
**Status**: 📋 Ready for Implementation
