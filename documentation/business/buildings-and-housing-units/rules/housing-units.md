---
title: Housing Unit Rules
type: business rules
status: draft
last_verified: 2026-11-07
verified_against: "HousingUnit, Room, PebScore, PebScoreHistory, CreateHousingUnitRequest, UpdateHousingUnitRequest, CreateRoomRequest, UpdateRoomRequest, BatchCreateRoomsRequest, CreatePebScoreRequest, HousingUnitService, RoomService, PebScoreService, PebScoreController, EstateSecurityService, AlertService, V006__uc006_manage_housing_units.sql, V007__uc007_manage_rooms.sql, V008__uc008_manage_peb_scores.sql, stakeholder confirmation 2026-11-07"
---

# Business Rules — Housing Units

These rules describe constraints enforced by the current implementation or established by confirmed business policy. Code-derived rules need business validation; implementation alone does not establish business intent.

| ID | Rule | Status |
|---|---|---|
| BR-HUNIT-001 | A unit number must be unique within its building without regard to letter case. | Needs business validation |
| BR-HUNIT-002 | A housing-unit floor must be between -10 and 100 inclusive. | Needs business validation |
| BR-HUNIT-003 | If a total surface is provided, it must be greater than zero. | Needs business validation |
| BR-HUNIT-004 | If a housing unit is recorded as having no terrace, its terrace surface and orientation are cleared. | Needs business validation |
| BR-HUNIT-005 | If a housing unit is recorded as having no garden, its garden surface and orientation are cleared. | Needs business validation |
| BR-HUNIT-006 | The application blocks housing-unit deletion when the unit has one or more rooms or fire extinguishers. | Needs clarification |
| BR-HUNIT-007 | If a terrace or garden orientation is provided, it must be one of N, S, E, W, NE, NW, SE, or SW. | Needs business validation |
| BR-HUNIT-008 | When a housing unit is updated with a terrace or garden, the corresponding surface must be greater than zero. | Needs clarification |
| BR-HUNIT-009 | A housing unit can reference at most one person as its direct owner. | Needs business validation |
| BR-HUNIT-010 | A room type must be one of `LIVING_ROOM`, `BEDROOM`, `KITCHEN`, `BATHROOM`, `TOILET`, `HALLWAY`, `STORAGE`, `OFFICE`, `DINING_ROOM`, or `OTHER`. | Needs business validation |
| BR-HUNIT-011 | A room's approximate surface must be greater than zero and less than 1,000 m². | Needs business validation |
| BR-HUNIT-012 | A room must be associated with an existing housing unit. | Needs business validation |
| BR-HUNIT-013 | A batch of rooms must contain between 1 and 20 entries. | Needs business validation |
| BR-HUNIT-014 | A housing unit may contain multiple rooms of the same type. | Needs business validation |
| BR-HUNIT-015 | A room batch is persisted in full or not at all. | Needs clarification |
| BR-HUNIT-016 | A recorded energy performance rating must be one of A++, A+, A, B, C, D, E, F, or G. | Needs business validation |
| BR-HUNIT-017 | An energy performance rating date cannot be in the future. | Needs business validation |
| BR-HUNIT-018 | If a certificate validity end date is provided, it must be after the rating date. | Needs business validation |
| BR-HUNIT-019 | The active energy performance rating is the record with the most recent certificate validity end date. | Confirmed |
| BR-HUNIT-020 | An estate manager may modify only the active energy performance rating. | Confirmed |
| BR-HUNIT-021 | A PEB record without a validity end date, or a tie for the most recent validity end date, has no defined active-record selection outcome. | Needs clarification |
| BR-HUNIT-022 | A certificate is marked expired when its validity end date is before the current date; a non-expired certificate receives an expiring-soon detail warning when its end date is before the date three calendar months from today. | Needs business validation |
| BR-HUNIT-023 | The application raises an estate alert for a PEB record with the most recent rating date when its validity end date is on or before the date obtained by adding that estate's configured warning period to today. | Needs business validation |
| BR-HUNIT-024 | A platform administrator may modify any energy performance rating in a housing unit's history. | Confirmed |

The PEB active-record and modification policies in BR-HUNIT-019, BR-HUNIT-020, and BR-HUNIT-024 were confirmed by the user on 2026-11-07. The application currently selects records by rating date and does not enforce the confirmed edit permissions. Creation does not apply the same required-surface check as updating; housing-unit deletion also has different effects depending on associated data. The intended behavior for mixed-validity room batches is inconsistent between the use-case analysis and a related story. PEB active-record selection for missing or tied validity dates and deletion permissions remain unresolved; see [Open Questions](../../../../backlog/open_questions.md#housing-units-and-leases) and [PEB scores](../../../../backlog/open_questions.md#peb-scores).

## Observed sources

`HousingUnit`, `Room`, `PebScore`, `PebScoreHistory`, the housing-unit, room, and PEB request DTOs, `HousingUnitService`, `RoomService`, `PebScoreService`, `PebScoreController`, `PebScoreRepository`, `AlertService`, `EstateConfigDTOs`, `HousingUnitController`, `RoomController`, `HousingUnitHasDataException`, `GlobalExceptionHandler`, and migrations `V006__uc006_manage_housing_units.sql`, `V007__uc007_manage_rooms.sql`, and `V008__uc008_manage_peb_scores.sql`.
