---
title: Business Glossary
type: business reference
status: draft
last_verified: 2026-11-07
verified_against: "HousingUnit, PebScore, PebScoreHistory, RentHistory, LeaseRentAdjustment, Boiler, BoilerServiceRecord, BoilerServiceValidityRule, FireExtinguisher, FireExtinguisherRevision, Meter, SaveBoilerRequest, SaveFireExtinguisherRequest, AddMeterRequest, CreateHousingUnitRequest, UpdateHousingUnitRequest, CreatePebScoreRequest, SetRentRequest, V010__uc010_manage_rents.sql, V014__uc014_manage_leases.sql, stakeholder confirmation 2026-11-07"
---

# Business Glossary

Concise definitions of terms used in this reference. Structurally important concepts are described in more detail in the linked domain concept pages.

| Term                       | Concise definition                                                                                                                        | Details                                                           |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Accounting month           | The month to which a transaction is assigned for tracking and financial reporting; it may differ from its bank transaction date.          | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) |
| Alert                      | A notification that calls attention to an event or condition arising from a business domain, such as equipment, a lease, or PEB data.      | [Alerts](./cross-cutting/concepts/alerts.md)                      |
| Boiler                     | A heating appliance tracked by its installation, service dates, and service history.                                                      | [Property equipment and assets](./equipment/concepts/property-equipment.md) |
| Boiler service validity rule | A dated, estate-specific duration used to calculate the validity end date of a boiler service record.                                    | [Boiler Management](../technical/equipment/boiler-management.md) |
| Building                   | A property in an estate that may contain housing units.                                                                                   | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Category / subcategory     | Hierarchical classification levels used to classify transactions.                                                                         | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) |
| Energy performance rating (PEB) | A dated energy-performance grade recorded for a housing unit, optionally with certificate details and a validity end date.             | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Estate                     | A property-management entity, such as a real-estate agency or an individual who owns several properties.                                    | [Estate and access](./estates-and-access/concepts/estate-and-access.md) |
| Estate membership          | The association that grants a user account a role within a specific estate.                                                                | [Estate and access](./estates-and-access/concepts/estate-and-access.md) |
| Financial transaction      | An inflow or outflow recorded in an estate, with a date, amount, and direction.                                                           | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) |
| Fire extinguisher          | Fire-safety equipment associated with a building or a housing unit and tracked with revision records.                                     | [Property equipment and assets](./equipment/concepts/property-equipment.md) |
| Garden                     | An outdoor garden associated with a housing unit, with an optional recorded surface and orientation.                                      | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Housing unit               | A dwelling or unit within a building that may be associated with a lease.                                                                 | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Lease                      | A rental relationship associated with a housing unit and one or more people.                                                              | [Lease](./leases/concepts/lease.md)                                |
| Manager / viewer           | Estate-scoped application access roles; they do not, by themselves, define business roles.                                                  | [Roles](./cross-cutting/roles.md)                                 |
| Meter                      | A water, gas, or electricity meter associated with a building or housing unit, tracked over an active or closed use period.                | [Property equipment and assets](./equipment/concepts/property-equipment.md) |
| Person                     | An individual recorded within the management scope who may own property or be associated with a lease.                                    | [Person](./people/concepts/person.md)                               |
| Person bank account        | An IBAN record associated with a person and used to help match transaction counterparties.                                                 | [Person](./people/concepts/person.md)                               |
| Platform administrator     | A user account with platform-wide administrative access, separate from estate-specific membership roles.                                  | [User account](./user-accounts-and-authentication/concepts/user-account.md) |
| Property equipment / asset | A tracked physical item associated with a building or housing unit, such as a boiler, fire extinguisher, or meter.                        | [Property equipment and assets](./equipment/concepts/property-equipment.md) |
| Regulated reference rent   | A rent amount defined or calculated under an applicable local scheme, separate from theoretical housing-unit rent and contractual lease rent; applicable schemes and calculation are not yet established for the application. | [Backlog](../../backlog/backlog.md) |
| Rent history               | Dated records of the theoretical monthly rent for a housing unit, used as a basis for its separate lease rent.                              | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Room                       | A space or room represented as part of a housing unit.                                                                                    | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Service / revision         | A recorded maintenance or inspection event for tracked equipment.                                                                         | [Property equipment and assets](./equipment/concepts/property-equipment.md) |
| Terrace                    | An outdoor area associated with a housing unit, with an optional recorded surface and orientation.                                        | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md) |
| Transaction-to-asset link  | A relationship that associates a financial transaction with tracked equipment, optionally assigning part of the transaction amount to it. | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) |
| User account               | An application identity used to sign in; it is distinct from a person record.                                                              | [User account](./user-accounts-and-authentication/concepts/user-account.md) |
