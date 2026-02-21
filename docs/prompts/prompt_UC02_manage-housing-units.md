I want to implement Use Case UC002 - Manage Housing Units for ImmoCare.

## CONTEXT

- Project: ImmoCare (property management system)
- Stack: Spring Boot 3.x (backend) + Angular 17+ (frontend) + PostgreSQL 15
- Architecture: API-First, mono-repo
- UC001 (Manage Buildings) is already implemented

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC002-manage-housing-units.md`: Flows, business rules, test scenarios
- `docs/analysis/user-stories/US006-create-housing-unit.md`: Acceptance criteria
- `docs/analysis/user-stories/US007-edit-housing-unit.md`: Acceptance criteria
- `docs/analysis/user-stories/US008-delete-housing-unit.md`: Acceptance criteria
- `docs/analysis/user-stories/US009-view-housing-unit-details.md`: Acceptance criteria
- `docs/analysis/data-model.md`: HousingUnit entity definition
- `docs/analysis/data-dictionary.md`: Attribute constraints and validation rules

## USER STORIES TO IMPLEMENT

1. **US006** - Create Housing Unit (MUST HAVE, 5 pts)
2. **US007** - Edit Housing Unit (MUST HAVE, 3 pts)
3. **US008** - Delete Housing Unit (MUST HAVE, 3 pts)
4. **US009** - View Housing Unit Details (MUST HAVE, 3 pts)

## HOUSING UNIT ENTITY

```
housing_unit {
  id                  BIGINT PK AUTO_INCREMENT
  building_id         BIGINT FK NOT NULL
  unit_number         VARCHAR(20) NOT NULL          -- unique within building
  floor               INTEGER                        -- -10 to 100
  landing_number      INTEGER
  total_surface       DECIMAL(8,2)                  -- m², > 0
  has_terrace         BOOLEAN NOT NULL DEFAULT FALSE
  terrace_surface     DECIMAL(8,2)                  -- required if has_terrace
  terrace_orientation VARCHAR(3)                    -- N,S,E,W,NE,NW,SE,SW
  has_garden          BOOLEAN NOT NULL DEFAULT FALSE
  garden_surface      DECIMAL(8,2)                  -- required if has_garden
  garden_orientation  VARCHAR(3)
  owner_name          VARCHAR(100)                  -- overrides building owner if set
  created_at          TIMESTAMP NOT NULL
  updated_at          TIMESTAMP NOT NULL
  UNIQUE (building_id, unit_number)
}
```

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/
├── service/
├── repository/
├── model/
│   ├── entity/
│   └── dto/
├── mapper/
└── exception/

frontend/src/app/
├── core/services/
├── shared/
└── features/
    └── housing-unit/    ← main target
```

## WHAT TO IMPLEMENT

### Backend

1. **`model/entity/HousingUnit.java`** — JPA entity with all fields above

2. **`model/dto/HousingUnitDTO.java`** — response DTO:
   - `{ id, buildingId, buildingName, unitNumber, floor, landingNumber, totalSurface, hasTerrace, terraceSurface, terraceOrientation, hasGarden, gardenSurface, gardenOrientation, ownerName, roomCount, createdAt, updatedAt }`

3. **`model/dto/CreateHousingUnitRequest.java`**:
   - `{ buildingId, unitNumber, floor, landingNumber, totalSurface, hasTerrace, terraceSurface, terraceOrientation, hasGarden, gardenSurface, gardenOrientation, ownerName }`
   - Validated with Bean Validation annotations

4. **`model/dto/UpdateHousingUnitRequest.java`**:
   - Same fields as Create except `buildingId` (not updatable)

5. **`mapper/HousingUnitMapper.java`** (MapStruct) — entity ↔ DTO

6. **`repository/HousingUnitRepository.java`**:
   - `findByBuildingId(Long buildingId)`
   - Existence check for duplicate unit number within building

7. **`service/HousingUnitService.java`**:
   - `getUnitsByBuilding(Long buildingId)` → List\<HousingUnitDTO\>
   - `getUnitById(Long id)` → HousingUnitDTO
   - `createUnit(CreateHousingUnitRequest)` → HousingUnitDTO
   - `updateUnit(Long id, UpdateHousingUnitRequest)` → HousingUnitDTO
   - `deleteUnit(Long id)` → void
   - Business rules enforced:
     - Unit number unique within building
     - Floor between -10 and 100
     - Surface > 0
     - `terraceSurface` + `terraceOrientation` required when `hasTerrace = true`
     - `gardenSurface` + `gardenOrientation` required when `hasGarden = true`
     - Cannot delete if unit has rooms, PEB history, rent history, or water meter history
     - `ownerName` inherits from building if null (display only, not stored)

8. **`controller/HousingUnitController.java`** — REST endpoints:

| Method | Endpoint | Description | Story |
|--------|----------|-------------|-------|
| GET | /api/v1/buildings/{buildingId}/units | List units in building | US009 |
| GET | /api/v1/units/{id} | Get unit details | US009 |
| POST | /api/v1/units | Create unit | US006 |
| PUT | /api/v1/units/{id} | Update unit | US007 |
| DELETE | /api/v1/units/{id} | Delete unit | US008 |

9. **`exception/HousingUnitNotFoundException.java`**

10. Update **`GlobalExceptionHandler`** for new exceptions

### Frontend

11. **`housing-unit.model.ts`** — TypeScript interface matching HousingUnitDTO

12. **`housing-unit.service.ts`** — API calls for all endpoints

13. **`HousingUnitListComponent`** — embedded in building details page:
    - Table: unit number, floor, surface, room count, owner
    - "Add Housing Unit" button
    - Click row → navigate to unit details

14. **`HousingUnitFormComponent`** — create/edit form:
    - Required fields: unit number
    - Conditional terrace section (shown when `hasTerrace` checked): surface + orientation dropdown
    - Conditional garden section (shown when `hasGarden` checked): surface + orientation dropdown
    - Orientation dropdown options: N, S, E, W, NE, NW, SE, SW
    - "Save" and "Cancel" buttons

15. **`HousingUnitDetailsComponent`** — full detail view with sections:
    - Unit info (all fields)
    - Rooms section (placeholder "Add Room" button — UC003)
    - PEB section (placeholder — UC004)
    - Rent section (placeholder — UC005)
    - Water Meter section (placeholder — UC006)
    - "Edit" and "Delete" buttons

16. Housing unit module, routing, and navigation links from building details

## BUSINESS RULES

| Rule | Description |
|------|-------------|
| BR-UC002-01 | Unit number must be unique within a building (not globally) |
| BR-UC002-02 | Floor: integer, -10 to 100 (negative = underground) |
| BR-UC002-03 | Surface > 0 m² |
| BR-UC002-04 | terraceSurface + terraceOrientation required when hasTerrace = true |
| BR-UC002-05 | gardenSurface + gardenOrientation required when hasGarden = true |
| BR-UC002-06 | Deletion blocked if unit has rooms, PEB, rent, or water meter data |
| BR-UC002-07 | Error on delete must list what's blocking and the count |
| BR-UC002-08 | On successful delete: redirect to parent building details page |
| BR-UC002-09 | ownerName: display building owner as placeholder when unit field is null |

## DELIVERABLES ORDER

1. `HousingUnit.java` entity
2. DTOs: `HousingUnitDTO`, `CreateHousingUnitRequest`, `UpdateHousingUnitRequest`
3. `HousingUnitMapper.java`
4. `HousingUnitRepository.java`
5. `HousingUnitService.java` + `HousingUnitServiceTest.java`
6. `HousingUnitNotFoundException.java` + GlobalExceptionHandler updates
7. `HousingUnitController.java` + `HousingUnitControllerTest.java`
8. `housing-unit.model.ts`
9. `housing-unit.service.ts`
10. `HousingUnitListComponent`
11. `HousingUnitFormComponent`
12. `HousingUnitDetailsComponent`
13. Module, routing, and navigation links

## ACCEPTANCE CRITERIA SUMMARY

- [ ] GET /api/v1/buildings/{buildingId}/units returns list of units
- [ ] POST /api/v1/units creates unit with required fields
- [ ] POST /api/v1/units returns 409 if unit number already exists in building
- [ ] POST /api/v1/units validates conditional terrace/garden fields
- [ ] PUT /api/v1/units/{id} updates unit information
- [ ] DELETE /api/v1/units/{id} removes unit with no associated data
- [ ] DELETE /api/v1/units/{id} returns 409 with detail message when unit has associated data
- [ ] Angular unit list embedded in building details page
- [ ] Angular form handles conditional terrace/garden sections dynamically
- [ ] Angular delete shows confirmation dialog before deletion
- [ ] All acceptance criteria from US006 to US009 are met
