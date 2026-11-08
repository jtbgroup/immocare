---
title: Person Management
type: technical reference
status: current
last_verified: 2026-11-08
verified_against: "PersonBankAccountRepository, TransactionImportService, person bank-account rules and open questions, source reviewed 2026-11-08"
---

# Person Management

This page describes the person registry, ownership links, person bank accounts, and current implementation behavior. The domain concepts and candidate business constraints are documented in [Person](../../business/people/concepts/person.md) and [Person Rules](../../business/people/rules/persons.md). Persisted fields and database constraints are listed in the [Person Data Dictionary](./person-data-dictionary.md).

## Estate scope and access

Person records and person bank-account routes include an estate ID. Members of that estate may read the person list, picker results, details, and bank accounts. Creating, editing, or deleting a person, and creating, editing, or deleting a person bank account, require manager access. Platform administrators pass these estate access checks.

The backend verifies that a person belongs to the requested estate for person detail, update, delete, and bank-account operations. List and picker queries are filtered by estate.

## Person records

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| List persons | `GET /api/v1/estates/{estateId}/persons?page=0&size=20&sort=lastName,asc&search=...` | Estate member | Returns a paginated summary; page defaults to `0`, size to `20`, sort to last name ascending. Optional case-insensitive partial search matches first name, last name, either name order, email, or national ID. |
| Search picker | `GET /api/v1/estates/{estateId}/persons/picker?q=...` | Estate member | Returns at most 10 results, ordered by last name then first name. Queries shorter than two characters return an empty list. |
| Read person | `GET /api/v1/estates/{estateId}/persons/{id}` | Estate member | Returns person fields and derived ownership, lease, and bank-account details. |
| Create person | `POST /api/v1/estates/{estateId}/persons` | Estate manager | Creates a person in the specified estate; returns `201 Created`. |
| Update person | `PUT /api/v1/estates/{estateId}/persons/{id}` | Estate manager | Replaces the editable person fields; returns `200 OK`. |
| Delete person | `DELETE /api/v1/estates/{estateId}/persons/{id}` | Estate manager | Returns `204 No Content` when deletion succeeds; references may prevent deletion. |

The list summary contains the person's name, city, national ID, and derived owner and tenant flags. The UI shows last name, first name, city, national ID, and owner/tenant badges. The `tenant` flag currently means that the person has any lease membership; it is not limited to active leases.

Person details include identity and contact fields, owned buildings, directly owned housing units, lease memberships with their statuses, derived owner/tenant flags, and registered person bank accounts. The lease list includes all statuses.

### Search picker

The picker currently searches names and national ID; it does not search email, despite that criterion in the legacy picker specification. Its result contains the full national ID when present. The picker is used when assigning owners to buildings or units and when adding people to leases.

### Create and update validation

- Last name and first name are required and limited to 100 characters.
- Birth date cannot be in the future.
- National ID is optional and limited to 20 characters. The service checks case-insensitive duplicates within the estate.
- GSM is limited to 20 characters; email is optional, must be a valid email when supplied, and is limited to 100 characters.
- Birth place and city are limited to 100 characters, street address to 200, postal code to 20, and country to 100.
- A new person defaults to country `Belgium` when no country is supplied.

The Flyway schema does not declare a unique constraint on `person.national_id`, while the JPA entity mapping marks the column unique. Hibernate is configured to validate rather than create the schema. The service's duplicate check is estate-scoped. The intended uniqueness scope therefore needs clarification; see [Person Rules](../../business/people/rules/persons.md).

## Ownership links

Building and housing-unit create/update operations accept an optional owner ID. A null ID clears the direct owner. Housing units can also expose an effective owner name inherited from their building when they have no direct owner.

The current services load a selected owner by person ID but do not verify that the person belongs to the building or unit's estate. Person detail calculations also look up owner references by person ID without an estate filter. This does not enforce the estate boundary for ownership links; the gap is tracked in the [backlog](../../../backlog/backlog.md).

Building API, list, search, and deletion behavior is documented in [Building Management](../buildings-and-housing-units/building-management.md).

## Person bank accounts

Person bank-account routes are nested under the estate and person:

| Operation | Endpoint | Access | Result |
|---|---|---|---|
| List bank accounts | `GET /api/v1/estates/{estateId}/persons/{personId}/bank-accounts` | Estate member | `200 OK`; primary account first, then creation time ascending. |
| Add bank account | `POST /api/v1/estates/{estateId}/persons/{personId}/bank-accounts` | Estate manager | `201 Created`. |
| Update bank account | `PUT /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}` | Estate manager | `200 OK`. |
| Delete bank account | `DELETE /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}` | Estate manager | `204 No Content`. |

The request requires an IBAN and permits an optional label and primary flag. Request validation currently accepts uppercase IBAN text without whitespace. Service normalization removes whitespace and converts to uppercase after request validation, so lowercase or spaced values are rejected before that normalization can run. When an account is marked primary, the service clears the primary flag from the person's other accounts; no primary account is required.

Duplicate checks compare IBANs case-insensitively within an estate. The Flyway schema also scopes the unique constraint to `(iban, estate_id)`, while the JPA entity marks the IBAN column individually unique. The mismatch and the conflict with the legacy specification's global-uniqueness criterion need clarification.

## Ownership and deletion behavior

The person deletion service rejects deletion when the person owns any building or housing unit, or is attached to a `DRAFT` or `ACTIVE` lease. It returns `409 Conflict` with the related building, unit, and lease lists. Registered person bank accounts cascade-delete when the person is deleted.

The database restricts deletion of a person referenced by any lease membership, including `FINISHED` or `CANCELLED` leases. The service does not include those historical memberships in its explicit check, so deletion can reach a database constraint failure instead of the structured `409` response. The legacy requirement and the persisted constraint need a product decision; see [Open Questions](../../../backlog/open_questions.md#people-and-bank-accounts).

## IBAN-based lease suggestions during import

Transaction-import behavior for IBAN matching and lease suggestions is documented in
[Transaction Import Management](../financial-transactions/transaction-import-management.md).
The import lookup is not estate-filtered when searching by IBAN, so a matching IBAN
in multiple estates can be ambiguous. The broader question of IBAN uniqueness and
matching remains in [Open Questions](../../../backlog/open_questions.md#people-and-bank-accounts).

## API errors and implementation differences

- A person ID not found returns `404 Not Found`; attempting to access a person from another estate is denied.
- Duplicate national IDs and duplicate IBANs are currently surfaced through generic conflict handling.
- Invalid request fields covered by Jakarta validation return `400 Bad Request`.
- A person linked to a finished or cancelled lease may pass the service's reference check but fail the database's `ON DELETE RESTRICT` constraint; that path is not handled as the structured person-reference conflict.
- The legacy specification describes a global person registry and an `ADMIN` actor. Current APIs are estate-scoped, with member reads and manager writes; `PLATFORM_ADMIN` also passes the estate checks.

## Implementation references

- Person endpoints and access: [PersonController](../../../backend/src/main/java/com/immocare/controller/PersonController.java)
- Person listing, validation, associations, and deletion: [PersonService](../../../backend/src/main/java/com/immocare/service/PersonService.java)
- Person and bank-account routes: [PersonBankAccountController](../../../backend/src/main/java/com/immocare/controller/PersonBankAccountController.java)
- Bank-account normalization, uniqueness, and primary selection: [PersonBankAccountService](../../../backend/src/main/java/com/immocare/service/PersonBankAccountService.java)
- Person search and picker queries: [PersonRepository](../../../backend/src/main/java/com/immocare/repository/PersonRepository.java)
- Owner assignment: [BuildingService](../../../backend/src/main/java/com/immocare/service/BuildingService.java) and [HousingUnitService](../../../backend/src/main/java/com/immocare/service/HousingUnitService.java)
- IBAN-based lease suggestions: [TransactionImportService](../../../backend/src/main/java/com/immocare/service/TransactionImportService.java)
- Frontend person management: [person service](../../../frontend/src/app/core/services/person.service.ts), [person bank-account service](../../../frontend/src/app/core/services/person-bank-account.service.ts), and [person components](../../../frontend/src/app/features/person/)
- Person picker: [PersonPickerComponent](../../../frontend/src/app/shared/components/person-picker/person-picker.component.ts)
