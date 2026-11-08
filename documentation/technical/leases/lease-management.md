---
title: Lease Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "Lease, LeaseTenant, LeaseRentAdjustment, LeaseController, LeaseService, LeaseRentAdjustmentRepository, AlertService, EstateConfigDTOs, V014__uc014_manage_leases.sql, lease.service.ts, lease-section.component.ts, lease-details.component.ts, lease-form.component.ts, rent-adjustment-section.component.ts, tenant-section.component.ts, lease-global-list.component.ts"
---

# Lease Management

This page describes the implemented lease API, persistence, interface, and alert calculations. It records current application behavior, not a confirmed specification of lease policy. The lease domain and its relationship to contractual rent and external regulation are described in the [Lease concept](../../business/leases/concepts/lease.md) and [Lease Rules](../../business/leases/rules/leases.md).

## Estate scope and access

All lease routes include an estate ID. Estate members can read lease records, and estate managers can create, edit, change status, manage tenants, adjust rent or charges, and delete eligible leases. The service verifies that the housing unit belongs to the route estate and that a requested lease belongs to it. People attached to a lease must also belong to that estate.

## API

| Operation | Endpoint | Access | Implemented behavior |
|---|---|---|---|
| List leases for a housing unit | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/leases` | Estate member | Returns the unit's leases with summary amounts, tenant names, status, dates, and alert flags. |
| Create a lease | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/leases` | Estate manager | Returns `201`; creates a `DRAFT` by default. `?activate=true` creates it as `ACTIVE`. Rejects creation when that unit already has a `DRAFT` or `ACTIVE` lease (BR-LEASE-002). |
| List estate leases | `GET /api/v1/estates/{estateId}/leases` | Estate member | Returns a paginated, filterable list. Defaults to `ACTIVE`, page `0`, size `20`, sorted by `startDate,desc`. |
| Read lease details | `GET /api/v1/estates/{estateId}/leases/{id}` | Estate member | Returns lease details, tenants, rent adjustments, and alert flags. |
| Read lease alerts | `GET /api/v1/estates/{estateId}/leases/alerts` | Estate member | Returns lease end-notice and indexation alerts for the estate. |
| Update a lease | `PUT /api/v1/estates/{estateId}/leases/{id}` | Estate manager | Replaces editable lease fields. Only `DRAFT` and `ACTIVE` leases can be updated. |
| Change lease status | `PATCH /api/v1/estates/{estateId}/leases/{id}/status` | Estate manager | Applies one of the implemented status transitions. |
| Add a person to a lease | `POST /api/v1/estates/{estateId}/leases/{id}/tenants` | Estate manager | Adds a same-estate person with a lease role. Only `DRAFT` and `ACTIVE` leases are editable. |
| Remove a person from a lease | `DELETE /api/v1/estates/{estateId}/leases/{id}/tenants/{personId}` | Estate manager | Removes a tenant from an editable lease; rejects removal of its last `PRIMARY` tenant. |
| Record a rent or charge adjustment | `POST /api/v1/estates/{estateId}/leases/{id}/rent-adjustments` | Estate manager | Records an adjustment and updates the current amount on the lease. Only `DRAFT` and `ACTIVE` leases are editable. |
| Delete a lease | `DELETE /api/v1/estates/{estateId}/leases/{id}` | Estate manager | Returns `204`; permitted only for `DRAFT` or `CANCELLED` leases (BR-LEASE-010). |

The estate lease list can filter by status, lease type, building, housing unit, start- and end-date ranges, and rent range. The frontend initially selects `ACTIVE` but allows users to select other statuses.

## Lease data and editing

A lease belongs to one housing unit and stores signature, start and end dates, lease type, duration and notice periods, contractual monthly rent, monthly charges and their type, registration references for the lease and inventory, deposit information, and tenant-insurance details. Its total is calculated as monthly rent plus monthly charges.

The request requires signature, start, and end dates, a lease type, positive duration and notice periods, and positive monthly rent. Monthly charges default to zero and charge type to `FORFAIT`; charge type can also be `PROVISION`. Deposit, registration, and insurance details are optional. The database requires the end date to be on or after the start date. See BR-LEASE-008 for the amount and period constraints.

The frontend proposes duration and notice-period defaults by lease type and recalculates the end date when the start date or duration changes. Editing the end date also updates the duration. The backend stores the submitted end date; it does not calculate the end date from duration. Lease types are `SHORT_TERM`, `MAIN_RESIDENCE_3Y`, `MAIN_RESIDENCE_6Y`, `MAIN_RESIDENCE_9Y`, `STUDENT`, `GLIDING`, `COMMERCIAL`, and `SHARING`.

The statuses are `DRAFT`, `ACTIVE`, `FINISHED`, and `CANCELLED`. The implemented transitions are:

| Current status | Allowed target status |
|---|---|
| `DRAFT` | `ACTIVE`, `CANCELLED` |
| `ACTIVE` | `FINISHED`, `CANCELLED` |
| `FINISHED` | None |
| `CANCELLED` | None |

These transitions are recorded as BR-LEASE-004; their intended business meaning remains unresolved. The application rejects creation when another lease for the unit is `DRAFT` or `ACTIVE` (BR-LEASE-002), but status activation does not repeat that check. Whether the restriction should also be enforced during activation remains open.

A status-change request also accepts `effectiveDate` and `notes`, but the service changes only the status; those values are not persisted. The `lease` table has no field for a finish or cancellation date or note.

## Lease participants

People are linked through `lease_tenant` with the roles `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. Creating a lease requires at least one `PRIMARY` person (BR-LEASE-003). A person cannot be added twice to the same lease, and the service rejects a person who belongs to another estate (BR-LEASE-007). Managers can add or remove people while a lease is `DRAFT` or `ACTIVE` (BR-LEASE-005); the last `PRIMARY` person cannot be removed (BR-LEASE-006). There is no operation to change a participant's role directly.

## Contractual rent and charge adjustments

Housing-unit rent history and lease adjustments represent separate data:

- A housing unit's dated rent history records its theoretical rent; see [Housing-Unit Rent History Management](../buildings-and-housing-units/rent-history-management.md).
- A lease stores its contractual rent and charges. `lease_rent_adjustment` records the affected field (`RENT` or `CHARGES`), previous and new amount, required reason, effective date, and creation time.

The adjustment request requires a positive new amount, reason, and effective date (BR-LEASE-009). Saving it updates the current rent or charge on the lease and retains the adjustment in history. The interface displays this history, but does not store dedicated index values, index months, or notification dates. There is no separate indexation endpoint or indexation-history table. The user-facing label "rent history" in the lease detail refers to these lease adjustments, not to the housing-unit rent timeline.

The application does not directly associate a regulated reference rent with a lease; see [BR-LEASE-001](../../business/leases/rules/leases.md).

## Lease alerts

The service calculates two lease alert types:

- **Indexation:** based on the anniversary of the lease start date. The warning window is hard-coded to 30 days. The alert is suppressed when a `RENT` adjustment has an effective date in the relevant calendar year. The configured estate key `contracts.alert.lease.indexation.days` is not read by `LeaseService`; estate configuration is described in [Cross-Cutting Configuration Management](../cross-cutting/configuration-management.md).
- **End notice:** based on `endDate` minus `noticePeriodMonths`; the alert becomes active on or after that date. The contextual alert query considers both `ACTIVE` and `DRAFT` leases and does not stop producing an end-notice alert after the lease end date. Detail and unit-summary DTOs calculate end-notice flags without checking lease status.

Alert calculations are not fully consistent across responses: global-list indexation flags are restricted to `ACTIVE` leases, while detail and unit summaries calculate the indexation flag without a status check. Global-list end-notice flags stop after the lease end date, but detail and unit-summary flags do not. The shared [AlertService](../../../backend/src/main/java/com/immocare/service/AlertService.java) combines lease alerts with other alert sources and sorts them by deadline. Cross-domain alert behavior is summarized in [Alerts](../../business/cross-cutting/concepts/alerts.md).

## Frontend behavior

- The housing-unit lease section shows leases other than `FINISHED` (including `CANCELLED`) in its main list and places `FINISHED` records in a collapsible history section. It displays status, participants, dates, rent, and charges.
- The lease details page shows status-transition controls, edit and delete actions where available, contractual amounts, participants, adjustments, and alert banners.
- The estate-wide lease list is paginated, filterable, and sortable.
- Loading failures in the housing-unit lease section only clear its loading indicator; no error is shown there.

## Differences from earlier lease analysis

The historical analysis and related stories describe separate indexation fields and endpoints, ask that effective dates and notes be recorded when finishing or cancelling a lease, and place cancelled leases in history. The current implementation instead records rent changes through `lease_rent_adjustment`, ignores the status request's `effectiveDate` and `notes`, and groups cancelled leases with the unit's non-finished leases. Those historical criteria are not treated here as confirmed business policy; the decisions are listed in [Open Questions](../../../backlog/open_questions.md#housing-units-and-leases).

## Sources

- [Lease controller](../../../backend/src/main/java/com/immocare/controller/LeaseController.java)
- [Lease service](../../../backend/src/main/java/com/immocare/service/LeaseService.java)
- [Lease entity](../../../backend/src/main/java/com/immocare/model/entity/Lease.java)
- [Lease participant entity](../../../backend/src/main/java/com/immocare/model/entity/LeaseTenant.java)
- [Lease rent-adjustment entity](../../../backend/src/main/java/com/immocare/model/entity/LeaseRentAdjustment.java)
- [Lease rent-adjustment repository](../../../backend/src/main/java/com/immocare/repository/LeaseRentAdjustmentRepository.java)
- [Lease migration](../../../backend/src/main/resources/db/migration/V014__uc014_manage_leases.sql)
- [Lease frontend service](../../../frontend/src/app/core/services/lease.service.ts)
- [Housing-unit lease section](../../../frontend/src/app/features/lease/components/lease-section/lease-section.component.ts)
- [Lease details component](../../../frontend/src/app/features/lease/lease-details/lease-details.component.ts)
- [Lease form component](../../../frontend/src/app/features/lease/lease-form/lease-form.component.ts)
- [Lease rent-adjustment component](../../../frontend/src/app/features/lease/components/rent-adjustment-section/rent-adjustment-section.component.ts)
- [Lease participant component](../../../frontend/src/app/features/lease/components/tenant-section/tenant-section.component.ts)
- [Estate lease-list component](../../../frontend/src/app/features/lease/lease-global-list/lease-global-list.component.ts)
