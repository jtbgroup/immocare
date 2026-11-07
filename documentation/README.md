---
title: Project Documentation
type: documentation index
status: current
last_verified: 2026-11-05
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Project Documentation

This documentation describes the property management domain independently of the current application's screens and structure, alongside selected technical references for behavior that must be understood across the application.

## Methodology

The rules for creating and maintaining the documentation are defined in [methodology.md](./methodology.md).

## Start here

- [Roles and responsibilities](./business/roles.md) — who is involved, what each role owns, and how access is separated from business responsibilities.
- [Capabilities](./business/capabilities.md) — current application capabilities and their implementation status.
- [Concise glossary](./business/glossary.md) — shared vocabulary and canonical terms used throughout the domain.
- [Concept map](./business/concept-map.md) — relationships between core business objects and their main interactions.
- [Estate and access](./business/concepts/estate-and-access.md) — estate scope, membership, and the difference between global and estate roles.
- [User account concept](./business/concepts/user-account.md) — application identity, platform administration, and its distinction from a person.
- [Estate and rental concepts](./business/concepts/estate-and-rental.md) — estate structure, properties, people, and lease-related concepts.
- [Property equipment and assets](./business/concepts/property-equipment.md) — equipment, fixtures, and tracked property assets.
- [Equipment rules](./business/rules/equipment.md) — rules governing equipment attachment, lifecycle, and validity.
- [User account rules](./business/rules/users.md) — observed account and access constraints requiring business validation.
- [Person rules](./business/rules/persons.md) — observed person and person-bank-account constraints and their validation status.
- [Financial transaction concept](./business/concepts/financial-transaction.md) — income, expense, classification, and linkage concepts for financial operations.
- [Initial estate rules](./business/rules/estate.md) — validated or observed estate-level business rules.
- [Initial transaction rules](./business/rules/financial-transactions.md) — rules governing financial transaction behavior and classification.

## Technical references

- [Technology stack and deployment](./technical/technology-stack-and-deployment.md) — confirmed architecture and stack targets, current deployment differences, and documentation boundaries.
- [Authentication](./technical/authentication.md) — current sign-in, sign-out, session, and current-user behavior, plus its boundary with authorization.
- [User account management](./technical/user-management.md) — current account administration, validation, platform-admin access, and deletion rules.
- [Person management](./technical/person-management.md) — current person, owner, lease-picker, and person-bank-account behavior.
- [Estate management](./technical/estate-management.md) — current estate APIs, access checks, estate selection, membership, and dashboard behavior.
- [Account and estate data dictionary](./technical/data-dictionary.md) — current persisted fields and constraints for accounts, estates, and memberships.
- [Person data dictionary](./technical/person-data-dictionary.md) — persisted fields and constraints for people and their bank accounts.
- [User, estate, and person data model](./technical/data-model.md) — current account, estate, person, ownership, and lease-membership relationships.

## Backlog

- [Backlog](../backlog/backlog.md) — planned product evolutions and implementation work.
- [Open questions](../backlog/open_questions.md) — unresolved decisions that still need treatment.

## Documentation status

The business pages describe the domain model and behavior represented in the repository; inferred business rules still require validation where marked. Technical references describe the implementation currently present in the codebase.
