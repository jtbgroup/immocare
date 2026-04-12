# User Story US076: View Revision History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US076 |
| **Epic** | Fire Safety Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the revision history of a fire extinguisher **so that** I can check past inspections.

## Acceptance Criteria

**AC1:** Each extinguisher card has a "View revisions (N)" toggle button where N is the total revision count.
**AC2:** Click toggle → history table expands inline with columns: Date | Notes | (delete action).
**AC3:** Revisions ordered by revision date DESC (most recent first).
**AC4:** No revisions → "No revision recorded yet." with the "Add revision" form shown directly.
**AC5:** Toggle again → panel collapses.
**AC6:** Dates formatted consistently with the rest of the application (AppDatePipe).

**Endpoint:** Revisions are embedded in `GET /api/v1/buildings/{buildingId}/fire-extinguishers` — no separate endpoint needed for list view.

**Last Updated:** 2026-03-04 | **Status:** Ready for Development
