---
title: Buildings and Housing Units
type: business concepts
status: draft
last_verified: 2026-11-07
verified_against: "HousingUnit, Room, PebScoreHistory, HousingUnitService, RoomService, PebScoreService, Building, Person"
---

# Concept — Buildings and Housing Units

A **building** (`Building`) is a property assigned to an [estate](../../estates-and-access/concepts/estate-and-access.md). It may contain housing units and may be associated with an owner.

A **housing unit** (`HousingUnit`) belongs to exactly one building. It represents a dwelling or other unit within that building and may record a unit number, floor, landing, total surface, terrace, garden, and a direct owner. A **room** (`Room`) describes a room or space within a housing unit.

Rooms are classified by type and may record an approximate surface. The sum of room surfaces describes the room composition; the application also stores the housing unit's total surface separately.

An energy performance rating (PEB) is recorded against a housing unit as a dated history that may include certificate details and a validity end date. The current rating is derived from that history. Observed rating and certificate behavior is described in the [housing-unit rules](../rules/housing-units.md) and [PEB Score Management](../../../technical/buildings-and-housing-units/peb-score-management.md); PEB expiry alerts are part of the cross-cutting [Alerts](../../cross-cutting/concepts/alerts.md) concept.

The model permits a housing unit to have a direct owner different from the building's owner. When no direct owner is recorded, the application displays the building owner's name as the unit's effective owner; whether this display fallback represents business ownership inheritance remains unresolved.

Observed constraints are recorded in the [building rules](../rules/buildings.md) and [housing-unit rules](../rules/housing-units.md). Current building operations are documented in [Building Management](../../../technical/buildings-and-housing-units/building-management.md); housing-unit and room APIs are documented in [Housing Unit Management](../../../technical/buildings-and-housing-units/housing-unit-management.md) and [Room Management](../../../technical/buildings-and-housing-units/room-management.md). Persisted fields and relationships are described in the [housing-unit data dictionary](../../../technical/buildings-and-housing-units/housing-unit-data-dictionary.md) and the cross-cutting [data model](../../../technical/cross-cutting/data-model.md).

Tracked [equipment](../../equipment/concepts/property-equipment.md)—including boilers, fire extinguishers, and meters—may be associated with a building or a housing unit. Equipment is a separate domain concept from the property structure.
