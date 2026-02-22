# ImmoCare — UC005 Manage Rents — Implementation Prompt

I want to implement Use Case UC005 - Manage Rents for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US021 | Set Initial Rent | MUST HAVE | 3 |
| US022 | Update Rent Amount | MUST HAVE | 3 |
| US023 | View Rent History | SHOULD HAVE | 2 |
| US024 | Track Rent Increases Over Time | COULD HAVE | 3 |
| US025 | Add Notes to Rent Changes | SHOULD HAVE | 1 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC005-manage-rents.md` — detailed flows, business rules, test scenarios
- `docs/analysis/user-stories/US021-025` — acceptance criteria
- `docs/analysis/data-model.md` — RENT_HISTORY entity definition
- `docs/analysis/data-dictionary.md` — attribute constraints and validation rules

---

## RENT_HISTORY ENTITY (to create)

```
rent_history {
  id               BIGINT PK AUTO_INCREMENT
  housing_unit_id  BIGINT FK NOT NULL → housing_unit
  monthly_rent     DECIMAL(10,2) NOT NULL   -- must be > 0, EUR
  effective_from   DATE NOT NULL
  effective_to     DATE NULL                -- NULL = current rent
  notes            VARCHAR(500) NULL
  created_at       TIMESTAMP NOT NULL
}
```

**Pattern**: Append-only. The current rent has `effective_to = NULL`. Only one record per unit may have `effective_to = NULL` at any given time.

---

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/
├── service/
├── repository/
├── model/
│   ├── entity/
│   └── dto/
├── mapper/
└── exception/

frontend/src/app/
├── core/services/
├── models/
└── features/
    └── housing-unit/   ← integrate the Rent section into HousingUnitDetailsComponent
```

---

## BACKEND

### 1. Flyway Migration

File `VXX__create_rent_history.sql`:
- Create table `rent_history` with the columns defined above
- Index on `housing_unit_id`
- Check constraint: `monthly_rent > 0`

---

### 2. `model/entity/RentHistory.java`

- `@Entity`, `@Table(name = "rent_history")`
- `@ManyToOne` → `HousingUnit`
- Fields: `id`, `housingUnit`, `monthlyRent`, `effectiveFrom`, `effectiveTo`, `notes`, `createdAt`
- `@PrePersist` to set `createdAt`

---

### 3. `model/dto/RentHistoryDTO.java` (response)

Fields:
- `id`, `housingUnitId`, `monthlyRent`, `effectiveFrom`, `effectiveTo`, `notes`, `createdAt`
- Computed `isCurrent`: `effectiveTo == null`
- Computed `durationMonths`: calculated between `effectiveFrom` and `effectiveTo` (or today if null)

---

### 4. `model/dto/SetRentRequest.java` (US021 — first rent)

Fields: `monthlyRent`, `effectiveFrom`, `notes`

Validations:
- `monthlyRent > 0`
- `effectiveFrom` not null
- `effectiveFrom` no more than 1 year in the future

---

### 5. `model/dto/UpdateRentRequest.java` (US022 — rent update)

Fields: `monthlyRent`, `effectiveFrom`, `notes`

Same validations as above, plus:
- `effectiveFrom >= effectiveFrom` of the current active rent

---

### 6. `mapper/RentHistoryMapper.java` (MapStruct)

- `RentHistory → RentHistoryDTO`
- Compute `isCurrent` and `durationMonths` in `@AfterMapping` or a default method

---

### 7. `repository/RentHistoryRepository.java`

```java
Optional<RentHistory> findByHousingUnitIdAndEffectiveToIsNull(Long housingUnitId);
List<RentHistory> findByHousingUnitIdOrderByEffectiveFromDesc(Long housingUnitId);
boolean existsByHousingUnitId(Long housingUnitId);
```

---

### 8. `service/RentHistoryService.java`

**Methods**:

- `getCurrentRent(Long unitId)` → `Optional<RentHistoryDTO>`
- `getRentHistory(Long unitId)` → `List<RentHistoryDTO>`
- `setInitialRent(Long unitId, SetRentRequest)` → `RentHistoryDTO`
  - Verifies no rent exists yet for this unit
  - Creates the record with `effectiveTo = null`
- `updateRent(Long unitId, UpdateRentRequest)` → `RentHistoryDTO`
  - Verifies a current rent exists
  - Closes the current rent: `effectiveTo = newEffectiveFrom - 1 day`
  - Creates the new rent with `effectiveTo = null`
  - Validates `effectiveFrom >= current.effectiveFrom`

**Error handling**:
- `HousingUnitNotFoundException` if unit doesn't exist
- `IllegalStateException` with a clear message if business rules are violated

---

### 9. `controller/RentHistoryController.java`

```
GET    /api/v1/housing-units/{unitId}/rents          → getRentHistory()
GET    /api/v1/housing-units/{unitId}/rents/current  → getCurrentRent()
POST   /api/v1/housing-units/{unitId}/rents          → setInitialRent() or updateRent()
                                                       (service detects which one applies)
```

> Alternative: two separate endpoints `POST /rents/initial` and `POST /rents/update` — choose whichever is cleanest.

All endpoints: `@PreAuthorize("hasRole('ADMIN')")`

---

### 10. Backend Tests

**`RentHistoryServiceTest.java`** — unit tests with Mockito:
- Set initial rent → OK
- Update rent → old rent closed, new rent created
- Error: set rent when one already exists
- Error: backdate before current rent's `effectiveFrom`
- Error: date more than 1 year in the future

**`RentHistoryControllerTest.java`** — MockMvc integration tests for all endpoints

---

## FRONTEND

### 11. `models/rent.model.ts`

```typescript
export interface RentHistory {
  id: number;
  housingUnitId: number;
  monthlyRent: number;
  effectiveFrom: string;        // ISO date string
  effectiveTo: string | null;
  notes: string | null;
  createdAt: string;
  isCurrent: boolean;
  durationMonths: number;
}

export interface SetRentRequest {
  monthlyRent: number;
  effectiveFrom: string;
  notes?: string;
}
```

---

### 12. `core/services/rent.service.ts`

- `getCurrentRent(unitId)` → `GET /api/v1/housing-units/{unitId}/rents/current`
- `getRentHistory(unitId)` → `GET /api/v1/housing-units/{unitId}/rents`
- `setOrUpdateRent(unitId, req)` → `POST /api/v1/housing-units/{unitId}/rents`

---

### 13. Integration in `HousingUnitDetailsComponent`

Add a **Rent** section to the housing unit detail page:

**"Current Rent" sub-section** (US021, US022, US023):
- If no rent: text "No rent recorded" + **"Set Rent"** button
- If current rent exists:
  - Formatted amount: `€850.00/month`
  - Effective from date: `Effective from: 2024-01-01`
  - Last change indicator (US024): `Last change: +€50.00 (+6.25%) on 2024-07-01` in green/red
  - **"Update Rent"** button
  - **"View History"** link

**Rent Form** (inline or modal):
- Amount field (€, > 0) with real-time validation
- Date picker (effective from)
- In update mode: display current rent as read-only + live change calculator (US022 AC3)
- Optional notes field with suggested templates (US025 AC5 — optional):
  - "Annual indexation", "Market adjustment", "After renovation", "Tenant negotiation"
- **"Save"** and **"Cancel"** buttons

**Rent History** (modal or sub-page, US023):
- Table sorted by date descending
- Columns: Monthly Rent | Effective From | Effective To (or "Current") | Duration | Notes
- Visual ↑ / ↓ indicators with percentage between each row (US024 AC1)
- Total increase since first rent (US024 AC2)

---

### 14. Styling & UX

- Amount displayed in green for increases, red for decreases (US022 AC4, US024 AC3)
- Warning dialog if new amount equals current rent (BR-UC005 Alternative Flow 3B)
- Loading spinner during API calls

---

## BUSINESS RULES (from UC005)

| Rule | Description |
|------|-------------|
| BR-01 | Append-only: never delete records from `rent_history` |
| BR-02 | `effective_to = NULL` means current rent; only one per unit allowed |
| BR-03 | Auto-close previous rent: `effective_to = new.effective_from - 1 day` |
| BR-04 | No overlapping rent periods |
| BR-05 | `monthly_rent > 0` |
| BR-06 | `effective_from` max 1 year in the future |
| BR-07 | Rents are indicative (target amounts), not actual tenant payments |
| BR-08 | `new.effective_from >= current.effective_from` |

---

## WHAT NOT TO DO

- Do not allow deletion or modification of existing `rent_history` records
- Do not expose a `DELETE` endpoint for rents
- Do not add dynamic currency support — EUR is fixed in Phase 1
- Do not link rents to actual tenant payments (tracked separately in BACKLOG-004)

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] Unit with no rent displays "No rent recorded" + "Set Rent" button
- [ ] First rent created with `effective_to = NULL`
- [ ] On update: old rent auto-closed, new rent created with `effective_to = NULL`
- [ ] Full rent history visible with calculated durations
- [ ] Amount ≤ 0 → error "Rent must be positive"
- [ ] Date more than 1 year in the future → validation error
- [ ] Backdate before current rent → validation error
- [ ] Change displayed as % with green/red color coding
- [ ] Notes are optional, stored, and visible in history table