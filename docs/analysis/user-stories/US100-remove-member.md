# US100 — Remove Member from Estate

## Overview

| Attribute | Value |
|---|---|
| **ID** | US100 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | MANAGER, PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** MANAGER **I want to** remove a member from my estate **so that** I can revoke their access.

## Acceptance Criteria

- AC1: Cannot remove self → HTTP 409 "You cannot remove yourself from an estate."
- AC2: Cannot remove the last MANAGER → HTTP 409 "Cannot remove the last manager of an estate."
- AC3: On success: user loses all access to the estate immediately, HTTP 204.
- AC4: Only MANAGER or PLATFORM_ADMIN can remove members.
- AC5: Confirmation dialog before removal showing the member's username.

## Endpoint

`DELETE /api/v1/estates/{id}/members/{userId}`

## Response

HTTP 204 — No content

## Business Rules

- BR-UC016-02: At least one MANAGER must remain after removal
- BR-UC016-03: User cannot remove themselves
