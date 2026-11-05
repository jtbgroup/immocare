---
title: Estate
type: business rules
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Candidate Business Rules — Estate

These rules describe constraints visible in the current implementation. Code-derived rules need business validation; an implementation constraint does not prove that it is intended business policy.

| ID | Observed rule | Status |
|---|---|---|
| BR-ESTATE-001 | Estate names are checked for uniqueness without regard to case. | Needs business validation |
| BR-ESTATE-002 | Estate creation accepts no members; if the submitted member list is non-empty, it must include a `MANAGER`. | Needs clarification |
| BR-ESTATE-003 | Estate membership operations reject demoting or removing the last `MANAGER`. | Needs clarification |
| BR-ESTATE-004 | Estate deletion is rejected when the estate contains one or more buildings. | Needs business validation |
| BR-ESTATE-005 | Duplicate user entries in an initial estate-creation request are silently ignored. | Needs business validation |
| BR-ESTATE-006 | A user cannot remove their own membership from an estate through membership management. | Needs business validation |
| BR-ESTATE-007 | A user cannot change their own role in an estate through membership management. | Needs business validation |

The code prevents the last manager from being demoted or removed through membership operations, but deleting that user's account removes their estate memberships. Whether the estate must retain a manager across account deletion remains unresolved.

## Observed sources

`EstateService`, `Estate`, `EstateMember`, `EstateRole`, `UserService`, `V003__uc003_manage_estates.sql`, and estate membership controllers.
