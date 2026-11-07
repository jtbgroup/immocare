---
title: Lease Technical References
type: technical reference index
status: current
last_verified: 2026-11-07
verified_against: "Lease, LeaseRentAdjustment, LeaseService, LeaseController, V014__uc014_manage_leases.sql"
---

# Lease Technical References

No complete technical reference for lease behavior is currently published. The domain concept is documented in [Lease](../../business/leases/concepts/lease.md). The current lease API stores rent and charge adjustments on a lease, including old and new values, an effective date, and a required reason. This is separate from the dated rent history stored directly on a housing unit; see [Housing-Unit Rent History Management](../buildings-and-housing-units/rent-history-management.md). Lease-end and indexation alerts are described in [Estate Management](../estates-and-access/estate-management.md) and the cross-cutting [Alerts](../../business/cross-cutting/concepts/alerts.md).
