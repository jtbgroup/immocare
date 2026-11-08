---
title: Lease Technical References
type: technical reference index
status: current
last_verified: 2026-11-07
verified_against: "Lease, LeaseTenant, LeaseRentAdjustment, LeaseService, LeaseController, V014__uc014_manage_leases.sql"
---

# Lease Technical References

The domain concept is documented in [Lease](../../business/leases/concepts/lease.md). The current API, access, lease lifecycle, participant management, adjustments, and alerts are documented in [Lease Management](./lease-management.md).

Lease rent and charge adjustments are stored on the lease with previous and new amounts, an effective date, and a required reason. They are separate from the dated theoretical rent history stored on a housing unit; see [Housing-Unit Rent History Management](../buildings-and-housing-units/rent-history-management.md). Cross-domain alert concepts are described in [Alerts](../../business/cross-cutting/concepts/alerts.md), and estate dashboard counts are described in [Estate Management](../estates-and-access/estate-management.md).
