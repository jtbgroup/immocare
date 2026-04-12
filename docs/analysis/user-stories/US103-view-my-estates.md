# US103 — View My Estates

## Overview

| Attribute | Value |
|---|---|
| **ID** | US103 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | All authenticated users |
| **Priority** | MUST HAVE |

## User Story

**As any** authenticated user **I want to** see the list of estates I belong to **so that** I can navigate between them.

## Acceptance Criteria

- AC1: Returns estates where the user is MANAGER or VIEWER, with their role and key stats (building count, unit count).
- AC2: PLATFORM_ADMIN sees all estates on the platform (equivalent to US095 but from their own context).
- AC3: Used to populate the estate selector (US101) and the header estate switcher.
- AC4: If the list is empty (user has no estates and is not PLATFORM_ADMIN) → show "No estate assigned" message with contact instructions.

## Endpoint

`GET /api/v1/estates/mine`

## Response

HTTP 200 — `List<EstateSummaryDTO>`

```json
[
  {
    "id": "a3f8c2d1-7b4e-4f2a-9c1d-8e5f3a2b1c0d",
    "name": "My Estate",
    "description": "Main property portfolio",
    "myRole": "MANAGER",
    "buildingCount": 3,
    "unitCount": 12
  }
]
```

## Business Rules

- BR-UC016-05: PLATFORM_ADMIN returns all estates with `myRole = null`
- BR-UC016-13: Role returned is the user's role in each estate independently
