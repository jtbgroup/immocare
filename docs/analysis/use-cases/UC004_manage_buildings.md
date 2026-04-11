# UC001 — Manage Buildings

## Overview

| Attribute | Value |
|---|---|
| ID | UC001 |
| Name | Manage Buildings |
| Actor | Admin |
| Module | Buildings |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to create, read, update, and delete buildings. A building is the top-level entity in the hierarchy: it contains housing units and can have an owner (a `Person`).

---

## User Stories

### US001 — Create Building

**As an** admin, **I want to** create a new building **so that** I can register a property in the system.

**Acceptance Criteria:**
- AC1: The form requires `name`, `streetAddress`, `postalCode`, `city`, `country`.
- AC2: `country` defaults to `Belgium` if not provided.
- AC3: `ownerId` is optional; when provided it must reference an existing `Person`.
- AC4: On success, the building is persisted and returned with HTTP 201.
- AC5: If `ownerId` does not exist, returns HTTP 409 with a descriptive error.

**Endpoint:** `POST /api/v1/buildings`

---

### US002 — Edit Building

**As an** admin, **I want to** update a building's information **so that** I can keep the registry accurate.

**Acceptance Criteria:**
- AC1: All fields from US001 can be updated.
- AC2: `ownerId` can be set, changed, or cleared (null removes the owner link).
- AC3: If the building does not exist, returns HTTP 404.
- AC4: If `ownerId` does not exist, returns HTTP 409.

**Endpoint:** `PUT /api/v1/buildings/{id}`

---

### US003 — Delete Building

**As an** admin, **I want to** delete a building **so that** I can remove obsolete entries.

**Acceptance Criteria:**
- AC1: Deletion succeeds when the building has no housing units.
- AC2: If the building still has housing units, returns HTTP 400 with `unitCount` in the response body.
- AC3: If the building does not exist, returns HTTP 404.

**Endpoint:** `DELETE /api/v1/buildings/{id}`

**Business Rule:** `BR-UC001-03` — Cannot delete a building that has one or more housing units.

---

### US004 — List Buildings

**As an** admin, **I want to** view a paginated list of buildings **so that** I can browse the property portfolio.

**Acceptance Criteria:**
- AC1: Returns a paginated result (default page size: 20).
- AC2: Can be filtered by `city` (exact match).
- AC3: Can be combined with a `search` term (matches name, address, or city — case-insensitive).
- AC4: Each result includes `id`, `name`, `streetAddress`, `postalCode`, `city`, `country`, `ownerId`, `ownerName`, `createdByUsername`, `unitCount`.

**Endpoint:** `GET /api/v1/buildings?city=&search=&page=&size=&sort=`

---

### US005 — Search Buildings

**As an** admin, **I want to** search buildings by name or address **so that** I can quickly find a specific property.

**Acceptance Criteria:**
- AC1: The `search` parameter performs a case-insensitive partial match on `name`, `streetAddress`, and `city`.
- AC2: Can be combined with the `city` filter.
- AC3: Returns an empty page if no match is found (not a 404).

**Endpoint:** `GET /api/v1/buildings?search={term}`

---

### US005b — List Cities

**As an** admin, **I want to** retrieve the list of distinct cities **so that** I can populate the city filter dropdown.

**Endpoint:** `GET /api/v1/buildings/cities`

---

## Data Model

### Table: `building`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `street_address` | VARCHAR(200) | NOT NULL |
| `postal_code` | VARCHAR(20) | NOT NULL |
| `city` | VARCHAR(100) | NOT NULL |
| `country` | VARCHAR(100) | NOT NULL, DEFAULT `'Belgium'` |
| `owner_id` | BIGINT | FK → `person.id` ON DELETE SET NULL, nullable |
| `created_by` | BIGINT | FK → `app_user.id` ON DELETE SET NULL, nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `BuildingDTO` (response)
```
id, name, streetAddress, postalCode, city, country,
ownerId, ownerName, createdByUsername, unitCount
```

### `CreateBuildingRequest` (POST body)
```
name*         VARCHAR(100)
streetAddress* VARCHAR(200)
postalCode*   VARCHAR(20)
city*         VARCHAR(100)
country       VARCHAR(100)  default "Belgium"
ownerId       Long          nullable
```

### `UpdateBuildingRequest` (PUT body)
Same fields as `CreateBuildingRequest`.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC001-01 | `name`, `streetAddress`, `postalCode`, `city` are required |
| BR-UC001-02 | `ownerId`, if provided, must reference an existing `Person` |
| BR-UC001-03 | Cannot delete a building that has at least one housing unit |
| BR-UC001-04 | `country` defaults to `Belgium` when null or blank |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Building not found | 404 | `Building not found` |
| Has housing units on delete | 400 | `Cannot delete building` + `unitCount` |
| `ownerId` not found | 409 | `PersonNotFoundException` message |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC001 — Manage Buildings in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Lombok, Spring Security (ROLE_ADMIN required on all endpoints)
- Frontend: Angular 17 standalone components, TypeScript, SCSS, RxJS
- Database: Flyway V001 (already contains the `building` table — do NOT generate a migration)
- Branch: develop

Backend classes to generate:
1. Entity: `Building` — table `building`, fields: id, name, streetAddress, postalCode, city, country, owner (ManyToOne → Person), createdBy (ManyToOne → AppUser), createdAt, updatedAt. @PrePersist / @PreUpdate on audit fields.
2. DTO: `BuildingDTO` (response), `CreateBuildingRequest` (POST, validated), `UpdateBuildingRequest` (PUT, validated)
3. Mapper: `BuildingMapper` (MapStruct) — maps owner.id → ownerId, ownerName = firstName + lastName, createdBy.username → createdByUsername, unitCount as ignored (set manually)
4. Repository: `BuildingRepository` extends JpaRepository — methods: findByCity(Pageable), searchBuildings(term, Pageable), searchBuildingsByCity(city, term, Pageable), findDistinctCities(), existsByOwnerId(Long), findByOwnerId(Long)
5. Exception: `BuildingNotFoundException`, `BuildingHasUnitsException` (with unitCount field)
6. Service: `BuildingService` — getAllBuildings(city, search, Pageable), getBuildingById(id), createBuilding(req), updateBuilding(id, req), deleteBuilding(id), getAllCities(). Enforce all BR-UC001-xx rules.
7. Controller: `BuildingController` — @RequestMapping("/api/v1/buildings"), all endpoints as per US001–US005b. @PreAuthorize("hasRole('ADMIN')").
8. GlobalExceptionHandler entries for `BuildingNotFoundException` (404) and `BuildingHasUnitsException` (400 with unitCount).

Frontend classes to generate:
1. Model: `building.model.ts` — interfaces Building, CreateBuildingRequest, UpdateBuildingRequest, Page<T>
2. Service: `BuildingService` — getAllBuildings(page, size, sort, city?, search?), getBuildingById(id), createBuilding(req), updateBuilding(id, req), deleteBuilding(id), getAllCities()
3. Components (standalone):
   - `BuildingListComponent` — paginated table with city filter and search, navigates to detail on row click
   - `BuildingFormComponent` — reactive form for create/edit (routed, reads :id for edit mode), person picker for owner, confirm cancel
   - `BuildingDetailsComponent` — shows all fields + unit list (delegates to HousingUnitListComponent)

Business rules to enforce:
- BR-UC001-01: required fields validated
- BR-UC001-02: ownerId picker uses existing PersonService search
- BR-UC001-03: show error message with unit count on delete failure
- BR-UC001-04: country defaults to Belgium
```
