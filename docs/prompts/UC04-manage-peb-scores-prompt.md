# ImmoCare — UC004 Manage PEB Scores — Implementation Prompt

I want to implement Use Case UC004 - Manage PEB Scores for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US017 | Add PEB Score to Housing Unit | MUST HAVE | 3 |
| US018 | View PEB Score History | SHOULD HAVE | 2 |
| US019 | Check PEB Certificate Validity | SHOULD HAVE | 2 |
| US020 | Track PEB Score Improvements | COULD HAVE | 3 |

## PEB_SCORE_HISTORY ENTITY

```
peb_score_history {
  id                 BIGINT PK
  housing_unit_id    BIGINT FK NOT NULL → housing_unit
  peb_score          VARCHAR(10) NOT NULL  -- A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G
  score_date         DATE NOT NULL
  certificate_number VARCHAR(50) NULL
  valid_until        DATE NULL
  created_at         TIMESTAMP NOT NULL
}
```

**Pattern**: Append-only. No UPDATE, no DELETE. Current = most recent `score_date`. Status computed: CURRENT / HISTORICAL / EXPIRED (`valid_until` < today).

## BACKEND

1. Flyway `VXX__create_peb_score_history.sql` — indexes on `(housing_unit_id, score_date DESC)` and `(housing_unit_id, valid_until)`
2. `PebScoreHistory` entity — `@PrePersist` only (no `@PreUpdate`)
3. `PebScoreHistoryDTO` — `{ id, housingUnitId, pebScore, scoreDate, certificateNumber, validUntil, createdAt, status, validityStatus }`
4. `AddPebScoreRequest` — `{ @NotBlank pebScore, @NotNull @PastOrPresent scoreDate, certificateNumber?, validUntil? }`
5. `PebScoreMapper` (MapStruct) — compute `status` and `validityStatus` in default methods
6. `PebScoreRepository` — `findByHousingUnitIdOrderByScoreDateDesc`
7. `PebScoreService`:
   - `getHistory(unitId)` → `List<PebScoreHistoryDTO>` (status computed for each)
   - `addScore(unitId, req)` — validate: score date ≤ today; validUntil > scoreDate if provided
   - `getImprovementSummary(unitId)` → comparison between consecutive entries (US020)
8. `PebScoreController` — base `/api/v1/housing-units/{unitId}/peb-scores`:
   - `GET /` → US018 (full history)
   - `POST /` → US017 (add score)

## FRONTEND (integrated into HousingUnitDetailsComponent)

9. `peb-score.model.ts` — `PebScore` enum, `PEB_SCORE_LABELS`, `PEB_SCORE_COLORS`, `PebScoreHistoryDTO`, `AddPebScoreRequest`
10. `peb-score.service.ts`
11. PEB section in `HousingUnitDetailsComponent`:
    - Current score badge (color-coded A++ dark green → G dark red) + date (US017)
    - Validity badge: red "Expired" / orange "Expires soon" / none (US019)
    - Improvement arrow indicator between consecutive scores (US020)
    - "View History" → collapsible table: Score, Date, Certificate, Valid Until, Status (US018)
    - "Add PEB Score" button → inline form with color-coded dropdown (US017)

## ACCEPTANCE CRITERIA

- [ ] POST: score date required; future date rejected; append-only (no PUT/DELETE)
- [ ] GET: sorted by score_date DESC; status computed correctly
- [ ] Expiry: red when valid_until < today; orange within 3 months
- [ ] Improvement: ↑/↓/− indicators between consecutive entries
- [ ] All US017–US020 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
