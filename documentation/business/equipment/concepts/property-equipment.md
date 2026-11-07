---
title: Property Equipment and Assets
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: "Boiler, BoilerServiceRecord, BoilerServiceValidityRule, FireExtinguisher, FireExtinguisherRevision, Meter, BoilerService, BoilerServiceHistoryService, FireExtinguisherService, MeterService, AlertService, EstateService, V009__uc009_manage_meters.sql, V011__uc011_manage_boilers.sql, V012__uc012_manage_fire_extinguishers.sql, V017__generalize_fire_extinguisher_ownership.sql"
---

# Concept — Property Equipment and Assets

Tracked equipment is associated with a building or a housing unit. Any supported equipment type can be associated with either kind of property; the type must not determine whether it can be attached to a building or a unit.

The equipment currently represented is:

- **Boiler** (`Boiler`)
- **Fire extinguisher** (`FireExtinguisher`)
- **Meter** (`Meter`)

Equipment records describe items that need lifecycle and maintenance history, and may need validity or maintenance alerts, as well as identification and financial cost tracking. These shared expectations apply across equipment types; see confirmed [BR-EQ-032](../rules/equipment.md). Equipment records are distinct from the building and housing-unit concepts themselves.

The association with a building or housing unit identifies the item's management context. It does not, by itself, establish legal ownership, responsibility for maintenance, or whether the equipment is included in a lease. See [equipment rule BR-EQ-001](../rules/equipment.md).

## Shared and type-specific information

Equipment types share the need to be identified and associated with a property context. Beyond that common foundation, each type has its own characteristics, lifecycle, history, validation, and alerting needs. A common concept must not erase those differences:

- Boilers have service records, estate-specific service-validity rules, and service-due alerts. Current API and lifecycle behavior is described in [Boiler Management](../../../technical/equipment/boiler-management.md).
- Fire extinguishers have revision records. The current model associates each extinguisher with either a building or a housing unit, but not both; this behavior needs business clarification under [BR-EQ-026](../rules/equipment.md). See [Fire Extinguisher Management](../../../technical/equipment/fire-extinguisher-management.md) for the implemented API and lifecycle.
- **Meters** (`Meter`): current history consists of the active and closed meter records, including replacement lifecycle; no maintenance or validity alert is implemented. See [Meter Management](../../../technical/equipment/meter-management.md) for observed behavior and [Meter Data Dictionary](../../../technical/equipment/meter-data-dictionary.md) for persisted fields.

The shared association and history expectations apply to all equipment types; equipment-specific history events and applicable validity or maintenance alerts remain type-specific under [BR-EQ-001 and BR-EQ-032](../rules/equipment.md).

## Boiler

A boiler is a heating appliance associated with either a building or a housing unit. The current model records its fuel type, installation and out-of-service dates, and last and next service dates. Its API accepts creation, updates, and deletion; the historical analysis's replacement and append-only lifecycle is not fully implemented.

Service events are recorded as a history linked to the boiler. Each observed service record has a service date and a validity end date. The application also supports an estate-scoped rule for calculating service validity duration and can raise alerts as the next service date approaches or becomes overdue.

The model currently recognizes gas, oil, electric, and heat-pump fuel types. Whether these are the complete business categories, and what maintenance is legally or contractually required, needs validation.

## Fire extinguisher

A fire extinguisher is associated with either a building or a housing unit. It has an identification number and can have a history of revisions, each recorded with a date and optional notes. The historical analysis describes a building association with an optional housing-unit assignment, so the current exactly-one-owner model still needs business clarification.

The model does not store a validity period, next revision date, or inspection outcome. The estate dashboard currently counts an alert when no revision exists or the latest revision is more than one year old ([BR-EQ-031](../rules/equipment.md)); whether this fixed threshold represents the intended inspection policy remains unresolved. The API and persistence behavior are described in [Fire Extinguisher Management](../../../technical/equipment/fire-extinguisher-management.md).

## Meter

A meter represents a water, gas, or electricity connection associated with a building or a housing unit. The application's API and lifecycle behavior are described in [Meter Management](../../../technical/equipment/meter-management.md); persisted fields are listed in the [Meter Data Dictionary](../../../technical/equipment/meter-data-dictionary.md).

## Equipment and financial transactions

A financial transaction can be linked to one or more tracked equipment items. A partial transaction amount may be assigned to an item. This is a cost-allocation relationship; it does not itself define who owns the equipment, who must maintain it, or who ultimately bears the cost.

The implementation currently supports transaction links to boilers, fire extinguishers, and meters. The applicable transaction directions and allocation rules need business confirmation; see the [financial transaction concept](../../financial-transactions/concepts/financial-transaction.md) and [transaction rules](../../financial-transactions/rules/financial-transactions.md).

## Conceptual relationships

```text
Portfolio → Building → Housing unit
      └───────────────┬──────────────┘
                      └── Equipment of any supported type
                          ├── Boiler → Service history and alerts
                          ├── Fire extinguisher → Revision history
                          └── Meter → Usage period and replacement history
Financial transaction → Equipment item(s) (optional cost allocation)
```
