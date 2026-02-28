# User Story US019: Check PEB Certificate Validity

| Attribute | Value |
|-----------|-------|
| **Story ID** | US019 |
| **Epic** | PEB Score Management |
| **Related UC** | UC004 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** see if a PEB certificate is still valid **so that** I know when to renew it.

## Acceptance Criteria

**AC1:** valid_until < today → red badge "Expired" shown on current score.
**AC2:** valid_until within next 3 months → orange badge "Expires soon" shown.
**AC3:** valid_until > today + 3 months → no validity warning shown (certificate valid).
**AC4:** No valid_until set → "Validity period not specified".

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
