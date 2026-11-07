---
title: Fire Extinguisher Data Dictionary
type: technical reference
status: current
last_verified: 2026-10-28
verified_against: "FireExtinguisher, FireExtinguisherRevision, SaveFireExtinguisherRequest, AddRevisionRequest, V012__uc012_manage_fire_extinguishers.sql, V017__generalize_fire_extinguisher_ownership.sql"
---

# Fire Extinguisher Data Dictionary

Fire-extinguisher fields and database constraints are listed below. API and lifecycle behavior is described in [Fire Extinguisher Management](./fire-extinguisher-management.md).

## `fire_extinguisher`

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `building_id` | `BIGINT` | Yes | References `building`; deletion of a referenced building cascades to the extinguisher. |
| `unit_id` | `BIGINT` | Yes | References `housing_unit`; deletion of a referenced unit is restricted. |
| `identification_number` | `VARCHAR(50)` | No | Required identifier; see [BR-EQ-023 and BR-EQ-024](../../business/equipment/rules/equipment.md). |
| `notes` | `TEXT` | Yes | Optional notes; the request limits notes to 2,000 characters. |
| `created_by` | `BIGINT` | Yes | References `app_user`; set to null when that user is deleted. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp. |
| `updated_by` | `BIGINT` | Yes | References `app_user`; set to null when that user is deleted. |
| `updated_at` | `TIMESTAMP` | No | Defaults to the current timestamp; updated by the entity lifecycle callback. |

Migration `V017` makes the two owner columns nullable individually and adds a check requiring exactly one of them to be non-null. It replaces the original building-only uniqueness constraint with separate unique indexes for building-owned and unit-owned identifiers. Those database indexes use the database's ordinary case-sensitive comparison; application validation also checks for duplicates ignoring case.

## `fire_extinguisher_revision`

| Field | Database type | Nullable | Constraints and meaning |
|---|---|---:|---|
| `id` | `BIGINT` | No | Identity primary key. |
| `fire_extinguisher_id` | `BIGINT` | No | References `fire_extinguisher`; deletion cascades with the extinguisher. |
| `revision_date` | `DATE` | No | Revision date; the application rejects future dates ([BR-EQ-025](../../business/equipment/rules/equipment.md)). |
| `notes` | `TEXT` | Yes | Optional notes; the request limits notes to 2,000 characters. |
| `created_at` | `TIMESTAMP` | No | Defaults to the current timestamp. |

## Sources

- [Fire-extinguisher entity](../../../backend/src/main/java/com/immocare/model/entity/FireExtinguisher.java)
- [Revision entity](../../../backend/src/main/java/com/immocare/model/entity/FireExtinguisherRevision.java)
- [Save request](../../../backend/src/main/java/com/immocare/model/dto/SaveFireExtinguisherRequest.java)
- [Revision request](../../../backend/src/main/java/com/immocare/model/dto/AddRevisionRequest.java)
- [Initial migration](../../../backend/src/main/resources/db/migration/V012__uc012_manage_fire_extinguishers.sql)
- [Ownership migration](../../../backend/src/main/resources/db/migration/V017__generalize_fire_extinguisher_ownership.sql)
