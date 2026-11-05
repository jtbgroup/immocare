---
title: Estate and Access
type: business concepts
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Concept — Estate and Access

## Estate

An **estate** is the application scope to which property and related management data are assigned. Buildings, people, leases, transactions, and estate-specific configuration are associated with an estate. The code does not establish whether an estate corresponds to a company, a property portfolio, a management mandate, or another legal or organizational unit; this remains an [open question](../../../backlog/open_questions.md#scope-and-roles).

## Estate membership

An **estate membership** associates a user account with one estate and an estate-scoped access role. A user account can have memberships in multiple estates, and the role is independent for each membership.

The application currently defines two estate roles:

- **Manager** — estate-scoped access that permits management operations.
- **Viewer** — estate-scoped read access.

These are application access roles, not an approved description of business responsibilities. Their observed access boundaries are summarized in [Roles and Responsibilities](../roles.md); implementation behavior is documented in [Estate Management](../../technical/estate-management.md).

## Platform administrator

A **platform administrator** has global account-level access. The application allows platform administrators to access estates without an estate membership; the estate list represents this transversal access with no estate-specific role. This access model is independent of whether the platform administrator also has an estate membership.

The distinction between user accounts and people recorded in property management is described in the [user account concept](./user-account.md).

## Related concepts

- [Estate and rental](./estate-and-rental.md) — buildings, housing units, people, and leases assigned to an estate.
- [Roles and responsibilities](../roles.md) — observed application access roles and their separation from business roles.
