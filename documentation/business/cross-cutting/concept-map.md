---
title: Business Concept Map
type: business reference
status: draft
last_verified: 2026-11-07
verified_against: "HousingUnit, Room, PebScoreHistory, RentHistory, LeaseRentAdjustment, V008__uc008_manage_peb_scores.sql, V010__uc010_manage_rents.sql, V014__uc014_manage_leases.sql"
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
        │       ├── PEB rating and certificate history
        │       ├── Rent history
        │       └── Leases
        │           ├── People (primary tenant, co-tenant, guarantor)
        │           └── Rent and charge adjustments
        ├── People (including owners and people associated with leases)
        │   └── Person bank accounts (IBANs used for transaction matching)
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
    └── May also have an estate membership and role
```

## How to read this map and its limitations

- The estate is the observed scope to which properties and financial data are assigned.
- In the current conceptual model, a building contains one or more housing units; a housing unit may contain rooms, PEB rating history, and rent history.
- The intended relationship between housing-unit rent history and rent recorded on a lease needs clarification.
- A lease is associated with a housing unit and people, and has its own dated rent and charge adjustments.
- People may be associated with buildings or housing units as owners, participate in leases, and have bank accounts used to suggest lease links during transaction import.
- Any tracked equipment type can be associated with either a building or a housing unit. Equipment-specific maintenance, revision, usage, and alert histories remain distinct.
- A transaction may be contextualized by a building, housing unit, or lease, and may separately be linked to equipment.
- A user account is an application identity; estate memberships are separate from person records managed in an estate. A platform administrator can access an estate without being a member and may also hold an estate membership.
- The links above reflect relationships represented in the model. They do not, by themselves, define legal ownership, management responsibility, or all business cardinalities.

See the [user account](../user-accounts-and-authentication/concepts/user-account.md), [estate and access](../estates-and-access/concepts/estate-and-access.md), [buildings and housing units](../buildings-and-housing-units/concepts/buildings-and-housing-units.md), [person](../people/concepts/person.md), [lease](../leases/concepts/lease.md), [equipment](../equipment/concepts/property-equipment.md), and [financial transaction](../financial-transactions/concepts/financial-transaction.md) pages, together with the cross-cutting [roles and responsibilities](./roles.md) and [alerts](./concepts/alerts.md) references. PEB implementation behavior is described in [PEB Score Management](../../technical/buildings-and-housing-units/peb-score-management.md); housing-unit rent history is described in [Housing-Unit Rent History Management](../../technical/buildings-and-housing-units/rent-history-management.md).
