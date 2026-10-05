# Concepts — Portfolio and Rental

This page describes the property-management objects represented in the current model. It does not define the portfolio's legal or organizational structure, which still needs business validation.

## Portfolio

The portfolio (`Estate`) is the scope to which buildings, people, leases, and transactions are assigned. Bank accounts and classification reference data are also associated with this scope.

In the application, membership and roles are specific to each portfolio. The corresponding business concept—company, owner, client, portfolio, or something else—cannot be determined from the code.

## Building, housing unit, and room

- A **building** (`Building`) is a property assigned to a portfolio. It may be associated with an owner.
- A **housing unit** (`HousingUnit`) belongs to a building and may be associated with a different owner.
- A **room** (`Room`) describes a room or space within a housing unit.

The model allows a housing unit to have an owner different from the building's owner. The observed code does not define an inheritance or precedence rule between these owners.

The model also manages property equipment—boilers, fire extinguishers, and meters—associated with a building or a housing unit. Operational equipment tracking is distinct from the definition of the property itself; see [Property equipment and assets](./property-equipment.md).

## Person

A person (`Person`) is recorded within a portfolio and may be referenced as the owner of a building or housing unit. A person may also be associated with a lease.

The model stores identifying and contact information; this page does not detail those attributes. Whether one person may hold multiple roles or appear in multiple portfolios is not established by the model relationships alone.

## Lease and associated people

A **lease** (`Lease`) concerns a housing unit. It groups contractual information, dates, rent and charge amounts, and associated people.

The statuses currently represented are `DRAFT`, `ACTIVE`, `FINISHED`, and `CANCELLED`. The code treats a `DRAFT` or `ACTIVE` lease as editable; this is not enough to establish the business conditions for transitioning between statuses.

People associated with a lease have one of the technical roles `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. The precise meaning and responsibilities of these roles, and the rules governing changes to the people on a lease, need validation.

## Relationships to keep in mind

```text
Portfolio → Building → Housing unit → Room
                                └→ Lease → Associated person(s)
Person → may own a building or housing unit
```

Portfolio assignment constraints visible in the implementation are documented in the [portfolio rules](../rules/portfolio.md).
