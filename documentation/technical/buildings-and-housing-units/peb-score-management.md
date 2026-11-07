---
title: PEB Score Management
type: technical reference
status: current
last_verified: 2026-11-07
verified_against: "PebScoreController, PebScoreService, PebScoreRepository, CreatePebScoreRequest, PebScoreHistory, PebSectionComponent, PebHistoryComponent, AlertService, EstateConfigDTOs, V008__uc008_manage_peb_scores.sql"
---

# PEB Score Management

This page describes the APIs, persistence, validation, history presentation, and expiry alerts for energy performance ratings recorded against housing units. The business concept is defined in [Buildings and Housing Units](../../business/buildings-and-housing-units/concepts/buildings-and-housing-units.md); code-derived constraints and their statuses are in [Housing Unit Rules](../../business/buildings-and-housing-units/rules/housing-units.md). The persisted record is described in the [Housing Unit Data Dictionary](./housing-unit-data-dictionary.md).

## Access and estate scope

All PEB routes are under `/api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores`. Estate members can read the history, current rating, and improvement summary. Estate managers can create, edit, and delete records.

The controller authorizes membership or manager access to the route's estate, but the service resolves the housing unit using only `unitId` and does not verify that the unit belongs to `{estateId}`. Cross-estate isolation is therefore not guaranteed; this scope gap is recorded in the [backlog](../../../backlog/backlog.md).

## API behavior

| Operation | Endpoint | Access | Current behavior |
|---|---|---|---|
| Add a rating | `POST /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores` | Estate manager | Creates a history record and returns `201 Created`. |
| Edit a rating | `PUT /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/{scoreId}` | Estate manager | Updates an existing record and returns the updated DTO. |
| Delete a rating | `DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/{scoreId}` | Estate manager | Deletes a record and returns `204 No Content`. |
| Read rating history | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores` | Estate member | Returns all records ordered by rating date descending. Tied dates have no explicit ordering. |
| Read current rating | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/current` | Estate member | Returns the first record ordered by rating date descending, or `204 No Content` if no record exists. |
| Read improvement summary | `GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/improvements` | Estate member | Returns the first-to-current grade difference and adjacent history steps, or `204 No Content` when there is no history. |

Although the product analysis describes PEB history as append-only, the current API and interface expose edit and delete operations. The unresolved policy is tracked in [Open Questions](../../../backlog/open_questions.md#peb-scores).

## Validation and computed values

- A rating is required and must be one of `A_PLUS_PLUS`, `A_PLUS`, `A`, `B`, `C`, `D`, `E`, `F`, or `G`.
- The rating date is required and cannot be in the future.
- The optional certificate number is limited to 50 characters and accepts letters, digits, and hyphens.
- If supplied, the validity end date must be after the rating date.
- Invalid create or update data returns `400 Bad Request`. A missing housing unit, or a rating ID not found for the requested unit during update or deletion, returns `404 Not Found`. The current-rating and improvement queries return `204 No Content` when the unit has no PEB records.
- The current rating is selected by descending rating date. There is no secondary ordering for records with the same date, so the single-current selection is not deterministic in that case.
- For each record, `status` is `EXPIRED` when its validity end date is before today, otherwise `CURRENT` for the selected current record, or `HISTORICAL` for another record. An omitted validity date does not cause an expired status.
- `expiryWarning` is `NO_DATE` when no validity end date exists, `EXPIRED` when it is before today, `EXPIRING_SOON` when the non-expired date is earlier than three calendar months from today, and `VALID` otherwise. The detail view's three-month threshold is fixed in the service.
- The improvement summary compares the oldest and newest records using the grade order from best (`A_PLUS_PLUS`) to worst (`G`). It returns a positive grade difference for improvement, a negative difference for degradation, and one transition per adjacent pair. The number of years is the count of complete calendar years between the dates.

## PEB expiry alerts

The cross-cutting [Alerts](../../business/cross-cutting/concepts/alerts.md) list includes alerts for current PEB certificates nearing or past their validity end date. The alert service uses each estate's `contracts.alert.peb.expiry.warning.months` setting, defaulting to three months, and includes a record whose validity date is on or before the resulting threshold. Expired certificates are presented as danger alerts; other matching certificates are warning alerts.

This estate-configured alert threshold is separate from the fixed three-month `expiryWarning` calculation returned for the housing-unit detail view. Whether those thresholds should be aligned is an [open question](../../../backlog/open_questions.md#peb-scores).

## Frontend behavior

The housing-unit detail page shows the current grade with a color-coded badge, rating date, and any certificate number or validity date. It provides an inline form for adding a record and a history view with certificate details, status, transition indicators, and an overall improvement/degradation summary. The form limits the rating date to today and checks that a supplied validity date follows the rating date. The grade palette progresses from green through yellow and red, but grade `G` is purple rather than the dark red specified in the product analysis. The selector lists grade labels without applying the per-grade colors specified for the options.

The history view also exposes edit and delete actions, consistent with the current API but inconsistent with an append-only history policy. With no current rating, the detail section displays a no-data state. Failures while loading the current rating, history, or improvement summary are not surfaced to the user; history or summary failures can appear as empty data.

## Persistence

Records are stored in `peb_score_history`, linked to one housing unit. Deleting a housing unit cascades to its PEB history. The data dictionary documents the persisted columns and database constraints.

## Implementation references

- Routes and access annotations: [PebScoreController](../../../backend/src/main/java/com/immocare/controller/PebScoreController.java)
- Validation, history, and computed status: [PebScoreService](../../../backend/src/main/java/com/immocare/service/PebScoreService.java) and [CreatePebScoreRequest](../../../backend/src/main/java/com/immocare/model/dto/CreatePebScoreRequest.java)
- Query ordering and estate alert selection: [PebScoreRepository](../../../backend/src/main/java/com/immocare/repository/PebScoreRepository.java)
- Estate alert aggregation and threshold: [AlertService](../../../backend/src/main/java/com/immocare/service/AlertService.java) and [EstateConfigDTOs](../../../backend/src/main/java/com/immocare/model/dto/EstateConfigDTOs.java)
- Entity and migration: [PebScoreHistory](../../../backend/src/main/java/com/immocare/model/entity/PebScoreHistory.java), [PebScore](../../../backend/src/main/java/com/immocare/model/entity/PebScore.java), and [V008__uc008_manage_peb_scores.sql](../../../backend/src/main/resources/db/migration/V008__uc008_manage_peb_scores.sql)
- Frontend service, form, and history: [PebScoreService](../../../frontend/src/app/core/services/peb-score.service.ts), [PEB section](../../../frontend/src/app/features/housing-unit/components/peb-section/), and [PEB history](../../../frontend/src/app/features/housing-unit/components/peb-history/)
