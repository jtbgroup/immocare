# US093 — Edit Estate

## Overview

| Attribute | Value |
|---|---|
| **ID** | US093 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** PLATFORM_ADMIN **I want to** edit an estate's information **so that** I can keep it accurate.

## Acceptance Criteria

- AC1: Editable fields: `name`, `description`.
- AC2: Name uniqueness enforced excluding the current estate (case-insensitive).
- AC3: Returns HTTP 404 if estate not found.
- AC4: Returns HTTP 409 if new name conflicts with an existing estate.

## Endpoint

`PUT /api/v1/admin/estates/{id}`

## Request Body

```json
{
  "name": "Updated Estate Name",
  "description": "Updated description"
}
```

## Response

HTTP 200 — `EstateDTO`

## Business Rules

- BR-UC016-01: Name unique case-insensitively (excluding self)
