# Concept — Property Equipment and Assets

Tracked equipment is associated with a building or a housing unit. Any supported equipment type can be associated with either kind of property; the type must not determine whether it can be attached to a building or a unit.

The equipment currently represented is:

- **Boiler** (`Boiler`)
- **Fire extinguisher** (`FireExtinguisher`)
- **Meter** (`Meter`)

Equipment records describe items that may need identification, operational history, maintenance, inspection, alerting, or financial cost tracking. They are distinct from the building and housing-unit concepts themselves.

The association with a building or housing unit identifies the item's management context. It does not, by itself, establish legal ownership, responsibility for maintenance, or whether the equipment is included in a lease. See [equipment rule BR-EQ-001](../rules/equipment.md).

## Shared and type-specific information

Equipment types share the need to be identified and associated with a property context. Beyond that common foundation, each type has its own characteristics, lifecycle, history, validation, and alerting needs. A common concept must not erase those differences:

- Boilers have service records, service-validity rules, and service-due alerts.
- Fire extinguishers have revision records; the business rules for revision validity and alerting still need to be established.
- Meters have usage periods and replacement history.

The shared association rule applies to all equipment types; it does not require every type to have the same history or maintenance process.

## Boiler

A boiler is a heating appliance associated with either a building or a housing unit. The current model records its fuel type, installation and out-of-service dates, and last and next service dates.

Service events are recorded as a history linked to the boiler. Each observed service record has a service date and a validity end date. The application also supports an estate-scoped rule for calculating service validity duration and can raise alerts as the next service date approaches or becomes overdue.

The model currently recognizes gas, oil, electric, and heat-pump fuel types. Whether these are the complete business categories, and what maintenance is legally or contractually required, needs validation.

## Fire extinguisher

A fire extinguisher is associated with a building and may additionally be associated with a housing unit. It has an identification number and can have a history of revisions, each recorded with a date and optional notes.

The observed model does not define a validity period, next revision date, inspection outcome, or the rules for determining when another revision is due. Those should not be inferred from the existence of a revision history.

## Meter

A meter is associated with either a building or a housing unit. The model records a meter type and number, a start date, and an optional end date; additional identifiers depend on the utility type.

The application distinguishes active meters (those without an end date) from historical meters. Replacing a meter closes the current record and creates a new one, preserving the usage history. A meter can also be closed without a replacement.

Observed utility types include gas, electricity, and water. Which meter types and identifiers are required depends on the type and whether the meter is associated with a building or a housing unit.

## Equipment and financial transactions

A financial transaction can be linked to one or more tracked equipment items. A partial transaction amount may be assigned to an item. This is a cost-allocation relationship; it does not itself define who owns the equipment, who must maintain it, or who ultimately bears the cost.

The implementation currently supports transaction links to boilers, fire extinguishers, and meters. The applicable transaction directions and allocation rules need business confirmation; see the [financial transaction concept](./financial-transaction.md) and [transaction rules](../rules/financial-transactions.md).

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
