---
title: Lease
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Concept — Lease

A **lease** (`Lease`) concerns a [housing unit](../../buildings-and-housing-units/concepts/buildings-and-housing-units.md). It groups contractual information, dates, rent and charge amounts, and associated [people](../../people/concepts/person.md).

The statuses currently represented are `DRAFT`, `ACTIVE`, `FINISHED`, and `CANCELLED`. The code treats a `DRAFT` or `ACTIVE` lease as editable; this does not establish the business conditions for transitioning between statuses.

People associated with a lease have one of the technical roles `PRIMARY`, `CO_TENANT`, or `GUARANTOR`. The precise meaning and responsibilities of these roles, and the rules governing changes to the people on a lease, need validation.

Lease statuses and participant roles are represented in the cross-cutting [data model](../../../technical/cross-cutting/data-model.md). Current lease-end and indexation alert counts shown on the estate dashboard are described in [Estate Management](../../../technical/estates-and-access/estate-management.md); alert handling can span multiple domains, as described in [Alerts](../../cross-cutting/concepts/alerts.md).
