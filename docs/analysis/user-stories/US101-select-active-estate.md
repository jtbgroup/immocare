# US101 — Select Active Estate

## Overview

| Attribute | Value |
|---|---|
| **ID** | US101 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | All authenticated users |
| **Priority** | MUST HAVE |

## User Story

**As a** user with access to multiple estates **I want to** choose which estate to work in **so that** I can manage each one independently.

## Acceptance Criteria

- AC1: On login, if user has access to exactly one estate → automatically enter that estate's context and navigate to its dashboard.
- AC2: If user has access to multiple estates → show estate selector screen before entering the app.
- AC3: PLATFORM_ADMIN with no estate membership → goes directly to the admin estate list (`/admin/estates`).
- AC4: Estate switcher available in the app header at all times to change context without logout.
- AC5: Switching estate navigates to the new estate's dashboard.
- AC6: Active estate stored client-side via Angular signal (`ActiveEstateService`).

## Implementation Notes

No dedicated API endpoint — selection is a client-side navigation to `/estates/{id}/dashboard`. The `estateId` is stored in `ActiveEstateService` (Angular signal) and used to construct all subsequent API URLs.

## Business Rules

- BR-UC016-06: `estateId` is always explicit in the URL path — never stored server-side in session
