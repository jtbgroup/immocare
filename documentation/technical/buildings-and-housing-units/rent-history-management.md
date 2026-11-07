---
title: Housing-Unit Rent History Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "RentHistoryController, RentHistoryService, RentHistoryRepository, RentHistoryMapper, RentHistory, RentHistoryDTO, SetRentRequest, HousingUnitRepository, RentSectionComponent, rent-section.component.html, RentService, rent.model.ts, LeaseController, LeaseService, LeaseRentAdjustment, LeaseRentAdjustmentRepository, AdjustRentRequest, AlertService, V010__uc010_manage_rents.sql, V014__uc014_manage_leases.sql"
---

# Housing-Unit Rent History Management

This page describes the theoretical monthly-rent timeline attached directly to a housing unit. It is a basis for setting, but is not itself, the contractual rent recorded on a lease ([BR-HUNIT-027](../../business/buildings-and-housing-units/rules/housing-units.md)). Persisted fields are listed in the [Housing Unit Data Dictionary](./housing-unit-data-dictionary.md).

## API and access

All routes are under `/api/v1/estates/{estateId}/housing-units/{unitId}/rents`.

| Operation | Method and suffix | Access | Implemented behavior |
|---|---|---|---|
| List rent history | `GET` | Estate member | Returns records ordered by effective-from date descending. |
| Read current rent | `GET /current` | Estate member | Returns the record whose `effectiveTo` is null, or `204 No Content` when none exists. |
| Add rent record | `POST` | Estate manager | Creates a record and recalculates its effective-to date and, when it becomes current, closes the former current record. Returns `201`. |
| Edit rent record | `PUT /{rentId}` | Estate manager | Changes the amount, effective-from date, and notes, then updates neighboring dates. |
| Delete rent record | `DELETE /{rentId}` | Estate manager | Deletes the record and assigns its end date to the closest earlier record, if one exists. Returns `204`. |

Platform administrators satisfy the estate-manager check through the shared estate security policy.

The controller checks membership or manager access for the route estate. The service verifies that the housing unit exists but does not verify that it belongs to that estate. Rent-record edits and deletions verify that the record belongs to the unit ID in the route, but not that the unit belongs to the route estate. This estate-scope gap is tracked in the [backlog](../../../backlog/backlog.md).

## Rent timeline behavior

- Each record has an amount, effective-from date, optional effective-to date, and optional notes. A null effective-to date is treated as current.
- When a new record is later than all existing records, the previous current record ends the day before it and the new record becomes current.
- When a new record is inserted before a later record, its end date is set to the day before that later record starts. The service does **not** shorten the preceding record in this case, so the resulting periods can overlap. Duplicate effective-from dates are not explicitly rejected. These timeline inconsistencies are tracked in the [backlog](../../../backlog/backlog.md).
- Editing a record updates its amount, date, and notes. The service recalculates the nearest preceding record and the edited record's end date using neighboring effective-from dates.
- Deleting a record assigns its end date to the nearest earlier record; deleting the current record therefore makes that earlier record current.

The request requires a monthly rent greater than zero, an effective-from date, and accepts at most 500 characters of optional notes. An effective-from date more than one year in the future is rejected ([BR-HUNIT-025](../../business/buildings-and-housing-units/rules/housing-units.md) and [BR-HUNIT-026](../../business/buildings-and-housing-units/rules/housing-units.md)).

The API response calculates duration as whole calendar months between the effective-from date and effective-to date, or today for a current record. The related historical requirements disagree on whether 2024-01-01 through 2024-06-30 is five or six months; the current calculation returns five. The intended duration convention remains an [open question](../../../backlog/open_questions.md#housing-units-and-leases).

## Interface behavior and differences

The housing-unit page shows a current-rent card, a history table with dates, duration, amount changes, and notes, plus increase/decrease and total-change summaries. Users can edit and delete individual history records. Optional note templates are available. The current-rent card displays whole euros, while the history table displays two decimal places. The optional trend chart described in the earlier requirements is not present.

The page does not display the specified warning when the new amount equals the selected record's existing amount; the amount is not blocked. A missing current record shows an empty state and a form to add rent. Failures loading current rent or history are not consistently surfaced to the user.

The rent history belongs to a housing unit, not a lease. A lease separately stores contractual monthly rent and charges; its adjustment records store the old and new values, field, reason, and effective date. Lease adjustments can affect rent or charges and are shown in the lease details. Lease indexation alerts are based on the lease and are separate from this housing-unit rent timeline; the shared alert list does not generate alerts from housing-unit rent-history records. See [Lease Technical References](../leases/README.md) and the cross-cutting [Alerts](../../business/cross-cutting/concepts/alerts.md) concept.

The business distinction between housing-unit rent and lease rent is confirmed in [BR-HUNIT-027](../../business/buildings-and-housing-units/rules/housing-units.md). Whether rent-history entries should be editable or deletable remains an [open question](../../../backlog/open_questions.md#housing-units-and-leases).

## Sources

- [Rent-history controller](../../../backend/src/main/java/com/immocare/controller/RentHistoryController.java)
- [Rent-history service](../../../backend/src/main/java/com/immocare/service/RentHistoryService.java)
- [Rent-history entity](../../../backend/src/main/java/com/immocare/model/entity/RentHistory.java)
- [Rent-history request](../../../backend/src/main/java/com/immocare/model/dto/SetRentRequest.java)
- [Rent-history response](../../../backend/src/main/java/com/immocare/model/dto/RentHistoryDTO.java)
- [Rent-history mapper](../../../backend/src/main/java/com/immocare/mapper/RentHistoryMapper.java)
- [Rent migration](../../../backend/src/main/resources/db/migration/V010__uc010_manage_rents.sql)
- [Housing-unit rent component](../../../frontend/src/app/features/housing-unit/components/rent-section/rent-section.component.ts)
- [Housing-unit rent template](../../../frontend/src/app/features/housing-unit/components/rent-section/rent-section.component.html)
- [Rent frontend service](../../../frontend/src/app/core/services/rent.service.ts)
- [Lease rent-adjustment entity](../../../backend/src/main/java/com/immocare/model/entity/LeaseRentAdjustment.java)
- [Lease migration](../../../backend/src/main/resources/db/migration/V014__uc014_manage_leases.sql)
