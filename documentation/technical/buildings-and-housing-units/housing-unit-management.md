---
title: Housing Unit Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "HousingUnitController, HousingUnitService, CreateHousingUnitRequest, UpdateHousingUnitRequest, HousingUnitFormComponent, HousingUnitDetailsComponent, RoomSectionComponent, PebScoreController, PebSectionComponent"
---

# Housing Unit Management

This page describes the current housing-unit API, access control, validation, and deletion behavior. Business concepts are in [Buildings and Housing Units](../../business/buildings-and-housing-units/concepts/buildings-and-housing-units.md), constraints are in [Housing Unit Rules](../../business/buildings-and-housing-units/rules/housing-units.md), and persisted fields are in the [Housing Unit Data Dictionary](./housing-unit-data-dictionary.md). Room operations are described in [Room Management](./room-management.md), and PEB rating history and certificate alerts are described in [PEB Score Management](./peb-score-management.md).

## Access and estate scope

All housing-unit routes are scoped to an estate. Estate members can list and read units; estate managers can create, update, and delete them. The requested building must belong to the route's estate, and a unit outside that estate is not available through that estate's routes. Platform administrators pass the estate access checks.

An optional owner ID is resolved to a person, but the housing-unit service does not check that the person belongs to the same estate as the unit. This consistency question is tracked in [Open Questions](../../../backlog/open_questions.md#people-and-bank-accounts), and the implementation gap is listed in the [backlog](../../../backlog/backlog.md).

## API behavior

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| List units in a building | `GET /api/v1/estates/{estateId}/buildings/{buildingId}/units` | Estate member | Returns the units in that estate and building, with computed room count, current rent, latest PEB score, active lease status, and effective owner name. The query has no explicit ordering. |
| List all units in an estate | `GET /api/v1/estates/{estateId}/units` | Estate member | Returns estate-scoped units ordered by unit number. |
| Read a unit | `GET /api/v1/estates/{estateId}/units/{id}` | Estate member | Returns the unit with the same computed fields. |
| Create a unit | `POST /api/v1/estates/{estateId}/units` | Estate manager | Requires a building ID, unit number, and floor; the building must belong to the estate. Returns `201 Created`. |
| Update a unit | `PUT /api/v1/estates/{estateId}/units/{id}` | Estate manager | Updates unit details without changing its building. Returns the updated unit. |
| Delete a unit | `DELETE /api/v1/estates/{estateId}/units/{id}` | Estate manager | Returns a success message if the deletion succeeds. Deletion behavior depends on associated records; see [Deletion behavior](#deletion-behavior). |

The list-in-building endpoint does not currently guarantee the floor-then-unit-number ordering specified in the legacy analysis. This implementation gap is listed in the [backlog](../../../backlog/backlog.md).

## Frontend behavior

The housing-unit form supports creation and editing, preselects a building when its ID is supplied, and allows an optional owner to be searched for or cleared. Terrace and garden fields are shown when their corresponding flags are selected. The unit detail page displays the building link, unit attributes, effective owner name, current lease status, current rent, and current PEB score, with separate sections for rooms, PEB history, rent history, leases, boilers, meters, and fire extinguishers. Room operations are detailed in [Room Management](./room-management.md); PEB history, editing, validity, and expiry alerts are detailed in [PEB Score Management](./peb-score-management.md). Deletion requires confirmation; a successful deletion returns the user to the parent building.

## Validation and computed fields

- Unit number is required and limited to 20 characters. The service checks case-insensitive uniqueness within a building; duplicates produce `409 Conflict`.
- Floor is required and constrained to -10 through 100 inclusive.
- Landing number is limited to 10 characters. Total surface is optional, but if provided it must be greater than zero.
- Terrace and garden orientations are optional and limited to N, S, E, W, NE, NW, SE, or SW.
- When a unit is created or updated with `hasTerrace = false` or `hasGarden = false`, the corresponding surface and orientation are cleared.
- When updating a unit, setting either outdoor-space flag to true requires its surface to be greater than zero. Creation does not apply this conditional check, and the form does not mark the surface fields as required. This difference remains an [open question](../../../backlog/open_questions.md#housing-units-and-leases).
- A supplied owner ID must identify an existing person. Estate consistency is not checked.
- The response's `effectiveOwnerName` uses the housing-unit owner's name if present and otherwise the building owner's name. `roomCount`, current rent, latest PEB score, and active or draft lease status are also computed for the response.

Request validation failures return `400 Bad Request`. Duplicate unit numbers return `409 Conflict` with the duplicate error shape. A missing unit returns `404 Not Found`.

## Deletion behavior

Before deletion, the service blocks a unit that has rooms or fire extinguishers and returns `409 Conflict` with `roomCount` and `fireExtinguisherCount`. Other related records are not handled uniformly:

- Room references are configured to cascade in the database, but the service blocks deletion when rooms exist.
- PEB history and rent history cascade when a unit is deleted.
- A lease reference restricts deletion in the database; the service does not pre-check it or translate that database failure into a housing-unit-specific response.
- Financial transaction links are set to null when the unit is deleted.
- Fire-extinguisher references restrict deletion and are also checked by the service.
- Boilers and meters use generic owner-type/owner-ID fields without a foreign key to `housing_unit`; the deletion service does not clean up or reject these references.

The intended retention and deletion policy for these associated records is unresolved; see [Open Questions](../../../backlog/open_questions.md#housing-units-and-leases).

## Implementation references

- Routes and authorization: [HousingUnitController](../../../backend/src/main/java/com/immocare/controller/HousingUnitController.java)
- Estate-scoped operations, validation, enrichment, and deletion: [HousingUnitService](../../../backend/src/main/java/com/immocare/service/HousingUnitService.java)
- Request and response models: [CreateHousingUnitRequest](../../../backend/src/main/java/com/immocare/model/dto/CreateHousingUnitRequest.java), [UpdateHousingUnitRequest](../../../backend/src/main/java/com/immocare/model/dto/UpdateHousingUnitRequest.java), and [HousingUnitDTO](../../../backend/src/main/java/com/immocare/model/dto/HousingUnitDTO.java)
- Persistence: [HousingUnit](../../../backend/src/main/java/com/immocare/model/entity/HousingUnit.java) and migration [V006__uc006_manage_housing_units.sql](../../../backend/src/main/resources/db/migration/V006__uc006_manage_housing_units.sql)
- Frontend service and views: [HousingUnitService](../../../frontend/src/app/core/services/housing-unit.service.ts), [housing-unit form](../../../frontend/src/app/features/housing-unit/housing-unit-form/), and [housing-unit details](../../../frontend/src/app/features/housing-unit/housing-unit-details/)
