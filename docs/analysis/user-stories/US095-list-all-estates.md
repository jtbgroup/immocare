# US095 — List All Estates

## Overview

| Attribute | Value |
|---|---|
| **ID** | US095 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** PLATFORM_ADMIN **I want to** view all estates on the platform **so that** I can oversee the entire platform.

## Acceptance Criteria

- AC1: Returns paginated list with: name, description, member count, building count, created at, created by username.
- AC2: Filterable by `search` (partial, case-insensitive match on name).
- AC3: Default sort: name ASC.
- AC4: Clicking a row navigates to the estate detail / member management page.

## Endpoint

`GET /api/v1/admin/estates?search=&page=&size=&sort=`

## Response

HTTP 200 — `Page<EstateDTO>`

## Business Rules

- BR-UC016-05: Only PLATFORM_ADMIN can access this endpoint
