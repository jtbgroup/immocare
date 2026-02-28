# ImmoCare — UC002 Manage Housing Units — Implementation Prompt

I want to implement Use Case UC002 - Manage Housing Units for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US006 | Create Housing Unit | MUST HAVE | 5 |
| US007 | Edit Housing Unit | MUST HAVE | 3 |
| US008 | Delete Housing Unit | MUST HAVE | 3 |
| US009 | View Housing Unit Details | MUST HAVE | 3 |
| US010 | Add Terrace to Housing Unit | SHOULD HAVE | 2 |
| US011 | Add Garden to Housing Unit | SHOULD HAVE | 2 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC002-manage-housing-units.md` — flows, business rules, test scenarios
- `docs/analysis/user-stories/US006-011` — acceptance criteria per story
- `docs/analysis/data-model.md` — HousingUnit entity definition
- `docs/analysis/data-dictionary.md` — attribute constraints and validation rules

---

## HOUSING UNIT ENTITY (to create)

```
housing_unit {
  id                   BIGINT PK AUTO_INCREMENT
  building_id          BIGINT FK NOT NULL → building(id) ON DELETE CASCADE
  unit_number          VARCHAR(20)   NOT NULL
  floor                INTEGER       NULL   -- between -10 and 100
  landing_number       VARCHAR(10)   NULL
  total_surface        DECIMAL(6,2)  NULL   -- m², must be > 0 if provided
  has_terrace          BOOLEAN       NOT NULL DEFAULT false
  terrace_surface      DECIMAL(6,2)  NULL   -- only when has_terrace = true
  terrace_orientation  VARCHAR(3)    NULL   -- N, S, E, W, NE, NW, SE, SW
  has_garden           BOOLEAN       NOT NULL DEFAULT false
  garden_surface       DECIMAL(6,2)  NULL   -- only when has_garden = true
  garden_orientation   VARCHAR(3)    NULL
  owner_id             BIGINT        NULL   FK → person(id) ON DELETE SET NULL
  created_at           TIMESTAMP     NOT NULL DEFAULT NOW()
  updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
  UNIQUE (building_id, unit_number)
}
```

**Key rules:**
- When `has_terrace = false`: always clear `terrace_surface` and `terrace_orientation` before save
- When `has_garden = false`: always clear `garden_surface` and `garden_orientation` before save
- `owner_id` inherits from building's `owner_id` for display only — NOT stored on the unit (display logic in service)
- Cannot delete if unit has rooms (throw `HousingUnitHasDataException` with `roomCount`)
- PEB, rent history, and meters are NOT blocking deletion (cascade DELETE handles them)

---

## BACKEND COMPONENTS

1. `model/entity/HousingUnit.java` — JPA entity
2. `model/dto/HousingUnitDTO.java`:
   - `{ id, buildingId, buildingName, unitNumber, floor, landingNumber, totalSurface, hasTerrace, terraceSurface, terraceOrientation, hasGarden, gardenSurface, gardenOrientation, ownerId, ownerName, roomCount, createdAt, updatedAt }`
3. `model/dto/CreateHousingUnitRequest.java` — validated
4. `model/dto/UpdateHousingUnitRequest.java` — same except `buildingId` excluded
5. `mapper/HousingUnitMapper.java` (MapStruct)
6. `repository/HousingUnitRepository.java`:
   - `findByBuildingId(Long buildingId)`
   - `existsByBuildingIdAndUnitNumberAndIdNot(buildingId, unitNumber, id)` — uniqueness check on edit
7. `service/HousingUnitService.java` — enforce all business rules above
8. `controller/HousingUnitController.java`:

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/buildings/{buildingId}/units | US009 |
| GET | /api/v1/units/{id} | US009 |
| POST | /api/v1/units | US006 |
| PUT | /api/v1/units/{id} | US007, US010, US011 |
| DELETE | /api/v1/units/{id} | US008 |

9. `exception/HousingUnitNotFoundException.java`
10. `exception/HousingUnitHasDataException.java` — with `roomCount` field
11. Update `GlobalExceptionHandler`

---

## FRONTEND

12. `housing-unit.model.ts` — TypeScript interfaces
13. `housing-unit.service.ts` — HTTP calls
14. `HousingUnitListComponent` — embedded in `BuildingDetailsComponent`:
    - Table: unit number, floor, surface, room count, owner name
    - "Add Housing Unit" button; click row → navigate to unit details
15. `HousingUnitFormComponent` — create/edit:
    - "Has Terrace" checkbox → conditional terrace fields (surface + orientation dropdown)
    - Uncheck → fields hidden AND cleared in form state
    - "Has Garden" same pattern
    - Owner person picker (US010, US011 are handled within this form)
16. `HousingUnitDetailsComponent`:
    - All fields display
    - Sections: Rooms (placeholder), PEB (placeholder), Rent (placeholder), Meters (placeholder)
    - "Edit", "Delete" buttons
17. Module, routing, navigation link in sidebar

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] POST creates unit; 409 if unit number duplicated in same building
- [ ] POST with hasTerrace=true and no surface/orientation → allowed (both optional)
- [ ] PUT clears terrace/garden data when flag set to false
- [ ] DELETE returns 409 with `roomCount` if rooms exist; succeeds if only PEB/rent/meter data
- [ ] Owner name displayed from building's owner when unit has no owner
- [ ] All US006–US011 acceptance criteria verified manually

---

**Last Updated**: 2026-02-27
**Branch**: `develop`
**Status**: ✅ Implemented
