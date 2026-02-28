# UC003 — Manage Rooms

## Overview

| Attribute | Value |
|---|---|
| ID | UC003 |
| Name | Manage Rooms |
| Actor | Admin |
| Module | Housing Units → Rooms |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to manage rooms within a housing unit. Rooms are typed (LIVING_ROOM, BEDROOM, etc.) and have an approximate surface. A batch creation mode allows adding multiple rooms at once. Room data feeds into the unit's `roomCount` and optionally `totalSurface`.

---

## User Stories

### US012 — Add Room

**As an** admin, **I want to** add a room to a housing unit **so that** I can document the unit's layout.

**Acceptance Criteria:**
- AC1: Required fields: `roomType`, `approximateSurface`.
- AC2: `approximateSurface` must be > 0 and < 1000.
- AC3: `roomType` must be one of the allowed enum values.
- AC4: Returns HTTP 201 on success.
- AC5: Returns HTTP 404 if the housing unit does not exist.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rooms`

---

### US013 — Edit Room

**As an** admin, **I want to** update a room **so that** I can correct its type or surface.

**Acceptance Criteria:**
- AC1: `roomType` and `approximateSurface` can be updated.
- AC2: Same constraints as creation apply.
- AC3: Returns HTTP 404 if the room or unit does not exist.

**Endpoint:** `PUT /api/v1/housing-units/{unitId}/rooms/{roomId}`

---

### US014 — Delete Room

**As an** admin, **I want to** delete a room **so that** I can correct erroneous entries.

**Acceptance Criteria:**
- AC1: Deletion succeeds unconditionally (no blocking dependencies).
- AC2: Returns HTTP 204 on success.
- AC3: Returns HTTP 404 if the room or unit does not exist.

**Endpoint:** `DELETE /api/v1/housing-units/{unitId}/rooms/{roomId}`

---

### US015 — Batch Create Rooms

**As an** admin, **I want to** add multiple rooms at once **so that** I can quickly configure a unit.

**Acceptance Criteria:**
- AC1: Accepts a list of `{ roomType, approximateSurface }` entries.
- AC2: All entries are validated before any are persisted (all-or-nothing).
- AC3: Returns the complete updated room list on success.
- AC4: Returns HTTP 400 with field-level errors if any entry is invalid.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rooms/batch`

---

### US016 — View Room Composition

**As an** admin, **I want to** view all rooms of a housing unit **so that** I can see the composition of the unit.

**Acceptance Criteria:**
- AC1: Returns the list of rooms with `id`, `roomType`, `approximateSurface`.
- AC2: Also returns `totalSurface` (sum of all `approximateSurface` values).
- AC3: Returns an empty list (not 404) if the unit has no rooms.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/rooms`

---

## Data Model

### Table: `room`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `housing_unit_id` | BIGINT | NOT NULL, FK → `housing_unit.id` ON DELETE CASCADE |
| `room_type` | VARCHAR(20) | NOT NULL, CHECK IN (LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER) |
| `approximate_surface` | DECIMAL(6,2) | NOT NULL, CHECK > 0 AND < 1000 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `RoomDTO` (response)
```
id, housingUnitId, roomType, approximateSurface, createdAt, updatedAt
```

### `RoomListResponse` (GET list response)
```
rooms: RoomDTO[]
totalSurface: Decimal   ← sum of approximateSurface
```

### `CreateRoomRequest` (POST body)
```
roomType*           RoomType enum
approximateSurface* Decimal  (> 0, < 1000)
```

### `UpdateRoomRequest` (PUT body)
Same as `CreateRoomRequest`.

### `BatchCreateRoomsRequest` (POST /batch body)
```
rooms*: CreateRoomRequest[]   (min 1 entry)
```

---

## Enums

### `RoomType`
```
LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET,
HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC003-01 | `approximateSurface` must be > 0 and < 1000 |
| BR-UC003-02 | `roomType` must be a valid enum value |
| BR-UC003-03 | A housing unit must exist before rooms can be added |
| BR-UC003-04 | Batch creation is all-or-nothing (transactional) |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Unit not found | 404 | `HousingUnitNotFoundException` |
| Room not found | 404 | `Room not found` |
| Invalid surface | 400 | validation error |
| Invalid roomType | 400 | validation error |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC003 — Manage Rooms in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS
- Database: Flyway V001 already contains `room` table — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Entity: `Room` — table `room`. Fields: id, housingUnit (ManyToOne HousingUnit), roomType (String, CHECK), approximateSurface (BigDecimal), createdAt, updatedAt. @PrePersist/@PreUpdate.
2. Enum (or use String with @Check): RoomType — LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER
3. DTOs: `RoomDTO`, `RoomListResponse` (rooms + totalSurface), `CreateRoomRequest` (validated), `UpdateRoomRequest`, `BatchCreateRoomsRequest` (validated list)
4. Mapper: `RoomMapper` (MapStruct) — map housingUnit.id → housingUnitId
5. Repository: `RoomRepository` — findByHousingUnitIdOrderByCreatedAtAsc, countByHousingUnitId, findById with unit check
6. Service: `RoomService` — getRooms(unitId) returns RoomListResponse with totalSurface, addRoom(unitId, req), updateRoom(unitId, roomId, req), deleteRoom(unitId, roomId), batchCreate(unitId, req) — @Transactional all-or-nothing
7. Controller: `RoomController` — @RequestMapping("/api/v1/housing-units/{unitId}/rooms"), all endpoints as per US010–US014

Frontend classes to generate:
1. Model: `room.model.ts` — RoomType enum+labels, Room, RoomListResponse, CreateRoomRequest, UpdateRoomRequest, BatchRoomEntry, BatchCreateRoomsRequest
2. Service: `RoomService` — getRooms(unitId), addRoom(unitId, req), updateRoom(unitId, roomId, req), deleteRoom(unitId, roomId), batchCreate(unitId, req)
3. Component: `RoomSectionComponent` (standalone, used inside HousingUnitDetailsComponent via [unitId] input)
   - Displays room list as a table: roomType (label), approximateSurface, actions (edit/delete)
   - Shows total surface below the table
   - Inline edit row (replaces row with form inputs on edit click)
   - "Add Room" button opens inline form row
   - "Batch Add" button opens a multi-row form (configurable number of rows)
   - Delete confirmation inline

Business rules to enforce in frontend:
- approximateSurface > 0 and < 1000 (form validation)
- roomType selected from dropdown of allowed values
- Batch: all rows must be valid before submitting
```
