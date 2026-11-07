---
title: Fire Extinguisher Management
type: technical reference
status: current
last_verified: 2026-10-28
verified_against: "FireExtinguisherController, FireExtinguisherService, FireExtinguisherRepository, FireExtinguisherSectionComponent, fire-extinguisher-section.component.html, V012__uc012_manage_fire_extinguishers.sql, V017__generalize_fire_extinguisher_ownership.sql"
---

# Fire Extinguisher Management

This page describes the implemented fire-extinguisher API, ownership, revision behavior, and interface. Equipment rules are listed in [Equipment Rules](../../business/equipment/rules/equipment.md); persisted fields are listed in the [Fire Extinguisher Data Dictionary](./fire-extinguisher-data-dictionary.md).

## API

| Operation | Endpoint | Access | Implemented behavior |
|---|---|---|---|
| List building-owned extinguishers | `GET /api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers` | Estate member | Returns records owned directly by the building, ordered by identification number. It does not include extinguishers owned by the building's housing units. |
| List housing-unit extinguishers | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/fire-extinguishers` | Estate member | Returns records owned by that housing unit, ordered by identification number. |
| Read extinguisher | `GET /api/v1/estates/{estateId}/fire-extinguishers/{id}` | Estate member | Returns the extinguisher and its revision history. |
| Create for building | `POST /api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers` | Estate manager | Creates a building-owned record; if `unitId` is supplied for a unit in the same building, creates it as unit-owned instead. |
| Create for housing unit | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/fire-extinguishers` | Estate manager | Creates a housing-unit-owned record. |
| Update extinguisher | `PUT /api/v1/estates/{estateId}/fire-extinguishers/{id}` | Estate manager | Updates identification number and notes; changing the owner is not supported. |
| Delete extinguisher | `DELETE /api/v1/estates/{estateId}/fire-extinguishers/{id}` | Estate manager | Deletes the record and its revision records; returns `204`. |
| Add revision | `POST /api/v1/estates/{estateId}/fire-extinguishers/{id}/revisions` | Estate manager | Adds a revision and returns the extinguisher with its revisions. |
| Delete revision | `DELETE /api/v1/estates/{estateId}/fire-extinguishers/{extId}/revisions/{revId}` | Estate manager | Deletes the revision ID supplied, but does not check that the revision belongs to the extinguisher ID in the route. |
| Search | `GET /api/v1/fire-extinguishers/search` | Authenticated user | Searches identification numbers, optionally filtered by building, and returns at most 20 results. |
| Count linked transactions | `GET /api/v1/fire-extinguishers/{id}/transaction-count` | Authenticated user | Returns the transaction-link count for the extinguisher ID. |

Estate-scoped operations verify that the building or housing unit is in the requested estate and that a unit selected during creation belongs to the route building. The search and transaction-count endpoints are authenticated but not estate-scoped. The revision-deletion operation verifies access to the route extinguisher but not that the separately supplied revision belongs to it; this can delete another extinguisher's revision. These gaps are tracked in the [backlog](../../../backlog/backlog.md).

## Ownership and lifecycle

The current schema permits exactly one management context per extinguisher: a building or a housing unit ([BR-EQ-026](../../business/equipment/rules/equipment.md)). For a unit-owned extinguisher, its parent building is resolved from the housing unit. A building-route create request may include a unit ID, but the service verifies the unit is in that building and stores the extinguisher as unit-owned, not as simultaneously building- and unit-owned.

The estate dashboard counts an extinguisher as needing attention if it has no revisions or its latest revision date is more than one year old ([BR-EQ-031](../../business/equipment/rules/equipment.md)). This is a fixed dashboard count, not a stored validity date or an alert in the shared alert list; the intended inspection interval is unresolved.

The API requires a nonblank identification number of at most 50 characters. The service rejects a duplicate number ignoring case within the same building or housing unit ([BR-EQ-023 and BR-EQ-024](../../business/equipment/rules/equipment.md)). The database's per-owner unique indexes are case-sensitive, so the case-insensitive behavior comes from application validation.

Revision records require a date and may include notes; future dates are rejected ([BR-EQ-025](../../business/equipment/rules/equipment.md)). Revisions are returned in descending revision-date and creation-time order. They can be deleted individually through the API. Deleting the extinguisher cascades to its revisions ([BR-EQ-027 and BR-EQ-028](../../business/equipment/rules/equipment.md)).

The historical analysis describes an extinguisher attached to a building with an optional housing-unit assignment and calls the revision history append-only, while a related requirement permits deleting individual revisions. The current schema instead enforces one owner context, and the API supports revision deletion. These decisions need business clarification; see [open questions](../../../backlog/open_questions.md).

The current uniqueness check is scoped to the direct building or housing-unit owner. Whether identification numbers must instead be unique across a building and all its housing units remains unresolved; see [open questions](../../../backlog/open_questions.md).

## Interface behavior and known gaps

The interface selects an owner context from the page where the section appears and does not provide a unit-assignment dropdown. On a building page, the list query returns only building-owned extinguishers; unit-owned extinguishers are available through the housing-unit query instead.

The revision panel shows a delete action for each record. Failed revision deletions are silently dismissed by the component. Deleting a boiler or extinguisher does not remove its transaction-asset links: `transaction_asset_link.asset_id` has no foreign key to the equipment record. These behaviors are tracked in the [backlog](../../../backlog/backlog.md).

## Sources

- [Fire-extinguisher controller](../../../backend/src/main/java/com/immocare/controller/FireExtinguisherController.java)
- [Fire-extinguisher service](../../../backend/src/main/java/com/immocare/service/FireExtinguisherService.java)
- [Fire-extinguisher repository](../../../backend/src/main/java/com/immocare/repository/FireExtinguisherRepository.java)
- [Fire-extinguisher entity](../../../backend/src/main/java/com/immocare/model/entity/FireExtinguisher.java)
- [Revision entity](../../../backend/src/main/java/com/immocare/model/entity/FireExtinguisherRevision.java)
- [Initial migration](../../../backend/src/main/resources/db/migration/V012__uc012_manage_fire_extinguishers.sql)
- [Ownership migration](../../../backend/src/main/resources/db/migration/V017__generalize_fire_extinguisher_ownership.sql)
- [Fire-extinguisher section component](../../../frontend/src/app/features/building/fire-extinguisher-section/fire-extinguisher-section.component.ts)
- [Fire-extinguisher section template](../../../frontend/src/app/features/building/fire-extinguisher-section/fire-extinguisher-section.component.html)
