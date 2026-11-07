---
title: Boiler Management
type: technical reference
status: current
last_verified: 2026-10-28
verified_against: "BoilerController, BoilerServiceController, BoilerService, BoilerServiceHistoryService, BoilerServiceValidityRuleService, EstateConfigController, AlertController, BoilerSectionComponent, boiler-section.component.html, V011__uc011_manage_boilers.sql"
---

# Boiler Management

This page describes the implemented boiler APIs, service records, validity rules, alerts, and interface behavior. Equipment rules are defined in [Equipment Rules](../../business/equipment/rules/equipment.md); persisted fields are listed in the [Boiler Data Dictionary](./boiler-data-dictionary.md).

## API

| Operation | Endpoint | Access | Implemented behavior |
|---|---|---|---|
| List housing-unit boilers | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/boilers` | Estate member | Returns all boilers for the owner, ordered by installation date descending; it does not select an active boiler. |
| Create housing-unit boiler | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/boilers` | Estate manager | Creates a boiler and returns `201`. |
| List building boilers | `GET /api/v1/estates/{estateId}/buildings/{buildingId}/boilers` | Estate member | Returns all boilers for the owner, ordered by installation date descending. |
| Create building boiler | `POST /api/v1/estates/{estateId}/buildings/{buildingId}/boilers` | Estate manager | Creates a boiler and returns `201`. |
| Read boiler | `GET /api/v1/estates/{estateId}/boilers/{id}` | Estate member | Returns a boiler by ID. |
| Update boiler | `PUT /api/v1/estates/{estateId}/boilers/{id}` | Estate manager | Replaces the editable boiler fields from the request. |
| Delete boiler | `DELETE /api/v1/estates/{estateId}/boilers/{id}` | Estate manager | Deletes the boiler; the database restricts deletion when service records reference it. |
| List service alerts | `GET /api/v1/estates/{estateId}/boilers/alerts` | Estate member | Intended to list alerts for the estate, but the controller currently passes no estate ID to the service, which therefore returns alerts across estates. |
| List service history | `GET /api/v1/boilers/{boilerId}/services` | Authenticated user | Returns the boiler's service records, ordered by service date descending. |
| Add service record | `POST /api/v1/boilers/{boilerId}/services` | Authenticated user | Adds a service record and returns `201`; the endpoint does not require estate membership or manager access. |
| Search boilers | `GET /api/v1/boilers/search` | Authenticated user | Searches brand, model, or serial number and optionally filters by building; returns at most 20 results. |
| Count linked transactions | `GET /api/v1/boilers/{boilerId}/transaction-count` | Authenticated user | Returns the transaction-link count for the boiler ID. |

Although the main boiler routes contain an `estateId`, the service checks only that a referenced building or housing unit exists. It does not check that the owner belongs to the route estate. Read, update, and delete operations by boiler ID likewise do not check that the boiler belongs to that estate. The service-alert route explicitly passes `null`, bypassing its optional estate filter. The service-history routes have no estate route and use authentication only. Search and transaction-count routes also have no estate-specific authorization. These estate-isolation and access gaps are tracked in the [backlog](../../../backlog/backlog.md).

## Current lifecycle and validation

The application returns every boiler associated with the requested owner and offers create, update, and delete operations. The interface displays this list and offers an add form for any owner; it does not implement a separate active-boiler view or a replacement operation. The database has a nullable `removal_date`, but the entity and API do not map it. The API instead exposes `outOfServiceDate`, which is not used to filter boilers or enforce an active-boiler lifecycle.

The create and update request requires a fuel type and installation date. Fuel type must be `GAS`, `OIL`, `ELECTRIC`, or `HEAT_PUMP`; brand, model, serial number, dates, and notes are otherwise optional subject to the request's length constraints. The service accepts a future installation date and a missing brand ([BR-EQ-029 and BR-EQ-030](../../business/equipment/rules/equipment.md)), although the earlier requirements specify otherwise. It rejects a future last-service date, a last-service date before installation, a next-service date before installation, and a last-service date before the out-of-service date when both are provided. See [BR-EQ-014 through BR-EQ-018](../../business/equipment/rules/equipment.md).

Adding a service record rejects a future service date ([BR-EQ-019](../../business/equipment/rules/equipment.md)). When the request omits `validUntil`, the service calculates it using the most recent validity rule whose `validFrom` is on or before the service date, or adds 24 months if no rule applies ([BR-EQ-020](../../business/equipment/rules/equipment.md)). A supplied `validUntil` is accepted as-is ([BR-EQ-021](../../business/equipment/rules/equipment.md)). Adding a service record also copies the service date and validity end date into the boiler's `lastServiceDate` and `nextServiceDate` fields. Existing records are not recalculated when a new validity rule is added.

New estates receive a seed boiler validity rule effective from 1900-01-01 with a 24-month duration. Estate managers can add further rules; estate members can read the rule history. Rules are ordered by `validFrom` descending, cannot be edited or deleted through the API, and must have a unique `validFrom` date within the estate. The warning period is stored per estate and defaults to three months ([BR-EQ-022](../../business/equipment/rules/equipment.md)).

The UI labels `validUntil` as automatically calculated, but it does not calculate or display a date before submission. The server calculates the date only when the submitted field is empty. The history component silently clears its loading indicator on a failed history request without showing an error.

## Difference from the historical analysis

The earlier requirements describe one active boiler per housing unit, atomic replacement, non-deletable boiler history, required brand, and a non-future installation date. The current API has no replacement endpoint or active-boiler constraint; it allows updates and deletion, treats brand as optional, and accepts future installation dates. The table's `removal_date` is not mapped by the current entity. Confirm the intended lifecycle and required fields before treating the earlier criteria as business rules; see [open questions](../../../backlog/open_questions.md). The resulting implementation gaps are tracked in the [backlog](../../../backlog/backlog.md).

## Sources

- [Boiler controller](../../../backend/src/main/java/com/immocare/controller/BoilerController.java)
- [Boiler service-history controller](../../../backend/src/main/java/com/immocare/controller/BoilerServiceController.java)
- [Boiler service](../../../backend/src/main/java/com/immocare/service/BoilerService.java)
- [Boiler service-history service](../../../backend/src/main/java/com/immocare/service/BoilerServiceHistoryService.java)
- [Validity-rule service](../../../backend/src/main/java/com/immocare/service/BoilerServiceValidityRuleService.java)
- [Estate configuration controller](../../../backend/src/main/java/com/immocare/controller/EstateConfigController.java)
- [Cross-domain alert controller](../../../backend/src/main/java/com/immocare/controller/AlertController.java)
- [Cross-domain alert service](../../../backend/src/main/java/com/immocare/service/AlertService.java)
- [Boiler migration](../../../backend/src/main/resources/db/migration/V011__uc011_manage_boilers.sql)
- [Boiler section component](../../../frontend/src/app/shared/components/boiler-section/boiler-section.component.ts)
- [Boiler section template](../../../frontend/src/app/shared/components/boiler-section/boiler-section.component.html)
