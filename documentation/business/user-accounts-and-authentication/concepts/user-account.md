---
title: User Account
type: business concepts
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Concept — User Account

A **user account** is an application identity used to sign in and access ImmoCare. It is not the same concept as a [person](../../people/concepts/person.md): person records describe people in the property-management domain, while a user account represents access to the application.

The current model has no dedicated identity link between a user account and a person record, though domain records may separately retain user audit references. The concepts remain separate for now; assessing whether an explicit identity link would be useful is a [backlog opportunity](../../../../backlog/backlog.md).

## Access scope

The application distinguishes platform-wide administration from estate-specific access:

- A **platform administrator** has global administrative access to user accounts and platform-level operations.
- **Manager** and **viewer** are access roles assigned to a user account for an estate; they do not describe the user's business identity or relationship to a property.

The application currently stores platform-administrator status on the user account. Estate-specific memberships are separate, and a platform administrator may also hold a role in an estate. The current estate-list API returns `myRole = null` to platform administrators even when they have an explicit membership; this behavior and the unresolved display choice are documented in [Estate Management](../../../technical/estates-and-access/estate-management.md).

## Current account information

The account data currently represented includes a username, email address, password hash, platform-administrator status, and creation and update timestamps. The application does not return the password hash in user API responses. The implemented fields and storage constraints are documented in the [user account data dictionary](../../../technical/user-accounts-and-authentication/user-account-data-dictionary.md) and [data model](../../../technical/cross-cutting/data-model.md).

## Related concepts

- [Person](../../people/concepts/person.md) — an individual recorded in the property-management domain.
- [Roles and responsibilities](../../cross-cutting/roles.md) — separates application access roles from business relationships.
- [Authentication](../../../technical/user-accounts-and-authentication/authentication.md) — describes sign-in and session behavior.
