---
title: Building Rules
type: business rules
status: draft
last_verified: 2026-11-07
verified_against: b99e38fb08888ee0973fc560a27258b15da926b0
---

# Business Rules — Buildings

These rules describe constraints enforced by the current implementation. Code-derived rules need business validation; implementation alone does not establish business intent.

| ID | Rule | Status |
|---|---|---|
| BR-BUILD-001 | A building record requires a name, street address, postal code, city, and country. | Needs business validation |
| BR-BUILD-002 | A building may be associated with at most one person as its owner. | Needs business validation |
| BR-BUILD-003 | A building cannot be deleted while it has one or more housing units. | Needs business validation |

The difference between the form's default country and the API's required country, the response status for an unknown owner, the response status for deletion blocked by housing units, and the fields searched when a city filter is also applied remain open decisions in [Open Questions](../../../../backlog/open_questions.md#buildings).

## Observed sources

`Building`, `CreateBuildingRequest`, `UpdateBuildingRequest`, `BuildingService`, `BuildingController`, `BuildingHasUnitsException`, `GlobalExceptionHandler`, and migration `V005__uc005_manage_buildings.sql`.
