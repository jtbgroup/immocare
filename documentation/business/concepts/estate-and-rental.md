---
title: Estate and Rental
type: business concepts
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Concepts — Estate and Rental

This page describes the property-management objects represented in the current model. It does not define the estate's legal or organizational structure, which still needs business validation.

## Estate scope

Buildings, people, leases, transactions, and other estate-specific management data are assigned to an [estate](./estate-and-access.md). Its legal or organizational meaning is not established by the code.

## Building, housing unit, and room

- A **building** (`Building`) is a property assigned to an estate. It may be associated with an owner.
- A **housing unit** (`HousingUnit`) belongs to a building and may be associated with a different owner.
- A **room** (`Room`) describes a room or space within a housing unit.

The model allows a housing unit to have an owner different from the building's owner. The observed code does not define an inheritance or precedence rule between these owners.

The model also manages property equipment—boilers, fire extinguishers, and meters—associated with a building or a housing unit. Operational equipment tracking is distinct from the definition of the property itself; see [Property equipment and assets](./property-equipment.md).

## Person

A **person** (`Person`) is recorded within an estate and may be referenced as the owner of a building or housing unit. A person may also be associated with a lease.

The model stores identifying and contact information; this page does not detail those attributes. A person may appear in multiple estates, but is then considered as a 2 distinct persons as the management around the person is estate-based.

A person record is separate from a user account: the current model has no dedicated identity link identifying them as the same human, though domain records may separately retain user audit references. User accounts and their access scopes are described in [User Account](./user-account.md); whether a person record should be linked to an account remains an open question.

## Lease and associated people

A **lease** (`Lease`) concerns a housing unit. It groups contractual information, dates, rent and charge amounts, and associated people.

The statuses currently represented are `DRAFT`, `ACTIVE`, `FINISHED`, and `CANCELLED`. The code treats a `DRAFT` or `ACTIVE` lease as editable; this is not enough to establish the business conditions for transitioning between statuses.

People associated with a lease have one of the technical roles `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. The precise meaning and responsibilities of these roles, and the rules governing changes to the people on a lease, need validation.

## Relationships to keep in mind

```text
Estate → Building → Housing unit → Room
                                └→ Lease → Associated person(s)
Person → may own a building or housing unit
```

Estate membership and access concepts are documented in [Estate and Access](./estate-and-access.md). Estate assignment constraints visible in the implementation are documented in the [estate rules](../rules/estate.md).
