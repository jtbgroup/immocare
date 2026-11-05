---
title: User Account
type: business concepts
status: draft
last_verified: 2026-11-04
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Concept — User Account

A **user account** is an application identity used to sign in and access ImmoCare. It is not the same concept as a [person](./estate-and-rental.md): person records describe people in the property-management domain, while a user account represents access to the application.

The current model has no dedicated identity link between a user account and a person record, though domain records may separately retain user audit references. Whether an explicit account-to-person link is needed remains an [open question](../../../backlog/open_questions.md#user-accounts).

## Access scope

The application distinguishes platform-wide administration from estate-specific access:

- A **platform administrator** has global administrative access to user accounts and platform-level operations.
- **Manager** and **viewer** are access roles assigned to a user account for an estate; they do not describe the user's business identity or relationship to a property.

The application currently stores platform-administrator status on the user account. Estate-specific membership is separate. These observed access rules are not yet confirmed business policy; see the [user account rules](../rules/users.md).

## Current account information

The account data currently represented includes a username, email address, password hash, platform-administrator status, and creation and update timestamps. The application does not return the password hash in user API responses. The implemented fields and storage constraints are documented in the technical [data dictionary](../../technical/data-dictionary.md) and [data model](../../technical/data-model.md).

## Related concepts

- [Person](./estate-and-rental.md) — an individual recorded in the property-management domain.
- [Roles and responsibilities](../roles.md) — separates application access roles from business relationships.
- [Authentication](../../technical/authentication.md) — describes sign-in and session behavior.
