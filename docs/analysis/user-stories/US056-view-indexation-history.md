# User Story US056: View Indexation History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US056 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the indexation history of a lease **so that** I can track all rent changes.

## Acceptance Criteria

**AC1:** Indexation section on lease details shows collapsible table with all records.
**AC2:** Columns: Application Date | Old Rent | Index Value | Index Month | Applied Rent | Notification Date | Notes. Sorted by application date DESC.
**AC3:** No indexations → "No indexation recorded yet".
**AC4:** Summary line: "Total indexation: +€X.XX (+Y.YZ%) since lease start".

**Endpoint:** `GET /api/v1/leases/{id}/indexations`

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
