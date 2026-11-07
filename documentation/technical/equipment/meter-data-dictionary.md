---
title: Meter Data Dictionary
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "Meter, MeterDTO, AddMeterRequest, ReplaceMeterRequest, RemoveMeterRequest, V009__uc009_manage_meters.sql"
---

# Meter Data Dictionary

The `meter` table stores utility meters associated with a building or housing unit. The owner is represented by a type and numeric ID pair rather than a foreign key. See [Meter Management](./meter-management.md) for API behavior and the [cross-cutting data model](../cross-cutting/data-model.md) for its place in the persistence model.

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `type` | `VARCHAR(20)` | No | Check constraint permits `WATER`, `GAS`, or `ELECTRICITY`. |
| `meter_number` | `VARCHAR(50)` | No | Meter's identifying number; see [BR-EQ-013](../../business/equipment/rules/equipment.md). |
| `label` | `VARCHAR(100)` | Yes | Optional display label. |
| `ean_code` | `VARCHAR(18)` | Yes | Optional in the schema; see [BR-EQ-004](../../business/equipment/rules/equipment.md) for the application-level requirement. |
| `installation_number` | `VARCHAR(50)` | Yes | Optional in the schema; see [BR-EQ-005](../../business/equipment/rules/equipment.md) for the application-level requirement. |
| `customer_number` | `VARCHAR(50)` | Yes | Optional in the schema; see [BR-EQ-006](../../business/equipment/rules/equipment.md) for the application-level requirement. |
| `owner_type` | `VARCHAR(20)` | No | Check constraint permits `HOUSING_UNIT` or `BUILDING`. |
| `owner_id` | `BIGINT` | No | ID of the owner specified by `owner_type`; no foreign key is declared. |
| `start_date` | `DATE` | No | Start of the meter's use period; see [BR-EQ-003](../../business/equipment/rules/equipment.md) for the application-level date rule. |
| `end_date` | `DATE` | Yes | Null means active. The database requires a non-null end date to be on or after `start_date`; see [BR-EQ-007](../../business/equipment/rules/equipment.md) for the application-level date rule. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp in the database and is populated by the entity lifecycle callback. |

The schema has no uniqueness constraint on meter numbers, EAN codes, or active meters by owner and type. Meter status is derived from `end_date`; there is no separate status column. The application-level rules are defined in [Equipment Rules](../../business/equipment/rules/equipment.md).

The optional replacement `reason` request field is not a persisted meter field. Its intended retention is unresolved; see [open questions](../../../backlog/open_questions.md).

## Sources

- [Meter entity](../../../backend/src/main/java/com/immocare/model/entity/Meter.java)
- [Add request](../../../backend/src/main/java/com/immocare/model/dto/AddMeterRequest.java)
- [Replacement request](../../../backend/src/main/java/com/immocare/model/dto/ReplaceMeterRequest.java)
- [Close request](../../../backend/src/main/java/com/immocare/model/dto/RemoveMeterRequest.java)
- [Meter migration](../../../backend/src/main/resources/db/migration/V009__uc009_manage_meters.sql)
