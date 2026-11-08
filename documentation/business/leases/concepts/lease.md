---
title: Lease
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: "Lease, LeaseRentAdjustment, LeaseService, LeaseController, V014__uc014_manage_leases.sql, stakeholder confirmation 2026-11-07"
---

# Concept — Lease

A **lease** (`Lease`) concerns a [housing unit](../../buildings-and-housing-units/concepts/buildings-and-housing-units.md). It groups contractual information, dates, rent and charge amounts, and associated [people](../../people/concepts/person.md).

The statuses currently represented are `DRAFT`, `ACTIVE`, `FINISHED`, and `CANCELLED`. The code treats a `DRAFT` or `ACTIVE` lease as editable; these lifecycle constraints and their validation status are recorded in [Lease Rules](../rules/leases.md).

People associated with a lease have one of the technical roles `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. The precise meaning and responsibilities of these roles, and the rules governing changes to the people on a lease, need validation.

The lease records its own contractual monthly rent and charges, with dated adjustment records. Its rent is distinct from the theoretical housing-unit rent, which serves as a basis for setting it ([BR-HUNIT-027](../../buildings-and-housing-units/rules/housing-units.md)).

Leases may be subject to external regulation established outside the application. Such regulation may govern lease terms, but a regulated reference rent is not directly associated with a lease in the domain model ([BR-LEASE-001](../rules/leases.md)). The application does not currently record or apply these requirements; the schemes and jurisdictions to support remain an [open question](../../../backlog/open_questions.md#housing-units-and-leases).

Lease statuses, participant roles, and rent-adjustment relationships are represented in the cross-cutting [data model](../../../technical/cross-cutting/data-model.md). The implemented API, lifecycle, adjustments, and alert calculations are documented in [Lease Management](../../../technical/leases/lease-management.md). Current lease-end and indexation alert counts shown on the estate dashboard are described in [Estate Management](../../../technical/estates-and-access/estate-management.md); alert handling can span multiple domains, as described in [Alerts](../../cross-cutting/concepts/alerts.md).
