# ImmoCare — UC003 Manage Rooms — Implementation Prompt

I want to implement Use Case UC003 - Manage Rooms for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Already implemented**: Buildings, Housing Units — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US012 | Add Room to Housing Unit | MUST HAVE | 3 |
| US013 | Edit Room | MUST HAVE | 2 |
| US014 | Delete Room | MUST HAVE | 2 |
| US015 | Quick Add Multiple Rooms | COULD HAVE | 5 |
| US016 | View Room Composition | MUST HAVE | 2 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC003-manage-rooms.md` — detailed flows, business rules, test scenarios
- `docs/analysis/user-stories/US012-016` — acceptance criteria per story
- `docs/analysis/data-model.md` — ROOM entity definition
- `docs/analysis/data-dictionary.md` — attribute constraints and validation rules

---

## ROOM ENTITY (to create)

```
room {
  id                  BIGINT PK AUTO_INCREMENT
  housing_unit_id     BIGINT FK NOT NULL → housing_unit
  room_type           VARCHAR(20) NOT NULL   -- enum, see values below
  approximate_surface DECIMAL(6,2) NOT NULL  -- must be > 0, max 999.99 m²
  created_at          TIMESTAMP NOT NULL
  updated_at          TIMESTAMP NOT NULL
}
```

**Room type enum values**:
`LIVING_ROOM`, `BEDROOM`, `KITCHEN`, `BATHROOM`, `TOILET`, `HALLWAY`, `STORAGE`, `OFFICE`, `DINING_ROOM`, `OTHER`

**Business rules**:
- Multiple rooms of the same type are allowed per unit (e.g., 3 bedrooms)
- Surface is approximate, not legally binding
- Rooms are leaf entities — they can be freely deleted with no cascade dependencies
- Total surface displayed in UI = sum of `approximate_surface` across all rooms of the unit

---

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
├── models/
└── features/
    └── housing-unit/   ← integrate the Rooms section into HousingUnitDetailsComponent
```

---

## BACKEND

### 1. Flyway Migration

File `VXX__create_room.sql`:
- Create table `room` with the columns defined above
- Index on `housing_unit_id`
- Check constraint: `approximate_surface > 0`

---

### 2. `model/entity/Room.java`

- `@Entity`, `@Table(name = "room")`
- `@ManyToOne` → `HousingUnit`
- Fields: `id`, `housingUnit`, `roomType`, `approximateSurface`, `createdAt`, `updatedAt`
- `@PrePersist` to set `createdAt`; `@PreUpdate` to set `updatedAt`
- `roomType` stored as `String` (VARCHAR enum)

---

### 3. `model/dto/RoomDTO.java` (response)

Fields:
- `id`, `housingUnitId`, `roomType`, `approximateSurface`, `createdAt`, `updatedAt`

---

### 4. `model/dto/CreateRoomRequest.java`

Fields and validation:
- `housingUnitId`: `@NotNull`
- `roomType`: `@NotBlank`, must be one of the defined enum values
- `approximateSurface`: `@NotNull`, `@DecimalMin("0.01")`, `@DecimalMax("999.99")`

---

### 5. `model/dto/UpdateRoomRequest.java`

Fields and validation (same rules as create, `housingUnitId` excluded):
- `roomType`: `@NotBlank`
- `approximateSurface`: `@NotNull`, `@DecimalMin("0.01")`, `@DecimalMax("999.99")`

---

### 6. `model/dto/BatchCreateRoomsRequest.java` (for US015 Quick Add)

Fields:
- `housingUnitId`: `@NotNull`
- `rooms`: `@NotEmpty`, `List<RoomEntry>` (max 20 entries)

`RoomEntry` inner class:
- `roomType`: `@NotBlank`
- `approximateSurface`: `@NotNull`, `@DecimalMin("0.01")`

---

### 7. `mapper/RoomMapper.java` (MapStruct)

- `Room → RoomDTO`
- `CreateRoomRequest → Room`

---

### 8. `repository/RoomRepository.java`

```java
List<Room> findByHousingUnitIdOrderByRoomTypeAsc(Long housingUnitId);
void deleteAllByHousingUnitId(Long housingUnitId);
```

---

### 9. `service/RoomService.java`

Methods:
- `getRoomsByUnit(Long housingUnitId)` → `List<RoomDTO>`
- `getTotalSurface(Long housingUnitId)` → `BigDecimal` (sum of all surfaces)
- `createRoom(CreateRoomRequest request)` → `RoomDTO`
- `updateRoom(Long id, UpdateRoomRequest request)` → `RoomDTO`
- `deleteRoom(Long id)` → void
- `batchCreateRooms(BatchCreateRoomsRequest request)` → `List<RoomDTO>` (US015)

Validation:
- Verify that the `housingUnitId` exists before creating/batch-creating rooms
- Throw `HousingUnitNotFoundException` if not found
- Throw `RoomNotFoundException` for update/delete on unknown id

---

### 10. `controller/RoomController.java`

Base URL: `/api/v1/housing-units/{unitId}/rooms`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/housing-units/{unitId}/rooms` | List all rooms for a unit (US016) |
| POST | `/api/v1/housing-units/{unitId}/rooms` | Create a single room (US012) |
| PUT | `/api/v1/housing-units/{unitId}/rooms/{id}` | Update a room (US013) |
| DELETE | `/api/v1/housing-units/{unitId}/rooms/{id}` | Delete a room (US014) |
| POST | `/api/v1/housing-units/{unitId}/rooms/batch` | Batch create rooms (US015) |

Response for list endpoint should also include:
- The list of `RoomDTO`
- `totalSurface`: sum of all surfaces (computed)

All endpoints require authentication. Return `403` if not ADMIN.

---

### 11. `exception/RoomNotFoundException.java`

- Extends `RuntimeException`
- Mapped to HTTP 404 via `@ControllerAdvice`

---

## FRONTEND

The Rooms feature is integrated **inside the existing `HousingUnitDetailsComponent`**, not a separate page.

---

### 12. `models/room.model.ts`

```typescript
export interface Room {
  id: number;
  housingUnitId: number;
  roomType: RoomType;
  approximateSurface: number;
  createdAt: string;
  updatedAt: string;
}

export type RoomType =
  | 'LIVING_ROOM' | 'BEDROOM' | 'KITCHEN' | 'BATHROOM'
  | 'TOILET' | 'HALLWAY' | 'STORAGE' | 'OFFICE'
  | 'DINING_ROOM' | 'OTHER';

export const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  LIVING_ROOM: 'Living Room',
  BEDROOM: 'Bedroom',
  KITCHEN: 'Kitchen',
  BATHROOM: 'Bathroom',
  TOILET: 'Toilet',
  HALLWAY: 'Hallway',
  STORAGE: 'Storage',
  OFFICE: 'Office',
  DINING_ROOM: 'Dining Room',
  OTHER: 'Other',
};

export interface RoomListResponse {
  rooms: Room[];
  totalSurface: number;
}

export interface CreateRoomRequest {
  housingUnitId: number;
  roomType: RoomType;
  approximateSurface: number;
}

export interface BatchCreateRoomsRequest {
  housingUnitId: number;
  rooms: { roomType: RoomType; approximateSurface: number }[];
}
```

---

### 13. `core/services/room.service.ts`

Methods:
- `getRooms(unitId: number): Observable<RoomListResponse>`
- `createRoom(request: CreateRoomRequest): Observable<Room>`
- `updateRoom(unitId: number, id: number, request): Observable<Room>`
- `deleteRoom(unitId: number, id: number): Observable<void>`
- `batchCreateRooms(request: BatchCreateRoomsRequest): Observable<Room[]>`

---

### 14. Rooms Section inside `HousingUnitDetailsComponent`

Add a **Rooms** section below the unit info panel. It should contain:

#### Display (US016):
- Table with columns: **Room Type**, **Surface (m²)**, **Actions**
- Each row has an **Edit** icon and a **Delete** icon
- Footer row showing **Total: XX.XX m²**
- If no rooms: display "No rooms defined yet"
- **"Add Room"** button at the top of the section
- **"Quick Add Rooms"** button at the top (optional, US015)

#### Add / Edit Room Form (US012, US013):
- Show as a **modal dialog** or **inline form**
- Fields:
  - Room Type: `<select>` with all enum options, human-readable labels
  - Surface: number input, suffix "m²", min 0.01, max 999.99
- Buttons: **Save**, **Cancel**
- Validation messages shown inline
- On success: refresh room list and total surface

#### Delete Confirmation (US014):
- Show confirmation dialog:
  - "Delete this room?"
  - Room type and surface shown
  - "This action cannot be undone"
- Buttons: **Confirm**, **Cancel**
- On confirm: delete room, refresh list and total surface

#### Quick Add Form (US015):
- Show as a **modal dialog**
- Display 3 empty rows by default (each row: type dropdown + surface input)
- **"Add Row"** button (max 20 rows)
- **"Remove"** icon per row
- Empty rows are ignored on save
- Invalid rows (e.g., missing surface) are highlighted in red
- **"Save All"** button → calls batch endpoint
- On success: "X rooms added successfully", refresh list

---

## ACCEPTANCE CRITERIA SUMMARY

### US012 — Add Room
- AC1: Add Room form displayed when clicking "Add Room"
- AC2: Room created with type + surface → success message + list updated + total recalculated
- AC3: All 10 room types visible in dropdown
- AC4: Error if room type not selected
- AC5: Error if surface is empty
- AC6: Multiple rooms of same type allowed

### US013 — Edit Room
- AC1: Edit form pre-filled with current values
- AC2: Room type updated successfully
- AC3: Surface updated + total recalculated
- AC4: Cancel discards changes

### US014 — Delete Room
- AC1: Room deleted + list shows n-1 rooms
- AC2: Confirmation dialog shown before deletion
- AC3: Cancel keeps room intact
- AC4: Total surface recalculated after deletion

### US015 — Quick Add Multiple Rooms
- AC1: Quick Add form shows 3 empty rows by default
- AC2: Add Row button works (up to 20)
- AC3: Remove Row button works
- AC4: Save All creates all valid rooms
- AC5: Empty rows silently skipped
- AC6: Invalid rows highlighted in red

### US016 — View Room Composition
- AC1: All rooms listed with type and surface
- AC2: Total surface calculated and displayed
- AC3: "No rooms defined yet" shown when empty
- AC4 (optional): Rooms can be grouped by type

---

## BUSINESS RULES

- `BR-UC003-01`: Room type and surface are both required
- `BR-UC003-02`: Surface must be > 0 and < 1000 m²
- `BR-UC003-03`: Multiple rooms of same type allowed per unit
- `BR-UC003-04`: Surface is approximate (not legally binding)
- `BR-UC003-05`: Rooms can be freely deleted (no dependent entities)
- `BR-UC003-06`: Total surface is informational only; does not override manually entered unit total surface

---

## PERFORMANCE REQUIREMENTS

- Room list loads with unit details: < 500ms
- Single room add/edit/delete: < 500ms
- Batch add (up to 20 rooms): < 1 second

---

## DEFINITION OF DONE

- [ ] Flyway migration created and tested
- [ ] Backend CRUD + batch endpoints implemented
- [ ] MapStruct mapper in place
- [ ] Unit tests for service layer
- [ ] Integration tests for controller layer
- [ ] Angular service and model created
- [ ] Rooms section integrated into HousingUnitDetailsComponent
- [ ] Quick Add modal implemented (US015)
- [ ] All acceptance criteria manually verified
- [ ] Code reviewed

---

**Last Updated**: 2026-02-22
**Status**: Ready for Implementation