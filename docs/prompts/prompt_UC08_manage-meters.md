# ImmoCare — UC008 Manage Meters — Implementation Prompt

I want to implement Use Case UC008 - Manage Meters for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US036 | View Meters of a Housing Unit | MUST HAVE | 2 |
| US037 | View Meters of a Building | MUST HAVE | 2 |
| US038 | Add a Meter to a Housing Unit | MUST HAVE | 3 |
| US039 | Add a Meter to a Building | MUST HAVE | 3 |
| US040 | Replace a Meter | MUST HAVE | 3 |
| US041 | Remove a Meter | SHOULD HAVE | 2 |
| US042 | View Meter History | SHOULD HAVE | 2 |

## METER ENTITY

```
meter {
  id                   BIGINT PK
  type                 VARCHAR(20) NOT NULL    -- WATER, GAS, ELECTRICITY
  meter_number         VARCHAR(50) NOT NULL
  label                VARCHAR(100) NULL       -- optional human-readable label
  ean_code             VARCHAR(18)  NULL       -- required for GAS, ELECTRICITY
  installation_number  VARCHAR(50)  NULL       -- required for WATER
  customer_number      VARCHAR(50)  NULL       -- required for WATER on BUILDING
  owner_type           VARCHAR(20) NOT NULL    -- HOUSING_UNIT, BUILDING
  owner_id             BIGINT NOT NULL
  start_date           DATE NOT NULL
  end_date             DATE NULL               -- NULL = active
  created_at           TIMESTAMP NOT NULL
}
```

**Pattern**: Append-only. Records never modified. Close = set `end_date`. Active = `end_date IS NULL`. Multiple active meters of same type per owner allowed.

## CONDITIONAL FIELD RULES

| Type | Context | Required fields |
|------|---------|-----------------|
| GAS | any | ean_code |
| ELECTRICITY | any | ean_code |
| WATER | HOUSING_UNIT | installation_number |
| WATER | BUILDING | installation_number + customer_number |

## BACKEND

1. Flyway `VXX__create_meter.sql` — index on `(owner_type, owner_id, end_date)` for active meter queries
2. `Meter` entity — `@PrePersist` only
3. `MeterDTO` — `{ id, type, meterNumber, label, eanCode, installationNumber, customerNumber, ownerType, ownerId, startDate, endDate, createdAt, isActive, durationMonths }`
4. `AddMeterRequest` — `{ @NotBlank type, @NotBlank meterNumber, label?, eanCode?, installationNumber?, customerNumber?, @NotNull startDate }`
5. `ReplaceMeterRequest` — `{ @NotBlank meterNumber, eanCode?, installationNumber?, customerNumber?, @NotNull newStartDate, reason? }`
6. `RemoveMeterRequest` — `{ @NotNull endDate }`
7. `MeterMapper` — compute `isActive` and `durationMonths`
8. `MeterRepository`:
   - `findByOwnerTypeAndOwnerIdOrderByStartDateDesc`
   - `findByOwnerTypeAndOwnerIdAndEndDateIsNull`
   - `findByIdAndEndDateIsNull`
9. `MeterService`:
   - `getActiveMeters(ownerType, ownerId)` → `List<MeterDTO>` — US036/US037
   - `getAllMeters(ownerType, ownerId)` → `List<MeterDTO>` — US042
   - `addMeter(ownerType, ownerId, req)` — validate conditional fields; startDate not future — US038/US039
   - `replaceMeter(meterId, req)` — atomic: close current (endDate = newStartDate), create new active — US040
   - `removeMeter(meterId, req)` — set endDate; validate endDate ≥ startDate and not future — US041
10. `MeterController`:

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/housing-units/{unitId}/meters?status=active | US036 |
| GET | /api/v1/buildings/{buildingId}/meters?status=active | US037 |
| POST | /api/v1/housing-units/{unitId}/meters | US038 |
| POST | /api/v1/buildings/{buildingId}/meters | US039 |
| PUT | /api/v1/housing-units/{unitId}/meters/{id}/replace | US040 |
| PUT | /api/v1/buildings/{buildingId}/meters/{id}/replace | US040 |
| DELETE | /api/v1/housing-units/{unitId}/meters/{id} | US041 |
| DELETE | /api/v1/buildings/{buildingId}/meters/{id} | US041 |
| GET | /api/v1/housing-units/{unitId}/meters | US042 (all, no filter) |
| GET | /api/v1/buildings/{buildingId}/meters | US042 (all, no filter) |

## FRONTEND (integrated into HousingUnitDetailsComponent and BuildingDetailsComponent)

11. `meter.model.ts` — `MeterType`, `OwnerType`, `MeterDTO`, `AddMeterRequest`, `ReplaceMeterRequest`, `RemoveMeterRequest`
12. `meter.service.ts`
13. Meters section in both Details components:
    - 3 blocks: WATER / GAS / ELECTRICITY
    - Each block: active meters list; empty → "No [type] meter assigned" + "Add Meter" button
    - WATER block on BUILDING shows `customerNumber`
    - Duration shown in months
    - "View History" link (only if ≥1 meter exists)
    - "Replace" and "Remove" buttons per active meter
    - History: inline collapsible table, Active (green) / Closed (grey) badges

## ACCEPTANCE CRITERIA

- [ ] GAS/ELECTRICITY: EAN required; WATER on unit: installationNumber required; WATER on building: both installationNumber + customerNumber required
- [ ] Replace: atomic — old meter closed, new active; both in history
- [ ] Remove: endDate set; not future; ≥ meter startDate
- [ ] History: duration correct for active (months since start) and closed (months between start/end)
- [ ] All US036–US042 acceptance criteria verified

**Last Updated**: 2026-02-27 | **Branch**: `develop` | **Status**: ✅ Implemented
