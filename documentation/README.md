# Project Documentation

This documentation describes the property management domain independently of the current application's screens and structure, alongside selected technical references for behavior that must be understood across the application.

## Methodology

The rules for creating and maintaining the documentation are defined in [methodology.md](./methodology.md).

## Start here

- [Roles and responsibilities](./business/roles.md) — who is involved, what each role owns, and how access is separated from business responsibilities.
- [Concise glossary](./business/glossary.md) — shared vocabulary and canonical terms used throughout the domain.
- [Concept map](./business/concept-map.md) — relationships between core business objects and their main interactions.
- [Portfolio and rental concepts](./business/concepts/portfolio-and-rental.md) — portfolio structure, properties, people, and lease-related concepts.
- [Property equipment and assets](./business/concepts/property-equipment.md) — equipment, fixtures, and tracked property assets.
- [Equipment rules](./business/rules/equipment.md) — rules governing equipment attachment, lifecycle, and validity.
- [Financial transaction concept](./business/concepts/financial-transaction.md) — income, expense, classification, and linkage concepts for financial operations.
- [Initial portfolio rules](./business/rules/portfolio.md) — validated or observed portfolio-level business rules.
- [Initial transaction rules](./business/rules/financial-transactions.md) — rules governing financial transaction behavior and classification.

## Technical references

- [Authentication](./technical/authentication.md) — current sign-in, sign-out, session, and current-user behavior, plus its boundary with authorization.

## Backlog

- [Backlog](../backlog/backlog.md) — planned product evolutions and implementation work.
- [Open questions](../backlog/open_questions.md) — unresolved decisions that still need treatment.

## Documentation status

The business pages describe the domain model and behavior represented in the repository; inferred business rules still require validation where marked. Technical references describe the implementation currently present in the codebase.
