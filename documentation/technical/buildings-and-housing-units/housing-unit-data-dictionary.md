---
title: Housing Unit Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "HousingUnit, Room, PebScoreHistory, V006__uc006_manage_housing_units.sql, V007__uc007_manage_rooms.sql, V008__uc008_manage_peb_scores.sql"
---

# Housing Unit Data Dictionary

This page documents persisted housing-unit, room, and PEB-history fields and database constraints. Business concepts are defined in [Buildings and Housing Units](../../business/buildings-and-housing-units/concepts/buildings-and-housing-units.md); application behavior is described in [Housing Unit Management](./housing-unit-management.md), [Room Management](./room-management.md), and [PEB Score Management](./peb-score-management.md). Relationships across the model are shown in the cross-cutting [data model](../cross-cutting/data-model.md).

## Entity: `housing_unit`

**Purpose:** Represents a dwelling or other unit within a building.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | Primary key, generated identity | No | Generated | Housing-unit identifier |
| `building_id` | `BIGINT` | Foreign key to `building(id)`, `ON DELETE CASCADE`; participates in unique constraint with `unit_number` | No | — | Parent building |
| `unit_number` | `VARCHAR(20)` | Unique with `building_id` in the database; database uniqueness is case-sensitive | No | — | Unit label within its building |
| `floor` | `INTEGER` | Check constraint: -10 through 100 | No | — | Floor number |
| `landing_number` | `VARCHAR(10)` | — | Yes | — | Optional landing or entrance designation |
| `total_surface` | `DECIMAL(7,2)` | Check constraint: greater than zero when non-null | Yes | — | Recorded total surface |
| `has_terrace` | `BOOLEAN` | — | No | `FALSE` | Whether the unit is recorded as having a terrace |
| `terrace_surface` | `DECIMAL(7,2)` | Check constraint: greater than zero when non-null | Yes | — | Recorded terrace surface |
| `terrace_orientation` | `VARCHAR(2)` | Check constraint: N, S, E, W, NE, NW, SE, or SW | Yes | — | Optional terrace orientation |
| `has_garden` | `BOOLEAN` | — | No | `FALSE` | Whether the unit is recorded as having a garden |
| `garden_surface` | `DECIMAL(7,2)` | Check constraint: greater than zero when non-null | Yes | — | Recorded garden surface |
| `garden_orientation` | `VARCHAR(2)` | Check constraint: N, S, E, W, NE, NW, SE, or SW | Yes | — | Optional garden orientation |
| `owner_id` | `BIGINT` | Foreign key to `person(id)`, `ON DELETE SET NULL` | Yes | — | Optional direct owner |
| `created_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as creator |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Creation time |
| `updated_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as last updater |
| `updated_at` | `TIMESTAMP` | — | No | Current timestamp | Most recent update time |

The database does not enforce that a terrace or garden surface is present when its corresponding flag is true. The application requires a positive surface on updates, but not consistently on creation; see [Housing Unit Management](./housing-unit-management.md) and [Open Questions](../../../backlog/open_questions.md#housing-units-and-leases).

## Entity: `room`

**Purpose:** Records a typed room or space within a housing unit.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | Primary key, generated identity | No | Generated | Room identifier |
| `housing_unit_id` | `BIGINT` | Foreign key to `housing_unit(id)`, `ON DELETE CASCADE` | No | — | Housing unit containing the room |
| `room_type` | `VARCHAR(20)` | Check constraint: `LIVING_ROOM`, `BEDROOM`, `KITCHEN`, `BATHROOM`, `TOILET`, `HALLWAY`, `STORAGE`, `OFFICE`, `DINING_ROOM`, or `OTHER` | No | — | Room classification |
| `approximate_surface` | `DECIMAL(6,2)` | Check constraint: greater than zero and less than 1,000 | No | — | Approximate room surface |
| `created_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as creator |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Creation time |
| `updated_by` | `BIGINT` | Foreign key to `app_user(id)`, `ON DELETE SET NULL` | Yes | — | Account recorded as last updater |
| `updated_at` | `TIMESTAMP` | — | No | Current timestamp | Most recent update time |

`RoomListResponse.totalSurface` is calculated from room surfaces and is not a persisted field. It is distinct from `housing_unit.total_surface`; whether the room total should update that field is unresolved in [Open Questions](../../../backlog/open_questions.md#housing-units-and-leases). Application-level rules are documented in the [housing-unit rules](../../business/buildings-and-housing-units/rules/housing-units.md).

Persisted room fields and constraints are based on migration `V007__uc007_manage_rooms.sql` and entity `Room`.

## Entity: `peb_score_history`

**Purpose:** Stores dated energy performance ratings and optional certificate details for a housing unit.

| Attribute | Data type | Persisted constraints | Nullable | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | Primary key, generated identity | No | Generated | PEB history-record identifier |
| `housing_unit_id` | `BIGINT` | Foreign key to `housing_unit(id)`, `ON DELETE CASCADE` | No | — | Housing unit the record belongs to |
| `peb_score` | `VARCHAR(10)` | Check constraint: `A_PLUS_PLUS`, `A_PLUS`, `A`, `B`, `C`, `D`, `E`, `F`, or `G` | No | — | Recorded energy performance grade |
| `score_date` | `DATE` | — | No | — | Date of the rating |
| `certificate_number` | `VARCHAR(50)` | — | Yes | — | Optional certificate identifier |
| `valid_until` | `DATE` | — | Yes | — | Optional certificate validity end date |
| `created_at` | `TIMESTAMP` | — | No | Current timestamp | Record creation time |

The database does not enforce that `score_date` is not in the future or that `valid_until` follows it; those checks are performed by the application. Application behavior is documented in [PEB Score Management](./peb-score-management.md), and the corresponding constraints are listed in the [housing-unit rules](../../business/buildings-and-housing-units/rules/housing-units.md).

## Sources

Housing-unit fields and constraints are based on migration `V006__uc006_manage_housing_units.sql` and entity `HousingUnit`. Room fields and constraints are based on migration `V007__uc007_manage_rooms.sql` and entity `Room`. PEB-history fields and constraints are based on migration `V008__uc008_manage_peb_scores.sql` and entity `PebScoreHistory`. Application-level constraints are documented in the [housing-unit rules](../../business/buildings-and-housing-units/rules/housing-units.md).
