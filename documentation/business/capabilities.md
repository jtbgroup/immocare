---
title: Capabilities
type: capability-catalog
status: draft
last_verified: 2026-11-05
verified_against: "b99e38fb08888ee0973fc560a27258b15da926b0"
sources: "Checked against current person, ownership, and bank-account implementation"
---

# Capabilities

What the application lets users do today. This is a catalog of capabilities, not a
set of user stories or screens. Each line points to the concept and rules that define
it. Future work is in [`backlog/backlog.md`](../../backlog/backlog.md), not here.

**Status values**: `Implemented`, `Partial` (works with a known gap listed in the
backlog).

> Status and known gaps reflect behavior checked against the current code.

## Access and estates

| Capability                                                    | Status      | Details                                                                   | Rules             | Main sources                                  |
| ------------------------------------------------------------- | ----------- | ------------------------------------------------------------------------- | ----------------- | --------------------------------------------- |
| Sign in, sign out, restore the current session                | Implemented | [Authentication](../technical/authentication.md)                          | —                 | `AuthController`, `SecurityConfig`            |
| Manage user accounts and platform-administrator access         | Partial     | [User account](./concepts/user-account.md), [account management](../technical/user-management.md) | BR-USER-001 to 010 | `UserController`, `UserService`, user frontend |
| Create estates and manage their memberships (`MANAGER`, `VIEWER`) | Partial | [Estate and access](./concepts/estate-and-access.md), [Estate management](../technical/estate-management.md) | BR-ESTATE-001 to 010 | `EstateService`, `EstateMember`, estate frontend |
| Select and switch between estates after sign-in                | Implemented | [Estate management](../technical/estate-management.md)                    | —                 | frontend `AuthService`, `ActiveEstateService`  |
| Platform-wide administration of estates                       | Implemented | [Roles](./roles.md)                                                       | —                 | `EstateSecurityService`                       |

Known gaps in estate creation, member search, estate deletion, and manager-facing controls are listed in the [backlog](../../backlog/backlog.md); unsettled policy decisions are in [open questions](../../backlog/open_questions.md).

Known people-management gaps include owner references that are not validated against the estate, picker search that omits email, person deletion failures for historical lease memberships, and IBAN uniqueness and lease-suggestion behavior that differ from the legacy specification. See the [backlog](../../backlog/backlog.md) and [open questions](../../backlog/open_questions.md).

## Properties

| Capability                                         | Status      | Details                                              | Rules | Main sources                      |
| -------------------------------------------------- | ----------- | ---------------------------------------------------- | ----- | --------------------------------- |
| Manage buildings, housing units and rooms          | Implemented | [Estate and rental](./concepts/estate-and-rental.md) | —     | `Building`, `HousingUnit`, `Room` |
| Record owners on buildings and housing units       | Implemented | [Estate and rental](./concepts/estate-and-rental.md) | —     | `Person`                          |
| Record energy performance ratings per housing unit | Implemented | —                                                    | —     | to fill                           |

## People and leases

| Capability                                                                               | Status      | Details                                              | Rules                  | Main sources |
| ---------------------------------------------------------------------------------------- | ----------- | ---------------------------------------------------- | ---------------------- | ------------ |
| Manage person records and view their owner, lease, and bank-account associations         | Partial     | [Estate and rental](./concepts/estate-and-rental.md), [Person Management](../technical/person-management.md) | BR-PERSON-001 to 004 | `PersonService`, `PersonController` |
| Search and paginate people within an estate                                               | Implemented | [Person Management](../technical/person-management.md) | —                   | `PersonRepository`, `PersonController` |
| Select people through the owner and lease autocomplete                                   | Partial     | [Person Management](../technical/person-management.md) | —                   | `PersonRepository`, `PersonPickerComponent` |
| Manage person bank accounts and mark a primary account                                   | Partial     | [Person Management](../technical/person-management.md) | BR-PERSON-005, 006  | `PersonBankAccountService`, `PersonBankAccountController` |
| Manage leases (draft, active, finished, cancelled) with rent, charges and guarantee type | Implemented | [Estate and rental](./concepts/estate-and-rental.md) | BR-LEASE-\* (to write) | `Lease`      |
| Attach people to a lease as `PRIMARY`, `CO_TENANT` or `GUARANTOR`                        | Implemented | [Estate and rental](./concepts/estate-and-rental.md) | BR-LEASE-\* (to write) | `Lease`      |
| View all leases in one list                                                              | Implemented | —                                                    | —                      | to fill      |

## Equipment and alerts

| Capability                                                      | Status      | Details                                                | Rules     | Main sources                    |
| --------------------------------------------------------------- | ----------- | ------------------------------------------------------ | --------- | ------------------------------- |
| Track boilers: fuel, installation, service history              | Implemented | [Property equipment](./concepts/property-equipment.md) | BR-EQ-\*  | `Boiler`                        |
| Track fire extinguishers and their revisions                    | Implemented | [Property equipment](./concepts/property-equipment.md) | BR-EQ-\*  | `FireExtinguisher`              |
| Track meters (gas, electricity, water): replace, close, history | Implemented | [Property equipment](./concepts/property-equipment.md) | BR-EQ-\*  | `Meter`                         |
| Attach any equipment type to a building or a housing unit       | Partial     | [Equipment rules](./rules/equipment.md)                | BR-EQ-001 | see backlog: generalize devices |
| Raise alerts when a boiler service is due or overdue            | Implemented | [Property equipment](./concepts/property-equipment.md) | —         | to fill                         |
| Cross-cutting alerts view                                       | Implemented | —                                                      | —         | to fill                         |

## Financial transactions

| Capability                                                                                     | Status      | Details                                                      | Rules               | Main sources                  |
| ---------------------------------------------------------------------------------------------- | ----------- | ------------------------------------------------------------ | ------------------- | ----------------------------- |
| Record income and expenses manually                                                            | Implemented | [Financial transaction](./concepts/financial-transaction.md) | BR-TX-001, 002, 007 | `FinancialTransactionService` |
| Classify transactions by category and subcategory (catalogue in Settings)                      | Implemented | [Financial transaction](./concepts/financial-transaction.md) | BR-TX-004           | `SubcategoryDirection`        |
| Link a transaction to a building, housing unit, lease or bank account                          | Implemented | [Financial transaction](./concepts/financial-transaction.md) | BR-TX-003           | `FinancialTransaction`        |
| Allocate part of a transaction to equipment (cost allocation)                                  | Implemented | [Property equipment](./concepts/property-equipment.md)       | BR-TX-006           | `TransactionAssetLink`        |
| Import bank transactions in batches, skip duplicates, get classification and lease suggestions | Partial     | [Financial transaction](./concepts/financial-transaction.md) | BR-TX-007, 009      | `TransactionImportService`    |
| Review imported rows: select, confirm, link to properties or people                            | Partial     | —                                                            | —                   | to fill                       |
| Financial dashboard                                                                            | Partial     | —                                                            | —                   | to fill                       |

Known gaps for the `Partial` rows: see the matching items in the backlog (import
review action and duplicate decisions, financial dashboard, generalized devices,
and user-account session invalidation and deletion controls).

## Not covered yet

Expense management, document attachments, individual alert dismissal and
multilingual support are planned; see the backlog. They are intentionally not listed
as capabilities.
