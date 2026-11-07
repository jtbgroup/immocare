---
title: Boiler Data Dictionary
type: technical reference
status: current
last_verified: 2026-10-28
verified_against: "Boiler, BoilerServiceRecord, BoilerServiceValidityRule, SaveBoilerRequest, AddBoilerServiceRecordRequest, AddBoilerServiceValidityRuleRequest, V011__uc011_manage_boilers.sql"
---

# Boiler Data Dictionary

The boiler schema stores equipment, service records, and estate-specific validity rules. API and lifecycle behavior is documented in [Boiler Management](./boiler-management.md).

## `boiler`

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `owner_type` | `VARCHAR(20)` | No | Check constraint permits `HOUSING_UNIT` or `BUILDING`. |
| `owner_id` | `BIGINT` | No | ID of the owner identified by `owner_type`; no foreign key is declared. |
| `brand` | `VARCHAR(100)` | Yes | Optional brand. |
| `model` | `VARCHAR(100)` | Yes | Optional model. |
| `serial_number` | `VARCHAR(100)` | Yes | Optional serial number. |
| `fuel_type` | `VARCHAR(20)` | No | Check constraint permits `GAS`, `OIL`, `ELECTRIC`, or `HEAT_PUMP`. |
| `installation_date` | `DATE` | No | Installation date; see [BR-EQ-015](../../business/equipment/rules/equipment.md). |
| `out_of_service_date` | `DATE` | Yes | Date the boiler was recorded as out of service; the API does not use it to identify or filter an active boiler. |
| `removal_date` | `DATE` | Yes | Present in the migration but not mapped by the current entity or API. |
| `last_service_date` | `DATE` | Yes | Flat service-date field that can be supplied in boiler requests and is updated when a service record is added; service validation is described in [BR-EQ-016 and BR-EQ-018](../../business/equipment/rules/equipment.md). |
| `next_service_date` | `DATE` | Yes | Flat service-validity field that can be supplied in boiler requests and is updated when a service record is added; see [Boiler Management](./boiler-management.md). |
| `notes` | `TEXT` | Yes | Optional notes. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp. |
| `updated_at` | `TIMESTAMP` | No | Set by the entity lifecycle callback. |
| `created_by` | `BIGINT` | Yes | References `app_user`; set to null when that user is deleted. |

The schema has no unique constraint limiting the number of boilers for an owner, and no foreign key from the polymorphic owner ID to either owner table.

## `boiler_service`

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `boiler_id` | `BIGINT` | No | References `boiler`; deletion of a referenced boiler is restricted. |
| `service_date` | `DATE` | No | Service date; the application rejects future dates ([BR-EQ-019](../../business/equipment/rules/equipment.md)). |
| `valid_until` | `DATE` | No | Stored validity end date; application calculation and override behavior are documented in [BR-EQ-020 and BR-EQ-021](../../business/equipment/rules/equipment.md). |
| `notes` | `TEXT` | Yes | Optional notes. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp. |
| `created_by` | `BIGINT` | Yes | References `app_user`; set to null when that user is deleted. |

## `boiler_service_validity_rule`

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `estate_id` | `UUID` | No | References the estate that owns the rule. |
| `valid_from` | `DATE` | No | Effective date; unique per estate. |
| `validity_duration_months` | `INTEGER` | No | Must be greater than zero. |
| `description` | `TEXT` | Yes | Optional description. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp. |
| `created_by` | `BIGINT` | Yes | References `app_user`; set to null when that user is deleted. |

The application creates a default rule for each new estate with `valid_from` 1900-01-01 and a 24-month duration. The database enforces uniqueness on `(estate_id, valid_from)` and requires a positive duration.

## Sources

- [Boiler entity](../../../backend/src/main/java/com/immocare/model/entity/Boiler.java)
- [Boiler service record entity](../../../backend/src/main/java/com/immocare/model/entity/BoilerServiceRecord.java)
- [Validity-rule entity](../../../backend/src/main/java/com/immocare/model/entity/BoilerServiceValidityRule.java)
- [Boiler request](../../../backend/src/main/java/com/immocare/model/dto/SaveBoilerRequest.java)
- [Boiler service-record request](../../../backend/src/main/java/com/immocare/model/dto/BoilerServiceDTOs.java)
- [Validity-rule request](../../../backend/src/main/java/com/immocare/model/dto/EstateConfigDTOs.java)
- [Boiler migration](../../../backend/src/main/resources/db/migration/V011__uc011_manage_boilers.sql)
