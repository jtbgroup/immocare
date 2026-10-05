# Business Concept Map

This map shows relationships represented in the application. It does not imply legal relationships, exact business cardinalities, or responsibilities.

```text
Portfolio
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
└── Management reference data, including accounts and categories
```

## How to read this map and its limitations

- The portfolio is the observed scope to which properties and financial data are assigned.
- In the current conceptual model, a building contains one or more housing units; a housing unit may contain rooms.
- A lease is associated with a housing unit and people.
- Any tracked equipment type can be associated with either a building or a housing unit. Equipment-specific maintenance, revision, usage, and alert histories remain distinct.
- A transaction may be contextualized by a building, housing unit, or lease, and may separately be linked to equipment.
- The links above reflect relationships represented in the model. They do not, by themselves, define legal ownership, management responsibility, or all business cardinalities.

See the [portfolio and rental](./concepts/portfolio-and-rental.md), [property equipment and assets](./concepts/property-equipment.md), and [financial transaction](./concepts/financial-transaction.md) pages.
