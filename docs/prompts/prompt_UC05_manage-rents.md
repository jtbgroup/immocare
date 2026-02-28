# ImmoCare — UC005 Manage Rents — Implementation Prompt

I want to implement Use Case UC005 - Manage Rents for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US021 | Set Initial Rent | MUST HAVE | 3 |
| US022 | Edit a Rent Record | MUST HAVE | 3 |
| US023 | View Rent History | SHOULD HAVE | 2 |
| US024 | Track Rent Increases Over Time | COULD HAVE | 3 |
| US025 | Add Notes to Rent Changes | SHOULD HAVE | 1 |

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
   - `GET /` → US023 (full history)
   - `POST /` → US021/US022 (set/update rent)

## FRONTEND (integrated into HousingUnitDetailsComponent)

9. `rent-history.model.ts` — `RentHistoryDTO`, `SetRentRequest`
10. `rent-history.service.ts`
11. Rent section in `HousingUnitDetailsComponent`:
    - No rent → "No rent recorded" + "Set Rent" button (US021)
    - Current rent → "€850.00/month", effective date, trend indicator, "Update Rent" + "View History" (US022, US023)
    - "View History" → collapsible table: Rent, From, To, Duration, Notes (US023)
    - Trend indicators ↑/↓/− between consecutive entries (US024)
    - Total increase summary (US024)
    - Notes field in form (US025)

## ACCEPTANCE CRITERIA

- [ ] POST creates new record and closes previous (effectiveTo = newFrom - 1 day)
- [ ] Amount €850.00 displayed with € symbol
- [ ] Same-amount warning shown (not blocked)
- [ ] Date > 1 year in future rejected
- [ ] All US021–US025 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
