# UC002 - Manage Housing Units

## BACKEND COMPONENTS

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
     - When `hasTerrace = false`: `terraceSurface` and `terraceOrientation` are always cleared before save
     - When `hasGarden = false`: `gardenSurface` and `gardenOrientation` are always cleared before save
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
    - Required fields: unit number, floor
    - Conditional terrace section (shown when `hasTerrace` checked): surface and orientation are optional
    - Conditional garden section (shown when `hasGarden` checked): surface and orientation are optional
    - When a checkbox is unchecked: surface and orientation fields are reset and not submitted
    - Orientation dropdown options: N, S, E, W, NE, NW, SE, SW
    - "Save" and "Cancel" buttons

15. **`HousingUnitDetailsComponent`** — full detail view with sections:
    - Unit info (all fields)
    - Terrace row shown only if `hasTerrace = true`; displays surface and orientation if provided
    - Garden row shown only if `hasGarden = true`; displays surface and orientation if provided
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
| BR-UC002-04 | When `hasTerrace = true`: terraceSurface and terraceOrientation are stored if provided (both optional). When `hasTerrace = false`: both fields are always cleared. |
| BR-UC002-05 | When `hasGarden = true`: gardenSurface and gardenOrientation are stored if provided (both optional). When `hasGarden = false`: both fields are always cleared. |
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
- [ ] POST /api/v1/units succeeds with hasTerrace=true even if surface and orientation are absent
- [ ] POST /api/v1/units clears terrace/garden data when respective flag is false
- [ ] PUT /api/v1/units/{id} updates unit information
- [ ] DELETE /api/v1/units/{id} removes unit with no associated data
- [ ] DELETE /api/v1/units/{id} returns 409 with detail message when unit has associated data
- [ ] Angular unit list embedded in building details page
- [ ] Angular form shows conditional terrace/garden sections; surface and orientation optional
- [ ] Angular form resets terrace/garden fields when checkbox is unchecked
- [ ] Angular details page shows terrace/garden row only when flag is true
- [ ] Angular delete shows confirmation dialog before deletion
- [ ] All acceptance criteria from US006 to US009 are met