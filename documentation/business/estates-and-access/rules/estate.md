---
title: Estate
type: business rules
status: draft
last_verified: 2026-11-05
verified_against: f295fb241898d79e75455198680219471509fcde
---

# Business Rules — Estate

This table records constraints observed in the implementation and stakeholder-confirmed estate policies. Code-derived rules need business validation; an implementation constraint does not prove that it is intended business policy.

| ID | Rule | Status |
|---|---|---|
| BR-ESTATE-001 | Estate names are checked for uniqueness without regard to case. | Needs business validation |
| BR-ESTATE-002 | Estate creation may accept no members and requires a `MANAGER` only when members are supplied. | Deprecated |
| BR-ESTATE-003 | Estate membership operations reject demoting or removing the last `MANAGER`. | Confirmed |
| BR-ESTATE-004 | Estate deletion is rejected when the estate contains one or more buildings. | Deprecated |
| BR-ESTATE-005 | Duplicate user entries in an initial estate-creation request are silently ignored. | Deprecated |
| BR-ESTATE-006 | A user cannot remove their own membership from an estate through membership management. | Needs business validation |
| BR-ESTATE-007 | A user cannot change their own role in an estate through membership management. | Needs business validation |
| BR-ESTATE-008 | Every estate must have at least one `MANAGER`, including at creation and after user-account deletion. | Confirmed |
| BR-ESTATE-009 | Duplicate user IDs in an initial estate-creation request are rejected. | Confirmed |
| BR-ESTATE-010 | Estate deletion is blocked while any estate-scoped data remains. | Confirmed |

BR-ESTATE-002, BR-ESTATE-004, and BR-ESTATE-005 record former observed behaviors and are deprecated as business rules. The current implementation permits creating an estate without a manager, checks for buildings but not all estate-scoped data before deletion, and silently skips duplicate member IDs. BR-ESTATE-003 and BR-ESTATE-008 through BR-ESTATE-010 reflect decisions confirmed by the product stakeholder on 2026-11-05; the implementation gaps are tracked in the [backlog](../../../../backlog/backlog.md). The scope of data that must move with a transferred building remains an [open question](../../../../backlog/open_questions.md#scope-and-roles).

## Observed sources

`EstateService`, `Estate`, `EstateMember`, `EstateRole`, `UserService`, `V003__uc003_manage_estates.sql`, and estate membership controllers.
