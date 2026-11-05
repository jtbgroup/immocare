---
title: User Account Management
type: technical reference
status: current
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# User Account Management

This document describes the user-account administration behavior currently implemented by the application. It consolidates account listing, creation, updates, password changes, and deletion without a use-case or user-story breakdown. The business meaning of a user account is described in the [user account concept](../business/concepts/user-account.md); data fields and storage are documented in the [user data dictionary](./data-dictionary.md) and [user data model](./data-model.md).

## Access boundary

All `/api/v1/users` operations require an authenticated `PLATFORM_ADMIN`, enforced by the backend controller. The frontend `/users` routes currently use only the authentication guard, not the platform-admin guard, so a non-admin can open the screens but cannot retrieve or change account data through the API. Other authenticated users cannot administer accounts.

Platform administration is represented by the global `isPlatformAdmin` flag. `MANAGER` and `VIEWER` are separate estate-membership roles; they are not account roles. See [Roles and Responsibilities](../business/roles.md) and [Authentication](./authentication.md).

## Account information

A user account contains an identifier, username, email address, password hash, platform-administrator flag, creation time, and update time. Passwords are encoded with BCrypt strength 12. User API responses return the identifier, username, email, `isPlatformAdmin`, `createdAt`, and `updatedAt`; they do not expose the password or its hash. Account validation policies are defined in the [user account rules](../business/rules/users.md), not repeated here.

The current model is not the legacy single-`ADMIN` role model. A platform administrator can access platform-wide administration; estate-specific roles are assigned separately. There is no dedicated identity link between an account and a `Person` record, although domain records may store user audit references.

## Available operations

| Operation | Endpoint | Result |
|---|---|---|
| List users | `GET /api/v1/users` | `200 OK` with all user DTOs. |
| Get a user | `GET /api/v1/users/{id}` | `200 OK` with the user DTO, or `404 Not Found`. |
| Create a user | `POST /api/v1/users` | `201 Created` with the created user DTO. |
| Update account details | `PUT /api/v1/users/{id}` | `200 OK` with the updated user DTO. |
| Change a user's password | `PATCH /api/v1/users/{id}/password` | `204 No Content`. |
| Delete a user | `DELETE /api/v1/users/{id}` | `204 No Content`. |

The frontend displays username, email, platform-admin status, and creation time in the user list. It sorts these fields client-side, defaults to username ascending, and paginates the retrieved list client-side with 20 users per page. Selecting a row opens its details, where account details can be edited, its password changed, or the account deleted.

## Account validation and lifecycle

### Create and update

- Account input and password policies are enforced by the backend according to BR-USER-001 and BR-USER-003–BR-USER-008; see the [canonical rule definitions](../business/rules/users.md).
- The create and update requests carry `isPlatformAdmin`. Updating account details does not change the password.
- Creation and update timestamps are assigned and maintained by the persistence entity lifecycle.

### Change password

- The password-change request contains a new password and its confirmation; no current password is requested.
- Password acceptance and confirmation requirements are defined by BR-USER-003 and BR-USER-006.
- The new password is BCrypt-encoded before it is stored.
- The application code does not explicitly invalidate the target user's active session after a password change. This differs from UC002.004 AC3 and is listed in the [backlog](../../backlog/backlog.md).

### Delete

- Deleting the currently authenticated account is prohibited.
- Deleting the last platform administrator is prohibited.
- The backend enforces both restrictions; the last-platform-administrator restriction is returned as a conflict.
- The application code does not explicitly invalidate the deleted user's active session. The details page also does not currently enforce both deletion guards in its controls: its current-user name is not populated, and its last-administrator check is passed as `false`; the backend still enforces the self-delete and last-administrator restrictions. These gaps differ from UC002.005 AC4, AC5, and AC7 and are listed in the [backlog](../../backlog/backlog.md).

## Error behavior

| Condition | Current API behavior |
|---|---|
| Unauthenticated request | `401 Unauthorized`. |
| Authenticated non-platform-administrator request | Denied by platform-admin method security. |
| User identifier not found | `404 Not Found`. |
| Username or email already in use | `409 Conflict`, with `USERNAME_TAKEN` or `EMAIL_TAKEN`. |
| Attempt to delete own account | `403 Forbidden`, with `CANNOT_DELETE_SELF`. |
| Attempt to delete the last platform administrator | `409 Conflict`, with `LAST_PLATFORM_ADMIN`. |
| Missing or invalid request fields covered by Jakarta validation | `400 Bad Request`. |

Two password-validation paths do not currently return the expected client-error responses: a password-composition failure thrown as `IllegalArgumentException` is handled as `409 Conflict` with the generic `DUPLICATE` code, and a password-confirmation mismatch has no specific exception mapping and falls through to the generic `500 Internal Server Error` handler. These are observed implementation behaviors, not intended password-policy semantics.

## Differences from the legacy specification

The historical UC-002 use case and UC002.001–UC002.005 user stories describe a single `ADMIN` role and role selection on accounts. The implementation now uses a global platform-admin flag and separate estate roles. Its endpoint and backend validation behavior are described above; unimplemented presentation details or requirements from the legacy specification are not assumed to be current behavior. Decisions about the role-model change and username case sensitivity are recorded in [open questions](../../backlog/open_questions.md#user-accounts).

## Implementation references

- Backend endpoints and platform-admin restriction: [UserController](../../backend/src/main/java/com/immocare/controller/UserController.java)
- Account rules and operations: [UserService](../../backend/src/main/java/com/immocare/service/UserService.java)
- Persisted account fields and authority mapping: [AppUser](../../backend/src/main/java/com/immocare/model/entity/AppUser.java)
- User response mapping: [UserMapper](../../backend/src/main/java/com/immocare/mapper/UserMapper.java)
- Request validation: [CreateUserRequest](../../backend/src/main/java/com/immocare/model/dto/CreateUserRequest.java), [UpdateUserRequest](../../backend/src/main/java/com/immocare/model/dto/UpdateUserRequest.java), and [ChangePasswordRequest](../../backend/src/main/java/com/immocare/model/dto/ChangePasswordRequest.java)
- Error response mapping: [GlobalExceptionHandler](../../backend/src/main/java/com/immocare/exception/GlobalExceptionHandler.java)
- Frontend account operations: [UserService](../../frontend/src/app/core/services/user.service.ts), [user.model.ts](../../frontend/src/app/models/user.model.ts), and the [user-management components](../../frontend/src/app/features/user/)
