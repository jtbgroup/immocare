# User Story US059: View Lease Alerts

| Attribute | Value |
|-----------|-------|
| **Story ID** | US059 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** see all upcoming lease deadlines in one place **so that** I don't miss indexation or end-of-lease obligations.

## Acceptance Criteria

**AC1:** Indexation alert on lease/unit details when anniversary within `indexationNoticeDays`.
**AC2:** End notice alert: "⚠ Lease ending soon — send notice before [date]" when within 30 days.
**AC3:** Global alerts page: all pending alerts with Unit, Lease ID, Alert type, Deadline date, sorted by deadline ASC.
**AC4:** Alert disappears after indexation recorded for that anniversary year.
**AC5:** No active leases → "No pending alerts".

**Endpoint:** `GET /api/v1/leases/alerts`

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
