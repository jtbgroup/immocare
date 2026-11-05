---
title: Estate
type: business rules
status: draft
last_verified: 2026-10-20
---

# Candidate Business Rules — Estate

These rules describe constraints visible in the current implementation. They **need business validation**: a technical constraint does not prove that it is an intended business policy.

| ID          | Observed rule                                                                                                                                     | Status                        |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| BR-PORT-001 | An estate name must be unique, ignoring case.                                                                                                     | Validated                     |
| BR-PORT-002 | When estate creation provides a non-empty member list of users, that list must include at least one `MANAGER`. The service accepts an empty list. | Validated - needs code review |
| BR-PORT-003 | The last `MANAGER` of an estate cannot be demoted or removed.                                                                                     | Validated                     |
| BR-PORT-004 | The observed service does not allow a estate containing at least one building to be deleted.                                                      | Validated                     |
| BR-PORT-005 | A user cannot be added to the same estate twice. Duplicate user IDs in the initial creation list are ignored.                                     | Validated                     |

## Observed sources

`EstateService`, `Estate`, `EstateMember`, and `EstateRole`. The manager-presence rule applies only when a non-empty member list is supplied; do not generalize it without validation.
