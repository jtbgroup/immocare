# US098 — Add Member to Estate

## Overview

| Attribute | Value |
|---|---|
| **ID** | US098 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | MANAGER, PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** MANAGER **I want to** add a user to my estate **so that** they can access the estate data.

## Acceptance Criteria

- AC1: Required: `userId`, `role` (MANAGER or VIEWER).
- AC2: Referenced user must already exist in `app_user` → HTTP 409 if not found.
- AC3: User already a member → HTTP 409 "This user is already a member of this estate."
- AC4: Only MANAGER or PLATFORM_ADMIN can add members.
- AC5: On success: new member immediately appears in the member list with correct role badge.

## Endpoint

`POST /api/v1/estates/{id}/members`

## Request Body

```json
{
  "userId": 42,
  "role": "VIEWER"
}
```

## Response

HTTP 201 — `EstateMemberDTO`

## Business Rules

- BR-UC016-02: Adding a MANAGER satisfies the minimum manager requirement
