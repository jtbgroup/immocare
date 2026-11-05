---
title: User Account Rules
type: business rules
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Candidate Business Rules — User Accounts

The following constraints are visible in the current implementation. They are inferred from code and **need business validation**; they are not confirmed organizational policy.

| ID | Observed rule | Status |
|---|---|---|
| BR-USER-001 | Account creation or update is rejected when its username or email address matches an existing account without regard to case. | Needs business validation |
| BR-USER-002 | Only a platform administrator can view or manage user accounts. | Needs business validation |
| BR-USER-003 | A password supplied when creating or changing an account must contain at least eight characters, including an uppercase letter, a lowercase letter, and a digit. | Needs business validation |
| BR-USER-004 | An account cannot be deleted by the user currently signed in to that account. | Needs business validation |
| BR-USER-005 | The last platform-administrator account cannot be deleted. | Needs business validation |
| BR-USER-006 | The password and its confirmation must match when creating an account or changing its password. | Needs business validation |
| BR-USER-007 | A username must be 3–50 characters long and consist only of letters, digits, or underscores. | Needs business validation |
| BR-USER-008 | An account email address must pass email format validation and contain no more than 100 characters. | Needs business validation |

## Observed sources

`UserController`, `UserService`, `UserRepository`, `AppUser`, `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest`, and the user-management frontend.
