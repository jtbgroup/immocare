# Prompt UC02 — Manage Housing Units

## Backend classes to generate

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
    - "Has Terrace" checkbox → conditional terrace fields (surface required + orientation dropdown optional)
    - Surface field becomes required (validated > 0) when "Has Terrace" is checked
    - Orientation can be set, changed, or cleared independently; empty string sent as null
    - Uncheck "Has Terrace" → fields hidden AND cleared in form state
    - "Has Garden" same pattern as terrace
    - Owner person picker
16. `HousingUnitDetailsComponent`:
    - All fields display
    - Sections: Rooms (placeholder), PEB (placeholder), Rent (placeholder), Meters (placeholder)
    - "Edit", "Delete" buttons
17. Module, routing, navigation link in sidebar

---

## BUSINESS RULES TO ENFORCE

| Rule | Description |
|---|---|
| BR-UC002-01 | `unitNumber` unique within building (case-insensitive) |
| BR-UC002-04 | `hasTerrace = false` → clear `terraceSurface` and `terraceOrientation` |
| BR-UC002-05 | `hasGarden = false` → clear `gardenSurface` and `gardenOrientation` |
| BR-UC002-06 | Cannot delete unit with rooms; return `roomCount` in error |
| BR-UC002-07 | `hasTerrace = true` → `terraceSurface` required and must be > 0 |
| BR-UC002-08 | `hasGarden = true` → `gardenSurface` required and must be > 0 |
| BR-UC002-09 | `effectiveOwnerName` = unit.owner ?? building.owner |

**Note on orientation (NullValuePropertyMappingStrategy):** The MapStruct mapper uses `IGNORE` strategy. After calling `updateEntityFromRequest`, the service must explicitly call `unit.setTerraceOrientation(normalizeOrientation(...))` and `unit.setGardenOrientation(normalizeOrientation(...))` when the respective flag is true, to allow clearing the orientation. `normalizeOrientation` converts `""` and `null` to `null`.

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] POST creates unit; 409 if unit number duplicated in same building
- [ ] POST with hasTerrace=true and no surface → 400 error; surface required and > 0
- [ ] POST with hasTerrace=true and surface set but no orientation → allowed; orientation optional
- [ ] PUT clears terrace/garden data when flag set to false
- [ ] PUT allows clearing orientation independently when flag remains true
- [ ] DELETE returns 409 with `roomCount` if rooms exist; succeeds if only PEB/rent/meter data
- [ ] Owner name displayed from building's owner when unit has no owner
- [ ] All US006–US011 acceptance criteria verified manually

---

**Last Updated**: 2026-03-01
**Branch**: `develop`
**Status**: ✅ Implemented