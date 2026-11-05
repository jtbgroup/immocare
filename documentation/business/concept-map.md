---
title: Business Concept Map
type: business reference
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Business Concept Map

This map shows relationships represented in the application. It does not imply legal relationships, exact business cardinalities, or responsibilities.

```text
User account
└── Estate membership (MANAGER or VIEWER for one estate)
    └── Estate
        ├── Buildings
        │   └── Housing units
        │       ├── Rooms
        │       └── Leases
        │           └── People (primary tenant, co-tenant, guarantor)
        ├── People (including owners and people associated with leases)
        ├── Tracked equipment (boilers, fire extinguishers, meters)
        │   ├── Any equipment type may be associated with a building or housing unit
        │   └── Type-specific service, revision, usage, and alert histories
        ├── Financial transactions
        │   ├── Bank account
        │   ├── Category → Subcategory
        │   ├── Building / housing unit / lease (contextual links)
        │   └── Equipment links (boiler, fire extinguisher, meter)
        └── Management reference data, including categories

Platform administrator
└── Transversal access to estates (membership not required)
```

## How to read this map and its limitations

- The estate is the observed scope to which properties and financial data are assigned.
- In the current conceptual model, a building contains one or more housing units; a housing unit may contain rooms.
- A lease is associated with a housing unit and people.
- Any tracked equipment type can be associated with either a building or a housing unit. Equipment-specific maintenance, revision, usage, and alert histories remain distinct.
- A transaction may be contextualized by a building, housing unit, or lease, and may separately be linked to equipment.
- A user account is an application identity; estate memberships are separate from person records managed in an estate. A platform administrator can access an estate without being a member.
- The links above reflect relationships represented in the model. They do not, by themselves, define legal ownership, management responsibility, or all business cardinalities.

See the [user account](./concepts/user-account.md), [estate and access](./concepts/estate-and-access.md), [estate and rental](./concepts/estate-and-rental.md), [property equipment and assets](./concepts/property-equipment.md), and [financial transaction](./concepts/financial-transaction.md) pages.
