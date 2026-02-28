# UC002 — Manage Housing Units

## Overview

| Attribute | Value |
|---|---|
| ID | UC002 |
| Name | Manage Housing Units |
| Actor | Admin |
| Module | Housing Units |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to create, read, update, and delete housing units within a building. A housing unit belongs to exactly one building and can optionally have a direct owner (overriding the building's owner). The detail view aggregates data from rooms, PEB scores, rent history, meters, and leases.

---

## User Stories

### US006 — Create Housing Unit

**As an** admin, **I want to** create a housing unit within a building **so that** I can register individual apartments.

**Acceptance Criteria:**
- AC1: Required fields: `buildingId`, `unitNumber`, `floor`.
- AC2: `unitNumber` must be unique within the building (case-insensitive). Returns HTTP 409 if duplicate.
- AC3: `ownerId` is optional; when set it must reference an existing `Person`.
- AC4: `hasTerrace = false` clears `terraceSurface` and `terraceOrientation`.
- AC5: `hasGarden = false` clears `gardenSurface` and `gardenOrientation`.
- AC6: On success returns HTTP 201 with the enriched `HousingUnitDTO`.

**Endpoint:** `POST /api/v1/units`

---

### US007 — Edit Housing Unit

**As an** admin, **I want to** update a housing unit **so that** I can correct or complete its information.

**Acceptance Criteria:**
- AC1: All fields from US006 can be updated except `buildingId`.
- AC2: `unitNumber` uniqueness is enforced excluding the current unit.
- AC3: `ownerId` can be set, changed, or cleared.
- AC4: Terrace/garden rules (BR-UC002-04/05) apply on update.
- AC5: Returns HTTP 404 if the unit does not exist.

**Endpoint:** `PUT /api/v1/units/{id}`

---

### US008 — Delete Housing Unit

**As an** admin, **I want to** delete a housing unit **so that** I can remove obsolete entries.

**Acceptance Criteria:**
- AC1: Delete an empty unit (no rooms, no PEB, no rent, no meter data) — unit is permanently removed, admin is redirected to building details, unit no longer appears in the list.
- AC2: Cannot delete a unit with rooms — error shown with room count; unit is NOT deleted.
- AC3: Cannot delete a unit with PEB history — error listing associated data; unit is NOT deleted.
- AC4: Confirmation dialog appears before deletion; clicking Cancel leaves the unit intact.
- AC5: Building unit count is updated after successful deletion.

**Endpoint:** `DELETE /api/v1/units/{id}`

**Business Rule:** `BR-UC002-06` — Cannot delete a housing unit that has at least one room.

---

### US009 — View Housing Unit Details

**As an** admin, **I want to** view a housing unit's details **so that** I can see all its characteristics and related data.

**Acceptance Criteria:**
- AC1: All unit fields are displayed: unit number, floor, landing, total surface, terrace info (if applicable), garden info (if applicable), owner name (inherited or specific), building link.
- AC2: The Rooms section shows the list of rooms with types and surfaces, total calculated surface, and an "Add Room" button.
- AC3: The PEB section shows the current score badge, score date, "View History" link, and "Add PEB Score" button.
- AC4: The Rent section shows current rent amount, effective from date, "Update Rent" and "View History" buttons.
- AC5: The Meters section shows active meters grouped by type.
- AC6: Clicking the building name navigates to the building details page.

**Endpoints:**
- `GET /api/v1/units/{id}` — single unit
- `GET /api/v1/buildings/{buildingId}/units` — all units in a building (ordered by floor ASC, unitNumber ASC)

---

### US010 — Add Terrace to Housing Unit

**As an** admin, **I want to** add terrace information to a housing unit **so that** I can track outdoor spaces associated with apartments.

**Acceptance Criteria:**
- AC1: Via the edit form, checking "Has Terrace" and entering surface and orientation saves the terrace data; unit details show terrace information.
- AC2: Orientation dropdown offers: N, S, E, W, NE, NW, SE, SW.
- AC3: Checking "Has Terrace" with no surface or orientation is valid — unit is saved with `hasTerrace = true` and no surface/orientation displayed.
- AC4: Unchecking "Has Terrace" clears `terraceSurface` and `terraceOrientation`; the fields are no longer displayed in unit details.

**Endpoint:** `PUT /api/v1/units/{id}`

---

### US011 — Add Garden to Housing Unit

**As an** admin, **I want to** add garden information to a housing unit **so that** I can track private outdoor garden spaces.

**Acceptance Criteria:**
- AC1: Via the edit form, checking "Has Garden" and entering surface and orientation saves the garden data; unit details show garden information.
- AC2: Orientation dropdown offers: N, S, E, W, NE, NW, SE, SW.
- AC3: A unit can have both terrace and garden simultaneously; both are stored and displayed.
- AC4: Leaving surface empty when "Has Garden" is checked shows error "Garden surface is required".

**Endpoint:** `PUT /api/v1/units/{id}`

---

## Data Model

### Table: `housing_unit`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `building_id` | BIGINT | NOT NULL, FK → `building.id` ON DELETE CASCADE |
| `unit_number` | VARCHAR(20) | NOT NULL, UNIQUE within building |
| `floor` | INTEGER | NOT NULL, CHECK -10..100 |
| `landing_number` | VARCHAR(10) | nullable |
| `total_surface` | DECIMAL(7,2) | nullable, CHECK > 0 |
| `has_terrace` | BOOLEAN | NOT NULL, DEFAULT false |
| `terrace_surface` | DECIMAL(7,2) | nullable, CHECK > 0 |
| `terrace_orientation` | VARCHAR(2) | nullable, CHECK IN (N,S,E,W,NE,NW,SE,SW) |
| `has_garden` | BOOLEAN | NOT NULL, DEFAULT false |
| `garden_surface` | DECIMAL(7,2) | nullable, CHECK > 0 |
| `garden_orientation` | VARCHAR(2) | nullable, CHECK IN (N,S,E,W,NE,NW,SE,SW) |
| `owner_id` | BIGINT | FK → `person.id` ON DELETE SET NULL, nullable |
| `created_by` | BIGINT | FK → `app_user.id` ON DELETE SET NULL, nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `HousingUnitDTO` (response)
```
id, buildingId, buildingName,
unitNumber, floor, landingNumber, totalSurface,
hasTerrace, terraceSurface, terraceOrientation,
hasGarden, gardenSurface, gardenOrientation,
ownerId, ownerName, effectiveOwnerName,
roomCount,             ← computed from room table
currentMonthlyRent,    ← from rent_history WHERE effectiveTo IS NULL
currentPebScore,       ← most recent peb_score_history
createdAt, updatedAt
```

### `CreateHousingUnitRequest` (POST body)
```
buildingId*       Long
unitNumber*       VARCHAR(20)
floor*            Integer  (-10..100)
landingNumber     VARCHAR(10)
totalSurface      Decimal
hasTerrace        Boolean  default false
terraceSurface    Decimal
terraceOrientation VARCHAR(2)
hasGarden         Boolean  default false
gardenSurface     Decimal
gardenOrientation VARCHAR(2)
ownerId           Long  nullable
```

### `UpdateHousingUnitRequest` (PUT body)
Same as `CreateHousingUnitRequest` minus `buildingId`.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC002-01 | `unitNumber` must be unique within the building (case-insensitive) |
| BR-UC002-02 | `buildingId` must reference an existing `Building` |
| BR-UC002-03 | `ownerId`, if provided, must reference an existing `Person` |
| BR-UC002-04 | If `hasTerrace = false`, `terraceSurface` and `terraceOrientation` are cleared |
| BR-UC002-05 | If `hasGarden = false`, `gardenSurface` and `gardenOrientation` are cleared |
| BR-UC002-06 | Cannot delete a unit that has at least one room |
| BR-UC002-09 | `effectiveOwnerName` = unit.owner ?? building.owner |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Unit not found | 404 | `Housing unit not found` |
| Has rooms on delete | 409 | `Cannot delete housing unit` + `roomCount` |
| Duplicate unit number | 409 | `DUPLICATE` |
| `buildingId` not found | 404 | `BuildingNotFoundException` |
| `ownerId` not found | 409 | `PersonNotFoundException` |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC002 — Manage Housing Units in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Lombok, Spring Security (ROLE_ADMIN on all endpoints)
- Frontend: Angular 17 standalone components, TypeScript, SCSS, RxJS
- Database: Flyway V001 already contains `housing_unit` — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Entity: `HousingUnit` — table `housing_unit`, all columns as per data model. @PrePersist/@PreUpdate on createdAt/updatedAt. Relations: ManyToOne Building, ManyToOne Person (owner), ManyToOne AppUser (createdBy). No collections on entity (avoid lazy issues).
2. DTOs: `HousingUnitDTO` (response with computed fields), `CreateHousingUnitRequest` (validated), `UpdateHousingUnitRequest` (validated)
3. Mapper: `HousingUnitMapper` (MapStruct) — map building.id/name, owner.id/name. Ignore computed fields (effectiveOwnerName, roomCount, currentMonthlyRent, currentPebScore) — set manually in service.
4. Repository: `HousingUnitRepository` — findByBuildingIdOrderByFloorAscUnitNumberAsc, countByBuildingId, existsByBuildingIdAndUnitNumberIgnoreCase, existsByBuildingIdAndUnitNumberIgnoreCaseExcluding(@Query), existsByOwnerId, findByOwnerId, findByBuildingId, existsByBuildingId
5. Exception: `HousingUnitNotFoundException`, `HousingUnitHasDataException` (with roomCount field)
6. Service: `HousingUnitService` — getUnitsByBuilding(buildingId), getUnitById(id), createUnit(req), updateUnit(id, req), deleteUnit(id). Method `toEnrichedDTO` sets effectiveOwnerName (unit.owner ?? building.owner), roomCount (from RoomRepository), currentMonthlyRent (from RentHistoryRepository WHERE effectiveTo IS NULL), currentPebScore (from PebScoreRepository ORDER BY scoreDate DESC).
7. Controller: `HousingUnitController` — endpoints as per US006–US009. No @RequestMapping prefix; use full paths on each method. @PreAuthorize not required (security configured globally) but add if consistent with existing code.

Frontend classes to generate:
1. Model: `housing-unit.model.ts` — interfaces HousingUnit, CreateHousingUnitRequest, UpdateHousingUnitRequest
2. Service: `HousingUnitService` — getUnitsByBuilding(buildingId), getUnitById(id), createUnit(req), updateUnit(id, req), delete(id)
3. Components (standalone):
   - `HousingUnitListComponent` — table inside building detail, shows unitNumber, floor, totalSurface, roomCount, currentMonthlyRent (€/mo), currentPebScore (colored badge). Navigates to unit detail on row click.
   - `HousingUnitFormComponent` — reactive form for create/edit. Routed at `/units/new?buildingId=` and `/units/:id/edit`. Includes person picker for owner. Conditional terrace/garden fields.
   - `HousingUnitDetailsComponent` — shows all fields grouped in cards. Hosts sub-components: RoomSectionComponent (UC003), PebSectionComponent (UC004), RentSectionComponent (UC005), MeterSectionComponent (UC008), LeaseSectionComponent (UC010). Edit and delete buttons in header.

Business rules to enforce:
- BR-UC002-01: show duplicate unitNumber error from backend
- BR-UC002-04/05: conditionally hide/clear terrace and garden sub-fields when toggle is false (US010, US011)
- BR-UC002-06: show roomCount in delete error message
- BR-UC002-09: display effectiveOwnerName with tooltip indicating source (unit/building)

Note: US010 (Add Terrace) and US011 (Add Garden) are implemented via the same edit form as US007. No dedicated endpoint — all handled by PUT /api/v1/units/{id}.
```
