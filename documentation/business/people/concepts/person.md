---
title: Person
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Concept — Person

A **person** (`Person`) is an individual recorded within an estate. A person may be associated as the owner of a [building or housing unit](../../buildings-and-housing-units/concepts/buildings-and-housing-units.md), or participate in a [lease](../../leases/concepts/lease.md).

A person record stores identifying and contact information. The same natural person may have separate estate-scoped records; the current model does not provide a shared identity across estates. Observed person and person-bank-account constraints are listed in the [person rules](../rules/persons.md).

A person record is distinct from a [user account](../../user-accounts-and-authentication/concepts/user-account.md). The current model has no dedicated identity link between them, although domain records may separately retain user audit references. Whether such a link is useful is tracked in the [backlog](../../../../backlog/backlog.md).

A person may have one or more registered bank accounts. Their IBANs can be used to suggest a related lease during transaction import; account management and current suggestion behavior are documented in [Person Management](../../../technical/people/person-management.md).
