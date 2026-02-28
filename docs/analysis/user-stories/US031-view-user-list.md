# User Story US031: View User List

| Attribute | Value |
|-----------|-------|
| **Story ID** | US031 |
| **Epic** | User Management |
| **Related UC** | UC007 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view all user accounts **so that** I can oversee who has system access.

## Acceptance Criteria

**AC1:** 3 users exist → list shows all 3 with: username, email, role, created date. Title "Users", "Create User" button visible.
**AC2:** Only own account exists → own account shown + "Create User" button.
**AC3:** Click "Username" header → sort A→Z; click again → Z→A.
**AC4:** Click a user row → navigate to user details page.
**AC5:** >20 users → 20 per page + pagination controls.

**Endpoint:** `GET /api/v1/users` — default sort: username ASC. Password hash never in response.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
