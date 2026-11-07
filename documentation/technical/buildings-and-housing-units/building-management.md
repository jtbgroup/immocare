---
title: Building Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Building Management

This page describes the current estate-scoped building APIs, validation, listing, and deletion behavior. Business concepts are in [Buildings and Housing Units](../../business/buildings-and-housing-units/concepts/buildings-and-housing-units.md), canonical constraints are in [Building Rules](../../business/buildings-and-housing-units/rules/buildings.md), and entity relationships are shown in the [data model](../cross-cutting/data-model.md). See [Housing Unit Management](./housing-unit-management.md) for housing-unit operations.

## Access and estate scope

Building list, city, and detail reads require estate membership. Creating, updating, and deleting buildings require manager access. Platform administrators pass these estate access checks.

List and city queries are filtered by estate. Detail, update, and delete operations verify that the building belongs to the requested estate; a building in another estate is denied. Creation assigns the building to the estate in the route.

## API behavior

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| List buildings | `GET /api/v1/estates/{estateId}/buildings?page=0&size=20&sort=name,asc&city=...&search=...` | Estate member | Returns a paginated result with building fields, owner ID and name, creator username, timestamps, and housing-unit count. Page size defaults to 20; the frontend requests name ascending by default. |
| List cities | `GET /api/v1/estates/{estateId}/buildings/cities` | Estate member | Returns distinct building cities in the estate, ordered ascending. |
| Read building | `GET /api/v1/estates/{estateId}/buildings/{id}` | Estate member | Returns the building and its housing-unit count. |
| Create building | `POST /api/v1/estates/{estateId}/buildings` | Estate manager | Creates the building and returns `201 Created` with its DTO. |
| Update building | `PUT /api/v1/estates/{estateId}/buildings/{id}` | Estate manager | Updates the editable fields and returns `200 OK`. A null owner ID clears the owner. |
| Delete building | `DELETE /api/v1/estates/{estateId}/buildings/{id}` | Estate manager | Deletes an eligible building and returns `200 OK` with a message. |

The city filter compares city values without regard to case. Search without a city filter performs case-insensitive partial matching across building name, street address, and city. When a city filter is supplied with search, the city must match the filter and the search term is matched against name or street address.

Creating and updating require non-blank name (maximum 100 characters), street address (200), postal code (20), city (100), and country (100). The building form initializes country to `Belgium`, but the API request validation rejects a missing or blank country before the entity's persistence default can apply. The actual field constraints are represented by BR-BUILD-001.

An optional owner ID is resolved to a person by ID. If no such person exists, the person-not-found handler returns `404 Not Found`; the building service does not verify that the owner belongs to the building's estate. The latter estate-scope question and associated implementation gap are tracked in [Open Questions](../../../backlog/open_questions.md#people-and-bank-accounts) and the [backlog](../../../backlog/backlog.md).

## Deletion behavior and known differences

Deletion is rejected when housing units are associated with the building. The API returns `400 Bad Request` and includes `unitCount` in the response body. The UC describes `400`, but the related delete story describes `409`; the intended contract remains an [open question](../../../backlog/open_questions.md#buildings). The deletion constraint is recorded as BR-BUILD-003.

The legacy UC and stories describe unscoped routes and use an `ADMIN` actor. The current API is estate-scoped: members can read, managers can write, and platform administrators also pass estate checks.

## Implementation references

- Backend routes and authorization: [BuildingController](../../../backend/src/main/java/com/immocare/controller/BuildingController.java)
- Estate-scoped queries, validation, and deletion: [BuildingService](../../../backend/src/main/java/com/immocare/service/BuildingService.java) and [BuildingRepository](../../../backend/src/main/java/com/immocare/repository/BuildingRepository.java)
- Request and response contracts: [CreateBuildingRequest](../../../backend/src/main/java/com/immocare/model/dto/CreateBuildingRequest.java), [UpdateBuildingRequest](../../../backend/src/main/java/com/immocare/model/dto/UpdateBuildingRequest.java), and [BuildingDTO](../../../backend/src/main/java/com/immocare/model/dto/BuildingDTO.java)
- Persistence: [Building](../../../backend/src/main/java/com/immocare/model/entity/Building.java) and migration [V005__uc005_manage_buildings.sql](../../../backend/src/main/resources/db/migration/V005__uc005_manage_buildings.sql)
- Frontend API client and views: [BuildingService](../../../frontend/src/app/core/services/building.service.ts), [building list](../../../frontend/src/app/features/building/building-list/), [building form](../../../frontend/src/app/features/building/building-form/), and [building details](../../../frontend/src/app/features/building/building-details/)
- Estate access coverage: [EstateAccessIntegrationTest](../../../backend/src/test/com/immocare/integration/EstateAccessIntegrationTest.java)
