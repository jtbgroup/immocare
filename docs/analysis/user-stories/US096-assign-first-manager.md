# US096 — Assign First Manager to Estate

## Overview

| Attribute | Value |
|---|---|
| **ID** | US096 |
| **UC** | [UC016 — Manage Estates](../use-cases/UC016_manage_estates.md) |
| **Actor** | PLATFORM_ADMIN |
| **Priority** | MUST HAVE |

## User Story

**As a** PLATFORM_ADMIN **I want to** assign the first manager to a newly created estate **so that** the estate can be handed off to its owner.

## Acceptance Criteria

- AC1: Can be done at creation time via optional `firstManagerId` in `CreateEstateRequest` (see US092).
- AC2: Can also be done separately via the membership endpoint (see US098).
- AC3: Assigned user receives role MANAGER in the estate.
- AC4: The PLATFORM_ADMIN is not automatically added as a member — they access all estates via their global flag.
- AC5: If `firstManagerId` does not reference an existing `app_user` → HTTP 409.

## Endpoint

`POST /api/v1/estates/{id}/members` (reuses US098)

## Business Rules

- BR-UC016-02: Estate must always have at least one MANAGER
- BR-UC016-05: PLATFORM_ADMIN does not need a member entry to access estates
