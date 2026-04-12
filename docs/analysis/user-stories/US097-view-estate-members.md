# US097 — View Estate Members

## Overview

| Attribute | Value |
|---|---|
| **ID** | US097 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | MANAGER, PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** MANAGER **I want to** view the list of members of my estate **so that** I can see who has access.

## Acceptance Criteria

- AC1: Returns all members: username, email, role (MANAGER / VIEWER), added at.
- AC2: Accessible to MANAGER of the estate and PLATFORM_ADMIN.
- AC3: VIEWER cannot access the members list → HTTP 403.
- AC4: Members sorted by username ASC.

## Endpoint

`GET /api/v1/estates/{id}/members`

## Response

HTTP 200 — `List<EstateMemberDTO>`

```json
[
  {
    "userId": 1,
    "username": "john",
    "email": "john@example.com",
    "role": "MANAGER",
    "addedAt": "2026-04-12T10:00:00"
  }
]
```

## Business Rules

- BR-UC016-08: VIEWER cannot access member management
