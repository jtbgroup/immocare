---
title: Person Rules
type: business rules
status: draft
last_verified: 2026-11-05
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Business Rules — Persons

These rules describe constraints visible in the current implementation. Code-derived rules need business validation; the implementation alone does not establish business policy.

| ID | Rule | Status |
|---|---|---|
| BR-PERSON-001 | A person record belongs to one estate. | Needs business validation |
| BR-PERSON-002 | A person record must include both a first name and a last name. | Needs business validation |
| BR-PERSON-003 | Within an estate, a national ID cannot be assigned to two person records when compared without regard to case. | Needs clarification |
| BR-PERSON-004 | Deletion is blocked while a person is an owner of a building or housing unit, or is associated with a `DRAFT` or `ACTIVE` lease. | Needs clarification |
| BR-PERSON-005 | Within an estate, an IBAN cannot be registered on more than one person bank account when compared without regard to case. | Needs clarification |
| BR-PERSON-006 | When one bank account for a person is marked primary, that person's other bank accounts are not primary. | Needs business validation |

BR-PERSON-003 needs clarification because the service checks uniqueness per estate, the JPA mapping declares a unique column, and the Flyway schema declares no national-ID unique constraint. BR-PERSON-004 needs clarification because the legacy specification only blocks active lease associations, while the service blocks both draft and active leases; the database also restricts deletion for every lease membership. BR-PERSON-005 needs clarification because the legacy specification says global IBAN uniqueness, while service checks and the Flyway unique constraint are estate-scoped; the JPA mapping declares the IBAN column unique by itself.

The open decisions about uniqueness scope, owner estate scope, deletion with historical lease memberships, picker search and national-ID display, lease suggestion selection, IBAN normalization, and primary-account cardinality are collected in [Open Questions](../../../../backlog/open_questions.md#people-and-bank-accounts).

## Observed sources

`PersonController`, `PersonService`, `PersonRepository`, `PersonBankAccountController`, `PersonBankAccountService`, `PersonBankAccountRepository`, `Person`, `PersonBankAccount`, `CreatePersonRequest`, `UpdatePersonRequest`, `SavePersonBankAccountRequest`, `V004__uc004_manage_persons.sql`, and `V014__uc014_manage_leases.sql`.
