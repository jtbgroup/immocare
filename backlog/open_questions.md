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

- Should durations in housing-unit rent history count complete elapsed calendar months or count calendar months inclusively? For example, should 2024-01-01 through 2024-06-30 be five or six months?
- May estate managers edit and delete existing housing-unit rent-history records, or should history be append-only?
- Should two housing-unit rent-history records be allowed to have the same effective-from date?
- Which external lease-regulation schemes and jurisdictions should the application support, and how should their requirements be represented without directly associating a regulated reference rent with a lease (BR-LEASE-001)?
- Does a housing unit inherit the building's owner when no unit-specific owner is recorded, or does the missing owner mean ownership is unknown?
- Can a building or housing unit have multiple owners? If so, should ownership shares and their changes over time be documented?
- What is the ownership rule when a building and one of its housing units have different owners?
- Should creation and update both require a positive surface when a terrace or garden is recorded? The update service enforces this, but creation does not (BR-HUNIT-008).
- Which associated data should prevent deletion of a housing unit, and which may be deleted or detached with it? Current behavior blocks rooms and fire extinguishers, cascades PEB and rent history, is restricted by leases, detaches transaction links, and does not clean up generic boiler or meter owner references (BR-HUNIT-006).
- Should the computed sum of room surfaces update `housing_unit.total_surface`, or remain a separate room-composition measure?
- For a room batch with one invalid non-empty row, should the system reject the whole batch or create the valid rows? The use-case analysis requires all-or-nothing validation, while a related story says valid rows can still be saved (BR-HUNIT-015).
- Are the implemented lease status transitions (`DRAFT` to `ACTIVE` or `CANCELLED`; `ACTIVE` to `FINISHED` or `CANCELLED`) the intended business lifecycle, and should finishing or cancelling retain an effective date and notes (BR-LEASE-004)? The status API accepts those fields but currently ignores them.
- Can leases for the same housing unit have overlapping date ranges, and should activating a draft be rejected when another lease is already active (BR-LEASE-002)? Creation is blocked while a draft or active lease exists, but status activation does not repeat that check and historical date ranges are not compared.
- Which maintenance or inspection obligations apply to boilers and fire extinguishers, and who is responsible for meeting them?
- Can a piece of equipment be associated with both a building and one of its housing units at the same time, or must it have exactly one management context?
- Are the currently supported boiler fuel and utility-meter categories complete?
- Do meters have validity, inspection, or maintenance intervals that should trigger alerts, and what warning period should apply?
- Should meter history include readings or maintenance events beyond the active and closed meter records retained during replacement?
- Should an extinguisher with no revision, or whose latest revision is more than one year old, count as needing attention on the estate dashboard (BR-EQ-031)? If so, should that interval be configurable?
- Should boilers follow the analyzed append-only replacement history, or may users edit and delete boiler records as the current application allows? The database has a `removal_date` column, but the current entity and API do not use it.
- Is a boiler brand required, and must its installation date be today or earlier? The earlier requirements specify both, while the current API permits a missing brand and a future installation date (BR-EQ-029, BR-EQ-030).
- Is the current validation that rejects a last-service date before an out-of-service date intentional (BR-EQ-018)?
- May a boiler service's explicitly supplied validity end date be before its service date? The current API accepts the override without a date-order check (BR-EQ-021).
- Must a fire extinguisher be owned either by a building or by a housing unit, or should a building-owned extinguisher also support an optional unit assignment? The current schema requires exactly one owner, unlike the earlier analysis (BR-EQ-026).
- Should fire-extinguisher revision history be append-only, or may individual revision records be deleted? The analysis describes append-only history, while a related requirement and the current API permit deletion (BR-EQ-028).
- Should a fire-extinguisher identification number be unique per direct owner, or across a building and all of its housing units? The current implementation checks uniqueness within the direct owner context (BR-EQ-024).
- Should the building-level extinguisher list include extinguishers assigned directly to its housing units, or remain limited to extinguishers owned by the building?
- Should multiple active meters of the same type be allowed for one building or housing unit (BR-EQ-011)?
- Should the optional meter replacement reason be retained as part of meter history?
- How should meter duration be calculated when the start and end dates do not fall on the same day of the month?
- Should a manager be able to change a person's role on a lease directly, and what validation should apply? The current API supports adding and removing participants but has no role-update operation.
- Should leases in `DRAFT` and `CANCELLED` status be deletable, while `ACTIVE` and `FINISHED` leases remain undeletable (BR-LEASE-010)?
- Should lease indexation retain dedicated details such as index value, index month, and notification date, or is a rent adjustment with an effective date and reason sufficient? The current implementation only stores general rent and charge adjustments.
- Should lease end-notice alerts apply to draft leases and remain visible after the lease end date? Current detail and unit-summary flags are not status-gated, and the contextual alert query includes drafts and does not expire an alert after the end date.
- Should cancelled leases be grouped with finished lease history on the housing-unit page? The current page places every non-finished lease, including cancelled ones, in its main list.

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

## Application and estate configuration

- How should the estate-configuration registry be defined and governed: where should supported keys, data types, defaults, validation, editability, and any platform fallback or override behavior be declared? These definitions are currently distributed across `EstateConfigDTOs.DEFAULT_CONFIG`, the configuration services, and their consumers.
- Should equipment-to-subcategory mappings be configured per estate or globally? Mapping keys are currently seeded per estate, but the mapping API operations and editor are not implemented.

## Transactions

- What exactly do `DRAFT`, `CONFIRMED`, and `RECONCILED` mean for the business? Who confirms or reconciles a movement, and what evidence is required? The creation choices and default are settled in BR-TX-012; the status meanings and reconciliation process remain open.
- Is `CANCELLED` an intended transaction status? The database constraint permits it, but the Java status enum does not.
- Can a `RECONCILED` transaction ever be changed? BR-TX-005 currently reflects an implementation restriction; its business rationale has not been confirmed.
- Should the generated transaction reference and/or transaction source be editable when a transaction is edited? The current form displays the reference but exposes neither field in the update request.
- Can a lease be associated with an expense in some business cases, or should that association be restricted to income?
- Are equipment links limited to expenses? The form hides them for income, but the API currently accepts links for either direction. Can one transaction be split across multiple equipment items?
- If several equipment items are linked, does an omitted amount assign the full amount to each one, or should there be at most one link without an amount?
- For the confirmed allocation of a transaction across multiple costs (BR-TX-011), what should each allocation identify, and must allocation amounts sum to the full transaction amount?
- What is the accounting-month assignment policy: bank transaction date, value date, rent period, or a category-specific rule? Manual entry currently proposes the current month on load and does not update that proposal when the subcategory or counterparty account changes.
- What is the business rule for identifying a duplicate bank movement? Are the same date, amount, and counterparty sufficient, and what is the scope over time?
- Should a subcategory be required for a manually entered transaction? The historical creation criteria say it is required, while the current form and API allow it to be omitted.
- Should manually created or edited transactions reinforce automatic classification and accounting-month learning rules? The current save path does this, but the suggestion behavior is outside the current manual-entry documentation scope.
- Must an automatically learned classification always be confirmed by a manager before it is considered reliable?
