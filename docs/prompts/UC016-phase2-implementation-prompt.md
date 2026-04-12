# ImmoCare — UC016 Manage Estates — Phase 2 Implementation Prompt

I want to implement UC016 - Manage Estates (Phase 2 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Prerequisite**: Phase 1 (V017) must be fully deployed and tested before starting this phase.
- **Flyway**: last migration is V017. Use **V018** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 2 of 6** of the multi-tenant estate migration.

| Phase | Flyway | Scope |
|---|---|---|
| Phase 1 | V017 | ✅ Done — Estate CRUD, membership, `app_user` migration |
| **Phase 2 (this prompt)** | V018 | `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V019 | `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V020 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| Phase 5 | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

## WHAT CHANGES IN THIS PHASE

- `estate_id UUID NOT NULL` added to `building`
- `BuildingController` routes migrated to `/api/v1/estates/{estateId}/buildings/**`
- `BuildingService` all queries filtered by `estateId`
- `HousingUnitController` routes migrated to `/api/v1/estates/{estateId}/units/**`
- `HousingUnitService` all queries filtered by `estateId` via building
- `FireExtinguisherController` routes migrated (fire extinguishers belong to a building)
- Frontend services updated to inject `estateId` in all URLs via `ActiveEstateService`
- `EstateDashboardDTO` counts for `totalBuildings` and `totalUnits` now populated

## WHAT DOES NOT CHANGE

- `housing_unit`, `room`, `meter`, `boiler`, `peb_score_history`, `rent_history`, `fire_extinguisher` — no new columns; estate scope derived via `building.estate_id`
- `PersonController`, `LeaseController`, `FinancialTransactionController` — untouched
- Phase 1 components and services — untouched

---

## DATABASE MIGRATION — `V018__estate_scope_buildings.sql`

```sql
-- Use case: UC016 — Manage Estates (Phase 2)

-- Add estate_id to building
ALTER TABLE building
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_building_estate ON building(estate_id);
```

> There is no existing data to migrate — the application starts fresh.

---

## BACKEND

### Modified Entity: `Building`

- Add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL).
- No other changes to the entity.

### Modified Repository: `BuildingRepository`

Replace or add all queries to include `estateId` filter:

```java
Page<Building> findByEstateId(UUID estateId, Pageable pageable);

@Query("""
    SELECT b FROM Building b
    WHERE b.estate.id = :estateId
    AND (LOWER(b.name) LIKE LOWER(CONCAT('%',:search,'%'))
      OR LOWER(b.streetAddress) LIKE LOWER(CONCAT('%',:search,'%'))
      OR LOWER(b.city) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
Page<Building> searchByEstate(@Param("estateId") UUID estateId,
                               @Param("search") String search,
                               Pageable pageable);

List<String> findDistinctCitiesByEstateId(UUID estateId);

boolean existsByEstateIdAndId(UUID estateId, Long buildingId);

long countByEstateId(UUID estateId);
```

Remove all existing methods that query without `estateId`.

### Modified Service: `BuildingService`

All methods receive `estateId` as first parameter:

```java
Page<BuildingDTO> getAllBuildings(UUID estateId, String search, String city, Pageable pageable);
BuildingDTO getBuildingById(UUID estateId, Long id);
// Verify building.estate.id == estateId → 403 if mismatch

BuildingDTO createBuilding(UUID estateId, CreateBuildingRequest req);
// Set building.estate from estateId

BuildingDTO updateBuilding(UUID estateId, Long id, UpdateBuildingRequest req);
void deleteBuilding(UUID estateId, Long id);
List<String> getAllCities(UUID estateId);
```

Ownership verification helper (reuse across services):
```java
private void verifyBuildingBelongsToEstate(UUID estateId, Long buildingId) {
    if (!buildingRepository.existsByEstateIdAndId(estateId, buildingId)) {
        throw new EstateAccessDeniedException();
    }
}
```

### Modified Controller: `BuildingController`

Change `@RequestMapping` from `/api/v1/buildings` to `/api/v1/estates/{estateId}/buildings`.

Add `@PathVariable UUID estateId` to all methods. Add `@PreAuthorize("@security.isMemberOf(#estateId)")` at class level (override with `isManagerOf` on mutating methods).

| Method | Path | PreAuthorize | Story |
|--------|------|------|-------|
| GET | `/api/v1/estates/{estateId}/buildings` | `isMemberOf` | US004 |
| GET | `/api/v1/estates/{estateId}/buildings/{id}` | `isMemberOf` | US004 |
| GET | `/api/v1/estates/{estateId}/buildings/cities` | `isMemberOf` | US005b |
| POST | `/api/v1/estates/{estateId}/buildings` | `isManagerOf` | US001 |
| PUT | `/api/v1/estates/{estateId}/buildings/{id}` | `isManagerOf` | US002 |
| DELETE | `/api/v1/estates/{estateId}/buildings/{id}` | `isManagerOf` | US003 |

### Modified Repository: `HousingUnitRepository`

Add `estateId` filter via building join to all queries:

```java
@Query("SELECT u FROM HousingUnit u WHERE u.building.estate.id = :estateId AND u.building.id = :buildingId ORDER BY u.floor ASC, u.unitNumber ASC")
List<HousingUnit> findByEstateIdAndBuildingId(@Param("estateId") UUID estateId, @Param("buildingId") Long buildingId);

boolean existsByBuilding_Estate_IdAndId(UUID estateId, Long unitId);

long countByBuilding_Estate_Id(UUID estateId);
```

### Modified Service: `HousingUnitService`

All methods receive `estateId` as first parameter. Add `verifyUnitBelongsToEstate(UUID estateId, Long unitId)` helper.

### Modified Controller: `HousingUnitController`

Change all routes to `/api/v1/estates/{estateId}/units/**`. Add `@PathVariable UUID estateId` and appropriate `@PreAuthorize` annotations.

| Method | Path |
|--------|------|
| GET | `/api/v1/estates/{estateId}/buildings/{buildingId}/units` |
| GET | `/api/v1/estates/{estateId}/units/{id}` |
| POST | `/api/v1/estates/{estateId}/units` |
| PUT | `/api/v1/estates/{estateId}/units/{id}` |
| DELETE | `/api/v1/estates/{estateId}/units/{id}` |

### Modified Controller: `FireExtinguisherController`

Fire extinguishers belong to a building — scope via building's estate:

| Old path | New path |
|----------|----------|
| `/api/v1/buildings/{buildingId}/fire-extinguishers` | `/api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers` |
| `/api/v1/fire-extinguishers/{id}` | `/api/v1/estates/{estateId}/fire-extinguishers/{id}` |
| `/api/v1/fire-extinguishers/{id}/revisions` | `/api/v1/estates/{estateId}/fire-extinguishers/{id}/revisions` |
| `/api/v1/fire-extinguishers/{extId}/revisions/{revId}` | `/api/v1/estates/{estateId}/fire-extinguishers/{extId}/revisions/{revId}` |

Add `verifyFireExtinguisherBelongsToEstate(UUID estateId, Long extId)` in `FireExtinguisherService`.

### Updated `EstateDashboardDTO` population

In `EstateService.getDashboard()`, replace the 0 stubs:

```java
int totalBuildings = (int) buildingRepository.countByEstateId(estateId);
int totalUnits     = (int) housingUnitRepository.countByBuilding_Estate_Id(estateId);
// activeLeases and pendingAlerts remain 0 until Phase 3/6
```

---

## FRONTEND

### HTTP Interceptor — `EstateHttpInterceptor`

Angular `HttpInterceptorFn` that prepends the active estate context to all estate-scoped API calls:

```typescript
// The interceptor does NOT rewrite URLs automatically.
// Each service method constructs its own URL using activeEstateService.activeEstateId().
// This interceptor only adds a custom header X-Estate-Id for server-side logging (optional).
```

> URL construction remains explicit in each service method — no magic rewriting.

### Modified Service: `BuildingService`

Inject `ActiveEstateService`. Prepend `estateId` to all URLs:

```typescript
private get estateId(): string {
  return this.activeEstateService.activeEstateId()!;
}

getAllBuildings(page, size, sort?, city?, search?): Observable<Page<Building>>
  → GET /api/v1/estates/{estateId}/buildings

getBuildingById(id): Observable<Building>
  → GET /api/v1/estates/{estateId}/buildings/{id}

getCities(): Observable<string[]>
  → GET /api/v1/estates/{estateId}/buildings/cities

createBuilding(req): Observable<Building>
  → POST /api/v1/estates/{estateId}/buildings

updateBuilding(id, req): Observable<Building>
  → PUT /api/v1/estates/{estateId}/buildings/{id}

deleteBuilding(id): Observable<void>
  → DELETE /api/v1/estates/{estateId}/buildings/{id}
```

### Modified Service: `HousingUnitService`

Same pattern — inject `ActiveEstateService`, update all URLs:

```typescript
getUnitsByBuilding(buildingId): Observable<HousingUnit[]>
  → GET /api/v1/estates/{estateId}/buildings/{buildingId}/units

getUnitById(id): Observable<HousingUnit>
  → GET /api/v1/estates/{estateId}/units/{id}

createUnit(req): Observable<HousingUnit>
  → POST /api/v1/estates/{estateId}/units

updateUnit(id, req): Observable<HousingUnit>
  → PUT /api/v1/estates/{estateId}/units/{id}

deleteUnit(id): Observable<void>
  → DELETE /api/v1/estates/{estateId}/units/{id}
```

### Modified Service: `FireExtinguisherService`

Update all URLs to include `estateId`.

### Routing updates

```typescript
// Replace existing flat routes with estate-scoped routes
{ path: 'estates/:estateId/buildings',
  loadChildren: () => import('./features/building/building.routes') },

{ path: 'estates/:estateId/units',
  loadChildren: () => import('./features/housing-unit/housing-unit.routes') },
```

All internal `routerLink` and `router.navigate()` calls in building and housing unit components must be updated to include `estateId`.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Building must belong to the estate in the URL | Backend: `verifyBuildingBelongsToEstate()` on all GET/PUT/DELETE by id |
| Housing unit must belong to the estate in the URL | Backend: `verifyUnitBelongsToEstate()` |
| Fire extinguisher must belong to the estate in the URL | Backend: `verifyFireExtinguisherBelongsToEstate()` |
| VIEWER cannot create/edit/delete buildings or units | Backend: `@PreAuthorize("@security.isManagerOf(#estateId)")` on mutating endpoints |
| estateId always explicit in URL | No implicit scoping — always passed as path variable |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify `PersonController`, `LeaseController`, `RoomController`, `MeterController`, `BoilerController`, `PebScoreController`, `RentHistoryController`
- Do NOT add `estate_id` to any table other than `building`
- Do NOT implement VIEWER enforcement in the frontend beyond what `@PreAuthorize` provides on the backend

---

## ACCEPTANCE CRITERIA

- [ ] V018: `building.estate_id` column added with FK and index
- [ ] All building endpoints moved to `/api/v1/estates/{estateId}/buildings/**`
- [ ] All housing unit endpoints moved to `/api/v1/estates/{estateId}/units/**`
- [ ] All fire extinguisher endpoints moved to `/api/v1/estates/{estateId}/fire-extinguishers/**`
- [ ] Accessing a building from the wrong estate → HTTP 403
- [ ] `BuildingService`, `HousingUnitService`, `FireExtinguisherService` all filter by `estateId`
- [ ] Dashboard `totalBuildings` and `totalUnits` return real counts
- [ ] Frontend `BuildingService` and `HousingUnitService` use `estateId` from `ActiveEstateService`
- [ ] All `routerLink` and `router.navigate()` calls in building/unit components include `estateId`
- [ ] VIEWER cannot create, edit, or delete buildings or units (403 from backend)

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
