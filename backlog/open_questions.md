# Open Questions

This file contains concrete business or product decisions that still need to be addressed. Do not treat these unresolved items as established domain rules.

## User accounts

- When a platform administrator also has an explicit estate membership, should the estate-list response expose that membership's role instead of returning `myRole = null`?

## Scope and roles

- What data should move with a building when it is transferred to another estate? Decide this using a concrete transfer scenario.
- Should counterparties be modeled as a managed business concept, and which counterparty categories are relevant?
- Is an owner always an individual in the documented scope? Could the owner be a company or a condominium association?
- What is a guarantor's business status in a lease, distinct from a tenant or co-tenant?
- Should one person be allowed to have multiple roles or belong to multiple portfolios?

## Property and rental

### Buildings

- Should the building API require country on every request, or should an omitted or blank country default to Belgium as the legacy specification and creation form suggest?
- Which status code should the API return when building deletion is blocked by housing units? The UC says `400`, its delete story says `409`, and current code returns `400`.
- When city and free-text search are combined, should the search term also match the city, or only the building name and street address? The UC and search story specify different fields.
- Should an unknown building owner ID return `404` as the current person-not-found handler does, or `409` as the legacy building requirements specify?

### Housing units and leases

- Does a housing unit inherit the building's owner when no unit-specific owner is recorded, or does the missing owner mean ownership is unknown?
- Can a building or housing unit have multiple owners? If so, should ownership shares and their changes over time be documented?
- What is the ownership rule when a building and one of its housing units have different owners?
- Should creation and update both require a positive surface when a terrace or garden is recorded? The update service enforces this, but creation does not (BR-HUNIT-008).
- Which associated data should prevent deletion of a housing unit, and which may be deleted or detached with it? Current behavior blocks rooms and fire extinguishers, cascades PEB and rent history, is restricted by leases, detaches transaction links, and does not clean up generic boiler or meter owner references (BR-HUNIT-006).
- Should the computed sum of room surfaces update `housing_unit.total_surface`, or remain a separate room-composition measure?
- For a room batch with one invalid non-empty row, should the system reject the whole batch or create the valid rows? The use-case analysis requires all-or-nothing validation, while a related story says valid rows can still be saved (BR-HUNIT-015).
- Which business events and criteria move a lease from draft to active, finished, or cancelled?
- Can lease periods for the same housing unit overlap?
- Which maintenance or inspection obligations apply to boilers and fire extinguishers, and who is responsible for meeting them?
- Can a piece of equipment be associated with both a building and one of its housing units at the same time, or must it have exactly one management context?
- Are the currently supported boiler fuel and utility-meter categories complete?
- What validity and alerting rules should apply to fire-extinguisher revisions?
- check the statuses of the leases. See business concept documentation.
- In a lease, changing the role of a person needs validation. Is that correct?

### PEB scores

- How should the active PEB record be selected when a record has no validity end date or multiple records share the most recent validity end date? The confirmed active-record definition uses the most recent validity end date (BR-HUNIT-019), but the request permits a missing date and no tie-breaker is defined (BR-HUNIT-021).
- Does the rule that only platform administrators may modify PEB history also apply to deleting records, or may estate managers delete the active record? The current API lets estate managers delete any record (BR-HUNIT-020, BR-HUNIT-024).
- Should the housing-unit detail warning use the estate-configured PEB alert threshold, or remain fixed at three months? The detail response uses three months while estate alerts use the configurable setting (BR-HUNIT-022, BR-HUNIT-023).

## People and bank accounts

- Should the same natural person be represented by one shared record across estates or by a separate estate-scoped record in each estate? The current model scopes each person record to one estate (BR-PERSON-001).
- Should national IDs be unique globally, or only within an estate? The service checks within an estate, while the legacy requirement says global uniqueness and the entity/schema constraints disagree (BR-PERSON-003).
- Should IBANs be unique globally, or only within an estate? The application and Flyway constraint are estate-scoped, while the legacy requirement says global uniqueness and the entity mapping disagrees (BR-PERSON-005).
- Must a building or housing-unit owner belong to the same estate as that property? Current owner assignment accepts a person ID without checking estate ownership (BR-PERSON-001).
- Should a person remain undeletable while referenced by any lease, including finished or cancelled leases, or should those historical associations be removable/archivable? The service blocks draft and active leases, but the database restricts all lease memberships (BR-PERSON-004).
- Should person autocomplete search include email, and should it expose the full national ID or only a masked suffix?
- Which lease should an IBAN match suggest during transaction import: only a lease in the transaction's estate, a lease covering the transaction date, an active lease preference, or a closest-date fallback? The current implementation selects the latest-starting lease and does not use transaction date.
- Should valid IBAN input be accepted with lowercase letters and/or whitespace and normalized before storage?
- Is a primary bank account optional, and should there be at most one primary account per person (BR-PERSON-006)?

## Transactions

- What exactly do `DRAFT`, `CONFIRMED`, and `RECONCILED` mean for the business? Who confirms or reconciles a movement, and what evidence is required?
- Can a `RECONCILED` transaction ever be changed?
- Can a lease be associated with an expense in some business cases, or should that association be restricted to income?
- Are equipment links limited to expenses? Can one transaction be split across multiple equipment items?
- If several equipment items are linked, does an omitted amount assign the full amount to each one, or should there be at most one link without an amount?
- What is the accounting-month assignment policy: bank transaction date, value date, rent period, or a category-specific rule?
- What is the business rule for identifying a duplicate bank movement? Are the same date, amount, and counterparty sufficient, and what is the scope over time?
- Must an automatically learned classification always be confirmed by a manager before it is considered reliable?
