---
title: Roles and Responsibilities
type: business reference
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Roles and Responsibilities

Distinguish **people's roles in the property business** from the **application access roles** currently implemented. The code primarily describes access rights; it does not fully define the organization's structure or business delegations.

## Business roles identified so far

| Role / relationship        | Representation currently visible in the application                                                                                      |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Estate manager             | `MANAGER` is an estate-scoped application access role that permits management operations within an estate.                                |
| Property owner             | A person can be associated as the owner of a building or housing unit.                                                                   |
| Tenant / occupant          | People associated with a lease have one of the technical relationships `PRIMARY`, `CO_TENANT`, or `GUARANTOR`.                           |
| Third party / counterparty | Financial movements can include counterparty information; the model does not define a separate set of business roles for counterparties. |

These labels describe relationships and access concepts represented in the application. They are not an approved responsibility matrix.

## Application access roles currently observed

| Access role      | Observed scope and rights                                                                                                                                         |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PLATFORM_ADMIN` | Global account-level access; can administer user accounts and access estates through estate-scoped checks without membership.                                                   |
| `MANAGER`        | An estate member allowed to perform management operations. This role is required, among other things, to create, update, delete, import, or confirm transactions. |
| `VIEWER`         | An estate member who passes the read-access check shared with managers. Protected modification operations require the `MANAGER` role.                             |

`PLATFORM_ADMIN` is a global account attribute; `MANAGER` and `VIEWER` are assigned per estate. A platform administrator may also hold an estate role; global access does not itself create estate membership. These are product security roles, not definitions of separate business responsibilities. A platform administrator is not thereby a property-business manager, owner, or tenant. See the [user account concept](./concepts/user-account.md) and [estate and access concept](./concepts/estate-and-access.md). Implementation references: `EstateRole`, `EstateSecurityService`, `TransactionController`, and `TransactionImportController`.

The sign-in mechanism, session lifecycle, and current-user endpoint are documented separately in [Authentication](../technical/authentication.md). Authentication establishes a user's identity; the roles above and estate membership are used for authorization.

## Governance rules currently observed

Constraints on managers are listed in the [estate rules](./rules/estate.md). They should not be treated as a complete definition of business governance.
