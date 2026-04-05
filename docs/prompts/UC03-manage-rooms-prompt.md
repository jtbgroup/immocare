# ImmoCare — UC003 Manage Rooms — Implementation Prompt

I want to implement Use Case UC003 - Manage Rooms for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US012 | Add Room to Housing Unit | MUST HAVE | 3 |
| US013 | Edit Room | MUST HAVE | 2 |
| US014 | Delete Room | MUST HAVE | 2 |
| US015 | Batch Create Rooms | COULD HAVE | 5 |
| US016 | View Room Composition | MUST HAVE | 2 |

## ROOM ENTITY

```
room {
  id                  BIGINT PK
  housing_unit_id     BIGINT FK NOT NULL → housing_unit
  room_type           VARCHAR(20) NOT NULL  -- LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM,
                                             -- TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER
  approximate_surface DECIMAL(6,2) NOT NULL  -- > 0, max 999.99 m²
  created_at          TIMESTAMP NOT NULL
  updated_at          TIMESTAMP NOT NULL
}
```

Rules: multiple rooms of same type allowed; rooms are leaf entities (free delete, no cascade deps).

## BACKEND

1. Flyway migration `VXX__create_room.sql`
2. `Room` entity — `@ManyToOne` → `HousingUnit`; `@PrePersist` + `@PreUpdate`
3. `RoomDTO` — `{ id, housingUnitId, roomType, approximateSurface, createdAt, updatedAt }`
4. `RoomListResponse` — `{ rooms: List<RoomDTO>, totalSurface: BigDecimal }`
5. `CreateRoomRequest` — `{ @NotNull housingUnitId, @NotBlank roomType, @NotNull @DecimalMin("0.01") @DecimalMax("999.99") approximateSurface }`
6. `UpdateRoomRequest` — same minus housingUnitId
7. `BatchCreateRoomsRequest` — `{ @NotNull housingUnitId, @NotEmpty @Size(max=20) rooms: List<RoomEntry> }`
8. `RoomMapper` (MapStruct)
9. `RoomRepository` — `findByHousingUnitIdOrderByRoomTypeAsc`
10. `RoomService`:
    - `getRoomsByUnit(unitId)` → `RoomListResponse` (includes totalSurface)
    - `createRoom(unitId, req)`, `updateRoom(unitId, id, req)`, `deleteRoom(unitId, id)`
    - `batchCreateRooms(unitId, req)` — skip null entries; validate all before saving
    - Validate `housingUnitId` exists; throw `HousingUnitNotFoundException` if not
11. `RoomController` — base `/api/v1/housing-units/{unitId}/rooms`:
    - `GET /` → US016
    - `POST /` → US012
    - `PUT /{id}` → US013
    - `DELETE /{id}` → US014
    - `POST /batch` → US015

## FRONTEND (integrated into HousingUnitDetailsComponent)

12. `room.model.ts` — `Room`, `RoomType`, `ROOM_TYPE_LABELS`, `RoomListResponse`, `CreateRoomRequest`, `BatchCreateRoomsRequest`
13. `room.service.ts` — `getRooms`, `createRoom`, `updateRoom`, `deleteRoom`, `batchCreateRooms`
14. Rooms section in `HousingUnitDetailsComponent`:
    - Table: Type, Surface, Actions (edit/delete icons)
    - Footer: Total surface
    - Empty state: "No rooms defined yet"
    - "Add Room" button → inline form or modal (US012)
    - Edit icon → pre-filled form (US013)
    - Delete icon → confirmation dialog (US014)
    - "Quick Add Rooms" button → modal with up to 20 rows; empty rows skipped; invalid rows highlighted (US015)

## ACCEPTANCE CRITERIA

- [ ] GET returns rooms + totalSurface
- [ ] POST: room type required; surface > 0 required; multiple of same type allowed
- [ ] PUT: form pre-filled; total surface recalculated on save
- [ ] DELETE: confirmation dialog; total surface recalculated
- [ ] Batch: empty rows skipped; invalid rows highlighted
- [ ] All US012–US016 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
