# ImmoCare — UC001 Manage Buildings — Implementation Prompt

I want to implement Use Case UC001 - Manage Buildings for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US001 | Create Building | MUST HAVE | 5 |
| US002 | Edit Building | MUST HAVE | 3 |
| US003 | Delete Building | MUST HAVE | 3 |
| US004 | View Buildings List | MUST HAVE | 3 |
| US005 | Search Buildings | SHOULD HAVE | 2 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC001-manage-buildings.md` — flows, business rules, test scenarios
- `docs/analysis/user-stories/US001-005` — acceptance criteria per story
- `docs/analysis/data-model.md` — Building entity definition
- `docs/analysis/data-dictionary.md` — attribute constraints and validation rules

---

## BUILDING ENTITY

```
building {
  id             BIGINT PK AUTO_INCREMENT
  name           VARCHAR(100) NOT NULL
  street_address VARCHAR(200) NOT NULL
  postal_code    VARCHAR(20)  NOT NULL
  city           VARCHAR(100) NOT NULL
  country        VARCHAR(100) NOT NULL DEFAULT 'Belgium'
  owner_id       BIGINT       NULL  FK → person(id) ON DELETE SET NULL
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
  updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
}
```

> **Note**: `owner_id` is a FK to `person`. The old `owner_name` VARCHAR column has been removed (UC009 migration).

---

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/BuildingController.java
├── service/BuildingService.java
├── repository/BuildingRepository.java
├── model/entity/Building.java
├── model/dto/BuildingDTO.java
├── model/dto/CreateBuildingRequest.java
├── model/dto/UpdateBuildingRequest.java
├── mapper/BuildingMapper.java
└── exception/BuildingNotFoundException.java, BuildingHasUnitsException.java

frontend/src/app/features/building/
├── building-list/
├── building-form/
└── building-details/
```

---

## BACKEND

### `BuildingDTO` (response)
```java
record BuildingDTO(
    Long id, String name, String streetAddress, String postalCode,
    String city, String country,
    Long ownerId, String ownerName,   // from person.firstName + person.lastName
    int unitCount,                    // computed
    LocalDateTime createdAt, LocalDateTime updatedAt
) {}
```

### `CreateBuildingRequest`
```java
record CreateBuildingRequest(
    @NotBlank @Size(max=100) String name,
    @NotBlank @Size(max=200) String streetAddress,
    @NotBlank @Size(max=20)  String postalCode,
    @NotBlank @Size(max=100) String city,
    @NotBlank @Size(max=100) String country,
    Long ownerId   // optional
) {}
```

### `UpdateBuildingRequest` — same fields as Create.

### `BuildingRepository`
```java
Page<Building> findByNameContainingIgnoreCaseOrStreetAddressContainingIgnoreCase(
    String name, String address, Pageable pageable);
List<String> findDistinctCities();
```

### `BuildingService`
- `getAllBuildings(String search, String city, Pageable)` → `Page<BuildingDTO>`
- `getBuildingById(Long id)` → `BuildingDTO`
- `createBuilding(CreateBuildingRequest)` → `BuildingDTO`
- `updateBuilding(Long id, UpdateBuildingRequest)` → `BuildingDTO`
- `deleteBuilding(Long id)` → void
- Business rules:
  - BR-UC001-01: name, streetAddress, postalCode, city, country required
  - BR-UC001-03: cannot delete if housing units exist → `BuildingHasUnitsException`

### `BuildingController`

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/buildings | US004, US005 |
| GET | /api/v1/buildings/{id} | US004 |
| POST | /api/v1/buildings | US001 |
| PUT | /api/v1/buildings/{id} | US002 |
| DELETE | /api/v1/buildings/{id} | US003 |
| GET | /api/v1/buildings/cities | US004 |

---

## FRONTEND

### `building.model.ts`
```typescript
interface Building { id, name, streetAddress, postalCode, city, country,
                     ownerId, ownerName, unitCount, createdAt, updatedAt }
interface CreateBuildingRequest { name, streetAddress, postalCode, city, country, ownerId? }
interface UpdateBuildingRequest { name, streetAddress, postalCode, city, country, ownerId? }
```

### Components
- `BuildingListComponent` — list with search bar, city filter dropdown, sort by name/city, pagination (US004, US005)
- `BuildingFormComponent` — create/edit form, "Has Owner" toggle showing person picker, unsaved-changes guard (US001, US002)
- `BuildingDetailsComponent` — view all fields, "Edit", "Delete" buttons, embedded `HousingUnitListComponent` (US003)

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] POST /api/v1/buildings creates building with correct owner link
- [ ] DELETE /api/v1/buildings/{id} returns 409 with `unitCount` if units exist
- [ ] City filter and search work independently and combined
- [ ] owner_name resolved from person firstName + lastName
- [ ] All US001–US005 acceptance criteria verified manually

---

## DEFINITION OF DONE

- [ ] All backend classes implemented
- [ ] Unit tests: BuildingService (>80% coverage)
- [ ] Integration tests: BuildingController (all endpoints)
- [ ] All frontend components implemented
- [ ] No regression on other features
- [ ] Code reviewed and merged to `develop`

---

**Last Updated**: 2026-02-27
**Branch**: `develop`
**Status**: ✅ Implemented
