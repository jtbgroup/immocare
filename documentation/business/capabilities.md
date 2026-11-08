---
title: Capabilities
type: capability-catalog
status: draft
last_verified: 2026-11-08
verified_against: "HousingUnitController, HousingUnitService, HousingUnitFormComponent, HousingUnitDetailsComponent, RoomController, RoomService, RoomSectionComponent, PebScoreController, PebScoreService, PebScoreRepository, PebSectionComponent, PebHistoryComponent, RentHistoryController, RentHistoryService, RentSectionComponent, Lease, LeaseController, LeaseService, LeaseRentAdjustment, Lease details and form components, PlatformConfigController, PlatformConfigService, EstateConfigController, EstateConfigService, EstateConfig component, EstateConfigAssetsComponent, BoilerServiceValidityRuleService, BoilerController, BoilerService, BoilerServiceHistoryService, FireExtinguisherController, FireExtinguisherService, MeterController, MeterService, MeterSectionComponent, AlertController, AlertService, EstateService, FinancialTransactionService, TransactionController, TransactionFormComponent, TransactionListComponent, TransactionDetailComponent, TransactionAssetLinkService, CreateTransactionRequest, V010__uc010_manage_rents.sql, V011__uc011_manage_boilers.sql, V013__uc013_manage_config.sql, V014__uc014_manage_leases.sql, V015__uc015_manage_financial_transactions.sql, stakeholder-confirmed domain taxonomy, stakeholder confirmations 2026-11-07 and 2026-11-08"
sources: "Aligned to the approved domain taxonomy"
---

# Capabilities

What the application lets users do today. This is a catalog of capabilities, not a
set of user stories or screens. Each line points to the concept and rules that define
it. Future work is in [`backlog/backlog.md`](../../backlog/backlog.md), not here.

**Status values**: `Implemented`, `Partial` (works with a known gap listed in the
backlog).

> Status and known gaps reflect behavior checked against the current code.

## User Accounts and Authentication

| Capability                                                    | Status      | Details                                                                   | Rules             | Main sources                                  |
| ------------------------------------------------------------- | ----------- | ------------------------------------------------------------------------- | ----------------- | --------------------------------------------- |
| Sign in, sign out, restore the current session                | Implemented | [Authentication](../technical/user-accounts-and-authentication/authentication.md) | —                 | `AuthController`, `SecurityConfig`            |
| Manage user accounts and platform-administrator access         | Partial     | [User account](./user-accounts-and-authentication/concepts/user-account.md), [account management](../technical/user-accounts-and-authentication/user-management.md) | BR-USER-001 to 010 | `UserController`, `UserService`, user frontend |

## Estates and Access

| Capability                                                    | Status      | Details                                                                   | Rules             | Main sources                                  |
| ------------------------------------------------------------- | ----------- | ------------------------------------------------------------------------- | ----------------- | --------------------------------------------- |
| Create estates and manage their memberships (`MANAGER`, `VIEWER`) | Partial | [Estate and access](./estates-and-access/concepts/estate-and-access.md), [Estate management](../technical/estates-and-access/estate-management.md) | BR-ESTATE-001 to 010 | `EstateService`, `EstateMember`, estate frontend |
| Select and switch between estates after sign-in                | Implemented | [Estate management](../technical/estates-and-access/estate-management.md) | —                 | frontend `AuthService`, `ActiveEstateService`  |
| Platform-wide administration of estates                       | Implemented | [Roles](./cross-cutting/roles.md)                                         | —                 | `EstateSecurityService`                       |

Known gaps in estate creation, member search, estate deletion, and manager-facing controls are listed in the [backlog](../../backlog/backlog.md); unsettled policy decisions are in [open questions](../../backlog/open_questions.md).

## Buildings and Housing Units

| Capability                                         | Status      | Details                                              | Rules | Main sources                      |
| -------------------------------------------------- | ----------- | ---------------------------------------------------- | ----- | --------------------------------- |
| Manage buildings, housing units and rooms          | Partial | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md), [Building Management](../technical/buildings-and-housing-units/building-management.md), [Housing Unit Management](../technical/buildings-and-housing-units/housing-unit-management.md), [Room Management](../technical/buildings-and-housing-units/room-management.md) | BR-BUILD-001 to 003; BR-HUNIT-001 to 015 | `Building`, `HousingUnit`, `Room` |
| Record owners on buildings and housing units       | Implemented | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md), [Person](./people/concepts/person.md) | — | `Person`                          |
| Record and review PEB ratings, certificate validity, and rating improvements for housing units | Partial | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md), [PEB Score Management](../technical/buildings-and-housing-units/peb-score-management.md), [Alerts](./cross-cutting/concepts/alerts.md) | BR-HUNIT-016 to 024 | `PebScoreHistory`, `PebScoreService`, `AlertService` |
| Record and review dated theoretical rent history for a housing unit | Partial | [Buildings and housing units](./buildings-and-housing-units/concepts/buildings-and-housing-units.md), [Housing-Unit Rent History Management](../technical/buildings-and-housing-units/rent-history-management.md) | BR-HUNIT-025 to 027 | `RentHistory`, `RentHistoryService`, `RentHistoryController` |

## People

| Capability                                                                               | Status      | Details                                              | Rules                  | Main sources |
| ---------------------------------------------------------------------------------------- | ----------- | ---------------------------------------------------- | ---------------------- | ------------ |
| Manage person records and view their owner, lease, and bank-account associations         | Partial     | [Person](./people/concepts/person.md), [Person Management](../technical/people/person-management.md) | BR-PERSON-001 to 004 | `PersonService`, `PersonController` |
| Search and paginate people within an estate                                               | Implemented | [Person Management](../technical/people/person-management.md) | —                   | `PersonRepository`, `PersonController` |
| Select people through the owner and lease autocomplete                                   | Partial     | [Person Management](../technical/people/person-management.md) | —                   | `PersonRepository`, `PersonPickerComponent` |
| Manage person bank accounts and mark a primary account                                   | Partial     | [Person Management](../technical/people/person-management.md) | BR-PERSON-005, 006  | `PersonBankAccountService`, `PersonBankAccountController` |

Known people-management gaps include owner references that are not validated against the estate, picker search that omits email, person deletion failures for historical lease memberships, and IBAN uniqueness and lease-suggestion behavior that differ from the legacy specification. See the [backlog](../../backlog/backlog.md) and [open questions](../../backlog/open_questions.md).

## Leases

| Capability                                                                               | Status      | Details                                              | Rules                  | Main sources |
| ---------------------------------------------------------------------------------------- | ----------- | ---------------------------------------------------- | ---------------------- | ------------ |
| Manage leases (draft, active, finished, cancelled) with rent, charges and guarantee type | Implemented | [Lease](./leases/concepts/lease.md), [Lease Management](../technical/leases/lease-management.md) | BR-LEASE-001 to 005, BR-LEASE-008 to 010 | `Lease`, `LeaseController`, `LeaseService`, `LeaseRentAdjustment` |
| Attach people to a lease as `PRIMARY`, `CO_TENANT` or `GUARANTOR`                        | Implemented | [Lease](./leases/concepts/lease.md), [Lease Management](../technical/leases/lease-management.md), [Person](./people/concepts/person.md) | BR-LEASE-003, BR-LEASE-005 to 007 | `Lease`      |
| View, filter and paginate leases in one estate-wide list                                 | Implemented | [Lease Management](../technical/leases/lease-management.md) | — | `LeaseController`, `LeaseService` |

## Equipment

| Capability                                                      | Status      | Details                                                | Rules     | Main sources                    |
| --------------------------------------------------------------- | ----------- | ------------------------------------------------------ | --------- | ------------------------------- |
| Track boilers: fuel, installation, and service history          | Partial | [Property equipment](./equipment/concepts/property-equipment.md), [Boiler Management](../technical/equipment/boiler-management.md) | BR-EQ-014 to 022, BR-EQ-029, BR-EQ-030, BR-EQ-032 | `BoilerService`, `BoilerServiceHistoryService` |
| Track fire extinguishers and their revision history              | Partial | [Property equipment](./equipment/concepts/property-equipment.md), [Fire Extinguisher Management](../technical/equipment/fire-extinguisher-management.md) | BR-EQ-023 to 028, BR-EQ-031, BR-EQ-032 | `FireExtinguisherService` |
| Track meters (water, gas, electricity): add, replace, close, and retain lifecycle history | Partial | [Property equipment](./equipment/concepts/property-equipment.md), [Meter Management](../technical/equipment/meter-management.md) | BR-EQ-002 to 013, BR-EQ-032 | `MeterController`, `MeterService` |
| Attach any equipment type to a building or a housing unit       | Partial     | [Equipment rules](./equipment/rules/equipment.md) | BR-EQ-001 | see backlog: generalize devices |
| Raise applicable validity and maintenance alerts for equipment | Partial | [Alerts](./cross-cutting/concepts/alerts.md), [Boiler Management](../technical/equipment/boiler-management.md), [Fire Extinguisher Management](../technical/equipment/fire-extinguisher-management.md), [Meter Management](../technical/equipment/meter-management.md), [Estate Management](../technical/estates-and-access/estate-management.md) | BR-EQ-022, BR-EQ-031, BR-EQ-032 | `AlertService`, `EstateService` |

## Financial Transactions

| Capability                                                                                     | Status      | Details                                                      | Rules               | Main sources                  |
| ---------------------------------------------------------------------------------------------- | ----------- | ------------------------------------------------------------ | ------------------- | ----------------------------- |
| Record, list, edit, and delete income and expenses manually                                    | Partial     | [Financial transaction](./financial-transactions/concepts/financial-transaction.md), [Manual Transaction Management](../technical/financial-transactions/manual-transaction-management.md) | BR-TX-001, 002, 005, 007, 008, 010, 012 | `FinancialTransactionService`, `TransactionFormComponent` |
| Classify transactions by category and subcategory (catalogue in Settings)                      | Implemented | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) | BR-TX-004 | `SubcategoryDirection` |
| Link a transaction to a building, housing unit, lease or bank account                          | Implemented | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) | BR-TX-003 | `FinancialTransaction` |
| Allocate part of a transaction to equipment (cost allocation)                                  | Implemented | [Financial transaction](./financial-transactions/concepts/financial-transaction.md), [Property equipment](./equipment/concepts/property-equipment.md) | BR-TX-006 | `TransactionAssetLink` |
| Allocate one expense transaction across multiple costs, including non-equipment costs          | Partial     | [Financial transaction](./financial-transactions/concepts/financial-transaction.md), [backlog](../../backlog/backlog.md) | BR-TX-011 | `TransactionAssetLink`; generic cost allocations are not implemented |
| Import bank transactions in batches, skip duplicates, get classification and lease suggestions | Partial     | [Financial transaction](./financial-transactions/concepts/financial-transaction.md), [Person Management](../technical/people/person-management.md) | BR-TX-007, 009 | `TransactionImportService` |
| Review and select imported rows; apply category, direction, building, housing-unit, lease, or equipment enrichment | Partial | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) | — | `TransactionImportService`, `TransactionImportController` |
| Financial dashboard                                                                            | Partial     | [Financial transaction](./financial-transactions/concepts/financial-transaction.md) | — | `TransactionDashboardComponent` |

## Cross-cutting capabilities

| Capability | Status | Details | Rules | Main sources |
|---|---|---|---|---|
| View pending alerts across business domains, including PEB certificate expiry | Implemented | [Alerts](./cross-cutting/concepts/alerts.md), [PEB Score Management](../technical/buildings-and-housing-units/peb-score-management.md), [Estate Management](../technical/estates-and-access/estate-management.md) | BR-HUNIT-023 | `AlertService`, `AlertController`, `EstateService` |
| Manage platform-wide settings and estate-scoped configuration | Partial | [Cross-Cutting Configuration Management](../technical/cross-cutting/configuration-management.md) | — | `PlatformConfigController`, `EstateConfigController`, estate settings UI |

The current alert list includes boiler service, lease indexation, and PEB certificate expiry alerts; it does not generate alerts from housing-unit rent history. The estate dashboard separately reports pending boiler, fire-extinguisher, lease-end, and indexation alert counts.

Known gaps for the `Partial` rows: see the matching items in the backlog (room,
PEB, meter, and boiler estate scoping; room not-found handling; PEB active-record
selection, record-specific permissions, deletion policy, rating palette and
load-error presentation; rent-timeline overlap and rent-history estate scoping;
configuration-key governance and the equipment-to-subcategory mapping editor;
meter history access and replacement-reason handling;
boiler replacement/history behavior; fire-extinguisher identifier uniqueness
scope, revision ownership and deletion, search scope, and alert policy; meter
history scope and any applicable validity/maintenance alerts; import review
action and duplicate decisions;
financial dashboard; generalized devices; and user-account session invalidation
and deletion controls).

## Planned but not implemented

Expense management, document attachments, individual alert dismissal and
multilingual support are planned; see the backlog. They are intentionally not listed
as capabilities.
