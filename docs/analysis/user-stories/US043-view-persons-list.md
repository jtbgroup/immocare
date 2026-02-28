# User Story US043: View Persons List

| Attribute | Value |
|-----------|-------|
| **Story ID** | US043 |
| **Epic** | Person Management |
| **Related UC** | UC009 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view all persons **so that** I can find and manage owners and tenants.

## Acceptance Criteria

**AC1:** Persons page shows paginated list with: Last Name, First Name, Email, GSM, City, Role badges. Default sort: last name ASC.
**AC2:** Person is owner of a building → blue "Owner" badge. Person is active lease tenant → green "Tenant" badge.
**AC3:** No persons → "No persons yet" + "Add Person" button.
**AC4:** >20 persons → pagination controls, 20 per page.
**AC5:** Type "dupont" in search → only persons with "dupont" in name, email, or national ID shown (case-insensitive).

**Endpoint:** `GET /api/v1/persons?search=&page=&size=`

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
