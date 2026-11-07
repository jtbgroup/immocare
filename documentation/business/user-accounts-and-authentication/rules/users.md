---
title: User Account Rules
type: business rules
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Business Rules — User Accounts

This table combines constraints observed in the implementation with explicitly confirmed account policies. Code-derived constraints remain subject to business validation; confirmed rules are identified in the status column and the confirmation note below.

| ID | Rule | Status |
|---|---|---|
| BR-USER-001 | Account creation or update is rejected when its username or email address matches an existing account without regard to case. | Needs business validation |
| BR-USER-002 | Only a platform administrator can view or manage user accounts. | Needs business validation |
| BR-USER-003 | A password supplied when creating or changing an account must contain at least eight characters, including an uppercase letter, a lowercase letter, and a digit. | Needs business validation |
| BR-USER-004 | An account cannot be deleted by the user currently signed in to that account. | Needs business validation |
| BR-USER-005 | The last platform-administrator account cannot be deleted. | Confirmed |
| BR-USER-006 | The password and its confirmation must match when creating an account or changing its password. | Needs business validation |
| BR-USER-007 | A username must be 3–50 characters long and consist only of letters, digits, or underscores. | Needs business validation |
| BR-USER-008 | An account email address must pass email format validation and contain no more than 100 characters. | Needs business validation |
| BR-USER-009 | A platform administrator cannot be demoted if doing so would leave no platform administrator. | Confirmed |
| BR-USER-010 | Sign-in matches usernames without regard to case. | Confirmed |

BR-USER-005, BR-USER-009, and BR-USER-010 were confirmed by the product stakeholder on 2026-11-05. The current implementation prevents deletion of the last platform administrator but does not prevent demoting the last one; sign-in currently looks up the username with the entered case. These implementation gaps are tracked in the [backlog](../../../../backlog/backlog.md).

## Observed sources

`UserController`, `UserService`, `UserRepository`, `UserDetailsServiceImpl`, `AppUser`, `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest`, and the user-management frontend.
