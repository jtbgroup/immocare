# Documentation Methodology

This document defines the rules for creating and maintaining the project's canonical documentation.

## Scope

The business documentation layer is the source of truth for the domain model as it is currently represented in the codebase and validated by the business context. It describes:

- the business domain,
- the vocabulary used in the company,
- the roles and responsibilities of people,
- the relationships between business objects,
- the rules that govern the domain,
- the business constraints that are already reflected in the implementation and accepted as valid.

It does not describe:

- speculative future states,
- user journeys,
- screens or UI flows,
- implementation-level use cases,
- application endpoints,
- persistence models,
- programming choices,
- internal technical architecture.

Business documentation must reflect the reality of the current code and validated business understanding. It must not include unresolved questions as if they were domain facts.

## Principles

1. All canonical documentation is written in English.
2. The business domain is the source of truth; the code is only one implementation view of that domain.
3. Business documentation is organized by meaning, not by feature backlog artifacts.
4. Use cases and user stories do not belong in the canonical business documentation layer.
5. Business rules receive stable identifiers and can be referenced without duplication.
6. Rules inferred from current code or product behavior must be marked as **Needs business validation** until confirmed by a business stakeholder.
7. Technical details belong in separate technical documentation.
8. The glossary is the shared vocabulary for the entire project.

## Language policy

The project documentation must be written in English. French content is not considered canonical unless explicitly retained as historical context.

## Documentation structure

The canonical documentation is organized by purpose:

- Business references describe the current domain: shared vocabulary, concepts and their relationships, roles, and rules.
- Technical references describe behavior and constraints implemented by the application.
- The documentation root README is the navigation index for the current pages; this methodology does not duplicate that index.
- The repository-root `/backlog` folder contains two separate files: `backlog.md` for future evolutions and `open_questions.md` for concrete, unresolved items that still need treatment.

Each document must have a clear, single purpose. Keep domain definitions, implemented behavior, unresolved decisions, and future work in their respective categories rather than mixing them.

## Separation between backlog and open questions

The two files in `/backlog` have separate purposes:

- `backlog.md` contains future evolutions, roadmap items, and planned improvements. These are not part of the canonical business domain description.
- `open_questions.md` contains unresolved questions that are concrete, grounded in the current domain or code, and still need a decision.
- Business documentation must not contain speculative questions or internal product ideas that are not clearly anchored in current business reality.

## Ownership of the documentation

Each document must have a clear purpose and remain limited to that purpose. Business references describe the domain; technical references describe implemented system behavior. Neither category should reproduce backlog items or use-case/story decomposition.

## Required use of this method

When adding or updating business documentation, the author must ensure that the new content:

- fits one of the documented business categories,
- uses the project glossary,
- respects the English-only policy,
- avoids use-case or story-style decomposition,
- distinguishes business rules from technical implementation.

## Validation rule

Any rule that is inferred from existing code or current behavior without explicit business confirmation must be marked as:

- **Needs business validation**

The documentation should never present such a rule as a confirmed business rule without that status.
