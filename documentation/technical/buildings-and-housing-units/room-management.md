---
title: Room Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "RoomController, RoomService, Room, CreateRoomRequest, UpdateRoomRequest, BatchCreateRoomsRequest, RoomSectionComponent, V007__uc007_manage_rooms.sql"
---

# Room Management

This page describes the current room operations and their API behavior. The business concept is documented in [Buildings and Housing Units](../../business/buildings-and-housing-units/concepts/buildings-and-housing-units.md); constraints are in [Housing Unit Rules](../../business/buildings-and-housing-units/rules/housing-units.md); persisted fields are in the [Housing Unit Data Dictionary](./housing-unit-data-dictionary.md).

## Estate access

Room routes include an estate ID and require estate membership; create, update, batch-create, and delete operations require manager access. However, the controller passes the estate ID only to authorization checks. `RoomService` looks up and modifies rooms by housing-unit and room IDs without verifying that the housing unit belongs to the requested estate. Room operations therefore do not currently enforce the estate boundary consistently. This is a security gap tracked in the [backlog](../../../backlog/backlog.md); do not treat the route check alone as proof of resource-level isolation.

## API behavior

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| List rooms and composition | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/rooms` | Estate member | Returns rooms ordered by room type and the sum of their approximate surfaces. An empty housing unit returns an empty room list and a total of zero. |
| Create a room | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms` | Estate manager | Creates one room and returns its DTO with `201 Created`. |
| Update a room | `PUT /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id}` | Estate manager | Updates room type and surface and returns the DTO with `200 OK`. The room must belong to the supplied housing unit. |
| Delete a room | `DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id}` | Estate manager | Deletes the room and returns `200 OK` with a message; the legacy story specifies `204 No Content`. |
| Batch-create rooms | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/batch` | Estate manager | Validates and creates 1–20 rooms in one transaction and returns the created room DTOs with `201 Created`. The response contains only the newly created rooms; the frontend reloads the full composition afterward. |

The room list's total surface is computed at read time. It does not update the housing unit's separately persisted total surface.

## Validation and errors

Each room requires a room type from the ten values listed in [Housing Unit Rules](../../business/buildings-and-housing-units/rules/housing-units.md) and an approximate surface greater than zero and less than 1,000 m². Batch entries are nested-validated before the transactional service persists them.

A missing housing unit is reported as `404 Not Found`. `RoomNotFoundException` has no dedicated exception mapping and currently reaches the generic exception handler, which responds with `500 Internal Server Error`, rather than the `404` described in the analysis. The room ID is checked against the supplied housing unit for update and deletion.

## Frontend behavior

The room section embedded in housing-unit details displays a room list and the computed total surface. Users can add one room, edit a row inline, or delete a room after confirmation. Quick add starts with three rows, supports up to 20, skips completely empty rows, and prevents submission if any partially completed row is invalid. After a successful batch request, the component reloads the room list.

The analysis materials disagree on batch handling: the use-case description requires validation of all rows and all-or-nothing persistence, while a related story says valid rows may be saved when another row is invalid. The current frontend prevents that partial submission and the backend persists the submitted batch transactionally; confirm the intended policy in [Open Questions](../../../backlog/open_questions.md#housing-units-and-leases).

## Implementation references

- Estate-scoped routes and authorization: [RoomController](../../../backend/src/main/java/com/immocare/controller/RoomController.java)
- Room operations, association checks, and batch transaction: [RoomService](../../../backend/src/main/java/com/immocare/service/RoomService.java)
- Entity, request, and response models: [Room](../../../backend/src/main/java/com/immocare/model/entity/Room.java), [CreateRoomRequest](../../../backend/src/main/java/com/immocare/model/dto/CreateRoomRequest.java), [UpdateRoomRequest](../../../backend/src/main/java/com/immocare/model/dto/UpdateRoomRequest.java), [BatchCreateRoomsRequest](../../../backend/src/main/java/com/immocare/model/dto/BatchCreateRoomsRequest.java), and [RoomListResponse](../../../backend/src/main/java/com/immocare/model/dto/RoomListResponse.java)
- Persistence: migration [V007__uc007_manage_rooms.sql](../../../backend/src/main/resources/db/migration/V007__uc007_manage_rooms.sql)
- Frontend API client and view: [RoomService](../../../frontend/src/app/core/services/room.service.ts) and [room section](../../../frontend/src/app/features/housing-unit/components/room-section/)
