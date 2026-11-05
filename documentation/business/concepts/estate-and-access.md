---
title: Estate and Access
type: business concepts
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Concept — Estate and Access

## Estate

An **estate** is a property-management entity to which properties and their related management data are assigned. It may represent a real-estate agency or an individual who owns several properties.

## Estate membership

An **estate membership** associates a user account with one estate and an estate-scoped access role. A user account can have memberships in multiple estates, and the role is independent for each membership.

The application currently defines two estate roles:

- **Manager** — estate-scoped access that permits management operations.
- **Viewer** — estate-scoped read access.

These are application access roles only; they do not define separate business responsibilities. Their observed access boundaries are summarized in [Roles and Responsibilities](../roles.md); implementation behavior is documented in [Estate Management](../../technical/estate-management.md).

## Platform administrator

A **platform administrator** has global account-level access. A platform administrator may also have an estate membership with an estate-specific role. The application allows platform administrators to access estates without membership; currently, the estate-list response represents their global access with no estate-specific role (`myRole = null`), even if an explicit membership exists. Whether to expose that explicit role in the response remains an [open question](../../../backlog/open_questions.md#scope-and-roles).

The distinction between user accounts and people recorded in property management is described in the [user account concept](./user-account.md).

## Related concepts

- [Estate and rental](./estate-and-rental.md) — buildings, housing units, people, and leases assigned to an estate.
- [Roles and responsibilities](../roles.md) — observed application access roles and their separation from business roles.
