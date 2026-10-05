# Roles and Responsibilities

Distinguish **people's roles in the property business** from the **application access roles** currently implemented. The code primarily describes access rights; it does not fully define the organization's structure or business delegations.

## Business roles identified so far

| Role / relationship | Representation currently visible in the application |
|---|---|---|
| Portfolio manager | `MANAGER` is an application access role that permits management operations within a portfolio. |
| Property owner | A person can be associated as the owner of a building or housing unit. |
| Tenant / occupant | People associated with a lease have one of the technical relationships `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. |
| Third party / counterparty | Financial movements can include counterparty information; the model does not define a separate set of business roles for counterparties. |

These labels describe relationships and access concepts represented in the application. They are not an approved responsibility matrix.

## Application access roles currently observed

| Access role | Observed scope and rights |
|---|---|
| `PLATFORM_ADMIN` | Cross-portfolio access; treated as both a manager and a member in portfolio-scoped access checks. |
| `MANAGER` | A portfolio member allowed to perform management operations. This role is required, among other things, to create, update, delete, import, or confirm transactions. |
| `VIEWER` | A portfolio member who passes the read-access check shared with managers. Protected modification operations require the `MANAGER` role. |

`PLATFORM_ADMIN` is a global account attribute; `MANAGER` and `VIEWER` are assigned per portfolio. These are product security roles, not definitions of business functions. Implementation references: `EstateRole`, `EstateSecurityService`, `TransactionController`, and `TransactionImportController`.

The sign-in mechanism, session lifecycle, and current-user endpoint are documented separately in [Authentication](../technical/authentication.md). Authentication establishes a user's identity; the roles above and estate membership are used for authorization.

## Governance rules currently observed

Constraints on managers are listed in the [portfolio rules](./rules/portfolio.md). They should not be treated as a complete definition of business governance.
