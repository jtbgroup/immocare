---
title: Meter Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "MeterController, MeterService, MeterRepository, MeterSectionComponent, meter-section.component.html, meter.model.ts, AlertService, V009__uc009_manage_meters.sql"
---

# Meter Management

This page describes the implemented meter API, lifecycle, authorization, validation, and interface behavior. The equipment domain rules are listed in [Equipment Rules](../../business/equipment/rules/equipment.md); persisted fields are listed in the [Meter Data Dictionary](./meter-data-dictionary.md).

## API

The estate-scoped endpoints support meters owned by buildings or housing units.

| Operation | Endpoint | Access | Behavior |
|---|---|---|---|
| List building meters | `GET /api/v1/estates/{estateId}/buildings/{buildingId}/meters` | Estate member | Returns history by default; `?status=active` returns active meters only. |
| Add building meter | `POST /api/v1/estates/{estateId}/buildings/{buildingId}/meters` | Estate manager | Creates a meter and returns `201`. |
| Replace building meter | `PUT /api/v1/estates/{estateId}/buildings/{buildingId}/meters/{meterId}/replace` | Estate manager | Closes the referenced active meter and creates its replacement; returns `200`. |
| Close building meter | `DELETE /api/v1/estates/{estateId}/buildings/{buildingId}/meters/{meterId}` | Estate manager | Accepts an `endDate` request body, closes the meter, and returns `204`. |
| List housing-unit meters | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/meters` | Estate member | Returns history by default; `?status=active` returns active meters only. |
| Add housing-unit meter | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/meters` | Estate manager | Creates a meter and returns `201`. |
| Replace housing-unit meter | `PUT /api/v1/estates/{estateId}/housing-units/{unitId}/meters/{meterId}/replace` | Estate manager | Closes the referenced active meter and creates its replacement; returns `200`. |
| Close housing-unit meter | `DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/meters/{meterId}` | Estate manager | Accepts an `endDate` request body, closes the meter, and returns `204`. |
| Search active meters | `GET /api/v1/meters/search` | Authenticated | Searches active meters by meter number, label, or EAN; optionally filters by building and returns at most 20 results. |
| Count linked transactions | `GET /api/v1/meters/{meterId}/transaction-count` | Authenticated | Returns the count of transaction links for the meter ID. |

Platform administrators satisfy the manager check through the shared estate security policy. The search and transaction-count endpoints have no estate route or estate-specific authorization check. Search may return active meters from every estate; a query shorter than two characters does not narrow results by text.

The estate-scoped endpoints check the caller's role for the route's `estateId`, but the service only checks whether the specified building or housing unit exists. It does not verify that this owner belongs to that estate. Replacement and close look up an active meter by meter ID without checking that the meter belongs to the owner in the route. Consequently, an authorized caller may access or mutate another estate's data by supplying mismatched route identifiers. Track remediation in the [backlog](../../../backlog/backlog.md).

## Lifecycle and validation

The service applies [BR-EQ-002 through BR-EQ-007 and BR-EQ-013](../../business/equipment/rules/equipment.md) for meter types, required attributes, meter numbers, and lifecycle dates. The database also constrains meter type and owner type and prevents an end date before a start date; see the [Meter Data Dictionary](./meter-data-dictionary.md) for persistence details.

An active meter has no end date ([BR-EQ-010](../../business/equipment/rules/equipment.md)). Active-meter results are sorted by type and then start date descending. History includes active and closed meter records, sorted by start date descending. Replacing a meter preserves the previous meter as a closed record; this is the current meter lifecycle history. The data model does not record meter readings or meter maintenance events.

No meter validity or maintenance interval, nor any related alert, is currently implemented. The confirmed expectation that applicable equipment intervals generate alerts is recorded in [BR-EQ-032](../../business/equipment/rules/equipment.md); whether meters have an applicable interval remains an open question.

Replacement follows [BR-EQ-008 and BR-EQ-009](../../business/equipment/rules/equipment.md): it is transactional, sets the existing meter's end date to the replacement start date, and creates a new meter of the same type. Equality between the old and new start dates is allowed. The implementation permits multiple active meters of the same type for one owner ([BR-EQ-011](../../business/equipment/rules/equipment.md)). Meter details are not edited after creation; closing updates the end date ([BR-EQ-012](../../business/equipment/rules/equipment.md)).

Replacement requests can include an optional `reason`, but the service does not store or return it. The request accepts a plain string; it does not validate the reason against the interface's listed choices.

## Interface behavior and known gaps

The meter section groups active meters by type. The history control is shown only when there is an active meter or history has already been loaded. When all meters are closed, history has not yet been loaded and the control is hidden, so the interface provides no way to request that history. History-load failures are not explicitly presented to the user.

The optional replacement reason is submitted by the interface but is discarded by the API. Displayed duration is calculated by subtracting calendar month values and does not account for the day of the month. These behaviors and the estate-scoping gaps above are tracked in the [backlog](../../../backlog/backlog.md); the reason-retention and duration policies are also listed in [open questions](../../../backlog/open_questions.md).

## Sources

- [MeterController](../../../backend/src/main/java/com/immocare/controller/MeterController.java)
- [MeterService](../../../backend/src/main/java/com/immocare/service/MeterService.java)
- [Meter](../../../backend/src/main/java/com/immocare/model/entity/Meter.java)
- [Meter requests](../../../backend/src/main/java/com/immocare/model/dto/AddMeterRequest.java)
- [Replacement request](../../../backend/src/main/java/com/immocare/model/dto/ReplaceMeterRequest.java)
- [Meter migration](../../../backend/src/main/resources/db/migration/V009__uc009_manage_meters.sql)
- [Meter section component](../../../frontend/src/app/shared/components/meter-section/meter-section.component.ts)
- [Meter section template](../../../frontend/src/app/shared/components/meter-section/meter-section.component.html)
- [Meter model](../../../frontend/src/app/models/meter.model.ts)
- [Cross-domain alert service](../../../backend/src/main/java/com/immocare/service/AlertService.java)
