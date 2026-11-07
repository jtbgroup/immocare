---
title: Buildings and Housing Units
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Concept — Buildings and Housing Units

A **building** (`Building`) is a property assigned to an [estate](../../estates-and-access/concepts/estate-and-access.md). It may contain housing units and may be associated with an owner.

A **housing unit** (`HousingUnit`) belongs to a building and may be associated with a different owner. A **room** (`Room`) describes a room or space within a housing unit.

The model permits a housing unit to have an owner different from the building's owner. Housing units without a direct owner can expose an effective owner name inherited from their building, but the model does not define a broader ownership precedence rule.

Observed constraints on buildings are recorded in the [building rules](../rules/buildings.md). Current building operations and estate-scoped access are documented in [Building Management](../../../technical/buildings-and-housing-units/building-management.md). The persisted relationships are shown in the cross-cutting [data model](../../../technical/cross-cutting/data-model.md).

Energy performance ratings (PEB) are recorded for housing units. Any alerting based on this information is part of the cross-cutting [Alerts](../../cross-cutting/concepts/alerts.md) concept, not an equipment-only concern.

Tracked [equipment](../../equipment/concepts/property-equipment.md)—including boilers, fire extinguishers, and meters—may be associated with a building or a housing unit. Equipment is a separate domain concept from the property structure.
