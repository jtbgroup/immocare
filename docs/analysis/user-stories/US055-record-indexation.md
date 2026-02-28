# User Story US055: Record Indexation

| Attribute | Value |
|-----------|-------|
| **Story ID** | US055 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** record a rent indexation **so that** the new rent amount is tracked.

## Acceptance Criteria

**AC1:** "Record Indexation" button visible on ACTIVE lease Indexation section.
**AC2:** Form: Old Rent (read-only), Application Date*, New Index Value*, New Index Month (month/year)*, Applied Rent*, Notification Sent Date (optional), Notes (optional).
**AC3:** Save → indexation record created, lease monthly_rent updated to Applied Rent, "Indexation recorded successfully".
**AC4:** DRAFT lease → no "Record Indexation" button.
**AC5:** Required fields missing → respective validation errors.

**Endpoint:** `POST /api/v1/leases/{id}/indexations` — HTTP 201.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
