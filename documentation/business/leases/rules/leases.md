---
title: Lease Rules
type: business rules
status: draft
last_verified: 2026-11-07
verified_against: "Lease, LeaseTenant, LeaseRentAdjustment, LeaseService, LeaseController, V014__uc014_manage_leases.sql, stakeholder confirmation 2026-11-07"
---

# Business Rules — Leases

These rules record confirmed policy and constraints inferred from current lease behavior. Code-inferred rules remain subject to the status shown below. The implemented behavior is documented in [Lease Management](../../../technical/leases/lease-management.md).

| ID | Rule | Status |
|---|---|---|
| BR-LEASE-001 | A regulated reference rent is not directly associated with a lease in the domain model; external regulation may govern lease terms without creating that relationship. | Confirmed |
| BR-LEASE-002 | The application rejects creating a lease for a housing unit that already has a lease in `DRAFT` or `ACTIVE` status. | Needs business validation |
| BR-LEASE-003 | A lease must have at least one `PRIMARY` participant when it is created. | Needs business validation |
| BR-LEASE-004 | Lease status transitions are limited to `DRAFT` → `ACTIVE` or `CANCELLED`, and `ACTIVE` → `FINISHED` or `CANCELLED`. | Needs clarification |
| BR-LEASE-005 | Only leases in `DRAFT` or `ACTIVE` status may be edited, have participants added or removed, or receive rent or charge adjustments. | Needs business validation |
| BR-LEASE-006 | A lease must retain at least one `PRIMARY` participant; removing its last `PRIMARY` participant is rejected. | Needs business validation |
| BR-LEASE-007 | A person may be attached to a lease only once, and must belong to the same estate as the lease's housing unit. | Needs business validation |
| BR-LEASE-008 | A lease's end date must not precede its start date; its duration, notice period, and monthly rent must be positive, and monthly charges must not be negative. | Needs business validation |
| BR-LEASE-009 | An adjustment must target rent or charges, have a positive new amount, and include a reason and effective date. | Needs business validation |
| BR-LEASE-010 | A lease may be deleted only while it is in `DRAFT` or `CANCELLED` status. | Needs clarification |

BR-LEASE-001 was confirmed by the user on 2026-11-07. BR-LEASE-004 and BR-LEASE-010 need clarification about the intended lifecycle and deletion policy. Whether the create-time restriction in BR-LEASE-002 must also be enforced when activating a draft remains open. See [Open Questions](../../../../backlog/open_questions.md#housing-units-and-leases) and the [backlog](../../../../backlog/backlog.md).

## Observed sources

`Lease`, `LeaseTenant`, `LeaseRentAdjustment`, `LeaseService`, `LeaseController`, `LeaseRentAdjustmentRepository`, and `V014__uc014_manage_leases.sql`.
