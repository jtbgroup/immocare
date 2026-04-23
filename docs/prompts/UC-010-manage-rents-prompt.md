# ImmoCare — UC005 Manage Rents — Implementation Prompt

I want to implement Use Case UC005 - Manage Rents for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| UC010.001 | Set Initial Rent | MUST HAVE | 3 |
| UC010.002 | Edit a Rent Record | MUST HAVE | 3 |
| UC010.003 | View Rent History | SHOULD HAVE | 2 |
| UC010.004 | Track Rent Increases Over Time | COULD HAVE | 3 |
| UC010.005 | Add Notes to Rent Changes | SHOULD HAVE | 1 |

## RENT_HISTORY ENTITY

```
rent_history {
  id               BIGINT PK
  housing_unit_id  BIGINT FK NOT NULL → housing_unit
  monthly_rent     DECIMAL(10,2) NOT NULL  -- > 0, EUR
  effective_from   DATE NOT NULL
  effective_to     DATE NULL               -- NULL = current rent
  notes            VARCHAR(500) NULL
  created_at       TIMESTAMP NOT NULL
}
```

**Pattern**: Timeline. One active record (`effective_to IS NULL`). Adding a new rent auto-closes the current one (`effective_to = newEffectiveFrom - 1 day`).

## BACKEND

1. Flyway `VXX__create_rent_history.sql`
2. `RentHistory` entity
3. `RentHistoryDTO` — `{ id, housingUnitId, monthlyRent, effectiveFrom, effectiveTo, notes, createdAt, isCurrent, durationMonths }`
4. `SetRentRequest` — `{ @NotNull @Positive monthlyRent, @NotNull effectiveFrom, notes? }`
5. `RentHistoryMapper` (MapStruct) — compute `isCurrent` and `durationMonths` in default method
6. `RentHistoryRepository` — `findByHousingUnitIdAndEffectiveToIsNull`, `findByHousingUnitIdOrderByEffectiveFromDesc`
7. `RentHistoryService`:
   - `getHistory(unitId)` → `List<RentHistoryDTO>`
   - `setRent(unitId, req)` → closes current record, creates new one
   - Validate: `monthlyRent > 0`; `effectiveFrom` not more than 1 year in future
8. `RentHistoryController` — base `/api/v1/housing-units/{unitId}/rent-history`:
   - `GET /` → UC010.003 (full history)
   - `POST /` → UC010.001/UC010.002 (set/update rent)

## FRONTEND (integrated into HousingUnitDetailsComponent)

9. `rent-history.model.ts` — `RentHistoryDTO`, `SetRentRequest`
10. `rent-history.service.ts`
11. Rent section in `HousingUnitDetailsComponent`:
    - No rent → "No rent recorded" + "Set Rent" button (UC010.001)
    - Current rent → "€850.00/month", effective date, trend indicator, "Update Rent" + "View History" (UC010.002, UC010.003)
    - "View History" → collapsible table: Rent, From, To, Duration, Notes (UC010.003)
    - Trend indicators ↑/↓/− between consecutive entries (UC010.004)
    - Total increase summary (UC010.004)
    - Notes field in form (UC010.005)

## ACCEPTANCE CRITERIA

- [ ] POST creates new record and closes previous (effectiveTo = newFrom - 1 day)
- [ ] Amount €850.00 displayed with € symbol
- [ ] Same-amount warning shown (not blocked)
- [ ] Date > 1 year in future rejected
- [ ] All UC010.001–UC010.005 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
