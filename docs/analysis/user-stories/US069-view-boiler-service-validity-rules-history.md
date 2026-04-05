# User Story US069: View Boiler Service Validity Rules History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US069 |
| **Epic** | Platform Administration |
| **Related UC** | UC012 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view all historical validity rules **so that** I can understand how the regulation has evolved.

## Acceptance Criteria

**AC1:** Table shows all rules ordered by valid_from DESC.

**AC2:** Columns: valid from, duration (months), description, created by, created at.

**AC3:** Current rule (valid_from ≤ today, most recent) has a "Current" badge.

**AC4:** Future rules (valid_from > today) have a "Scheduled" badge.

**AC5:** Rules have no edit or delete buttons — read-only after creation.

**AC6:** Seed rule (1900-01-01, 24 months) visible as the oldest entry.

**Endpoint:** `GET /api/v1/config/boiler-validity-rules` — HTTP 200.

**Last Updated:** 2026-04-04 | **Status:** Ready for Development
