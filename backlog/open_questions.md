# Open Questions

This file contains concrete business or product decisions that still need to be addressed. Do not treat these unresolved items as established domain rules.

## User accounts

- Does the global `PLATFORM_ADMIN` account flag intentionally replace the legacy per-account `ADMIN` role described in UC-002 and its user stories?
- Should sign-in usernames be case-sensitive? Account creation and update check username uniqueness without regard to case, while the login lookup uses the entered username as-is.
- Should changing `isPlatformAdmin` be prevented when it would leave no platform administrator, as account deletion is currently prevented for the last platform administrator?
- Should a user account be linked to a `Person` record, or should the two remain separate?

## Scope and roles

- What does an "estate" represent in the organization: a company, estate, management mandate, client, or another scope?
- What specific responsibilities distinguish a manager from a viewer? Are there other business roles?
- Should counterparties be modeled as a managed business concept, and which counterparty categories are relevant?
- Is an owner always an individual in the documented scope? Could the owner be a company or a condominium association?
- What is a guarantor's business status in a lease, distinct from a tenant or co-tenant?
- Must a estate always have at least one manager, including when it contains no members or properties?
- What business conditions and consequences should apply when deleting a estate?
- Should one person be allowed to have multiple roles or belong to multiple portfolios?
- Should duplicate member entries in an initial estate-creation request be rejected or ignored?

## Property and rental

- Does a housing unit inherit the building's owner when no unit-specific owner is recorded, or does the missing owner mean ownership is unknown?
- Can a building or housing unit have multiple owners? If so, should ownership shares and their changes over time be documented?
- What is the ownership rule when a building and one of its housing units have different owners?
- Which business events and criteria move a lease from draft to active, finished, or cancelled?
- Can lease periods for the same housing unit overlap?
- Which maintenance or inspection obligations apply to boilers and fire extinguishers, and who is responsible for meeting them?
- Can a piece of equipment be associated with both a building and one of its housing units at the same time, or must it have exactly one management context?
- Are the currently supported boiler fuel and utility-meter categories complete?
- What validity and alerting rules should apply to fire-extinguisher revisions?
- check the statuses of the leases. See business concept documentation.
- In a lease, changing the role of a person needs validation. Is that correct?

## Transactions

- What exactly do `DRAFT`, `CONFIRMED`, and `RECONCILED` mean for the business? Who confirms or reconciles a movement, and what evidence is required?
- Can a `RECONCILED` transaction ever be changed?
- Can a lease be associated with an expense in some business cases, or should that association be restricted to income?
- Are equipment links limited to expenses? Can one transaction be split across multiple equipment items?
- If several equipment items are linked, does an omitted amount assign the full amount to each one, or should there be at most one link without an amount?
- What is the accounting-month assignment policy: bank transaction date, value date, rent period, or a category-specific rule?
- What is the business rule for identifying a duplicate bank movement? Are the same date, amount, and counterparty sufficient, and what is the scope over time?
- Must an automatically learned classification always be confirmed by a manager before it is considered reliable?
