# ImmoCare — UC005 Manage Rents — Implementation Prompt

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores — follow the same patterns

---

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US021 | Set Initial Rent | MUST HAVE | 3 |
| US022 | Edit a Rent Record | MUST HAVE | 3 |
| US023 | View Rent History | SHOULD HAVE | 2 |
| US024 | Track Rent Increases Over Time | COULD HAVE | 3 |
| US025 | Add Notes to Rent Changes | SHOULD HAVE | 1 |

---

## RENT_HISTORY ENTITY

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

---

## BACKEND

### 1. Flyway Migration

File `VXX__create_rent_history.sql`:
- Create table `rent_history` with columns above
- Index on `housing_unit_id`
- Check constraint: `monthly_rent > 0`

---

### 2. `model/entity/RentHistory.java`

- `@Entity`, `@Table(name = "rent_history")`
- `@ManyToOne` → `HousingUnit`
- Fields: `id`, `housingUnit`, `monthlyRent`, `effectiveFrom`, `effectiveTo`, `notes`, `createdAt`
- `@PrePersist` to set `createdAt`
- Setters required on: `monthlyRent`, `effectiveFrom`, `effectiveTo`, `notes`

---

### 3. `model/dto/RentHistoryDTO.java` (response)

```java
public record RentHistoryDTO(
    Long id,
    Long housingUnitId,
    BigDecimal monthlyRent,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,      // null = current
    String notes,
    LocalDateTime createdAt,
    boolean isCurrent,          // computed: effectiveTo == null
    long durationMonths         // computed: between effectiveFrom and effectiveTo (or today)
) {}
```

---

### 4. `model/dto/SetRentRequest.java`

```java
public record SetRentRequest(
    @NotNull @Positive BigDecimal monthlyRent,
    @NotNull LocalDate effectiveFrom,
    String notes
) {}
```

Validations:
- `monthlyRent > 0`
- `effectiveFrom` not null
- `effectiveFrom` not more than 1 year in the future (service-level)

---

### 5. `mapper/RentHistoryMapper.java` (MapStruct)

- `RentHistory → RentHistoryDTO`
- Compute `isCurrent` (`effectiveTo == null`) and `durationMonths` in default method

---

### 6. `repository/RentHistoryRepository.java`

```java
Optional<RentHistory> findByHousingUnitIdAndEffectiveToIsNull(Long housingUnitId);
List<RentHistory> findByHousingUnitIdOrderByEffectiveFromDesc(Long housingUnitId);
```

---

### 7. `service/RentHistoryService.java`

**Methods**:

- `getCurrentRent(Long unitId)` → `Optional<RentHistoryDTO>`
- `getRentHistory(Long unitId)` → `List<RentHistoryDTO>`
- `addRent(Long unitId, SetRentRequest)` → `RentHistoryDTO`
  - Inserts new record at the correct position in the timeline
  - If new record is most recent → closes previous record: `effectiveTo = newEffectiveFrom - 1 day`
  - If inserted in the middle → sets `effectiveTo = next.effectiveFrom - 1 day` and closes previous
- `updateRent(Long unitId, Long rentId, SetRentRequest)` → `RentHistoryDTO`
  - Updates monthlRent, effectiveFrom, notes on the existing record
  - Recalculates: previous record `effectiveTo = newEffectiveFrom - 1 day`
  - Recalculates: this record `effectiveTo = next.effectiveFrom - 1 day` (or null if most recent)
- `deleteRent(Long unitId, Long rentId)`
  - Deletes the record
  - Previous record (older) inherits the deleted record's `effectiveTo`
  - If deleted record was most recent → previous becomes current (`effectiveTo = null`)

**Validation** (in all write methods):
- `effectiveFrom` not more than 1 year in the future → `IllegalArgumentException`
- Record not found or belongs to different unit → `IllegalArgumentException`
- Unit not found → `HousingUnitNotFoundException`

---

### 8. `controller/RentHistoryController.java`

```
GET    /api/v1/housing-units/{unitId}/rents            → getRentHistory()
GET    /api/v1/housing-units/{unitId}/rents/current    → getCurrentRent()  [204 if none]
POST   /api/v1/housing-units/{unitId}/rents            → addRent()         [201]
PUT    /api/v1/housing-units/{unitId}/rents/{rentId}   → updateRent()      [200]
DELETE /api/v1/housing-units/{unitId}/rents/{rentId}   → deleteRent()      [204]
```

All endpoints: `@PreAuthorize("hasRole('ADMIN')")`

---

### 9. Backend Tests

**`RentHistoryServiceTest.java`** — unit tests with Mockito:
- `addRent` — first record saved with `effectiveTo = null`
- `addRent` — most recent record closes previous: `effectiveTo = newFrom - 1 day`
- `addRent` — middle insertion: sets correct `effectiveTo` on new record and previous
- `addRent` — date more than 1 year in future → `IllegalArgumentException`
- `updateRent` — fields updated, previous and next neighbours recalculated
- `updateRent` — record not found → `IllegalArgumentException`
- `deleteRent` — previous record inherits `effectiveTo` of deleted record
- `deleteRent` — deleting most recent → previous becomes current (`effectiveTo = null`)
- `deleteRent` — record not found → `IllegalArgumentException`
- `getCurrentRent` / `getRentHistory` — unit not found → `HousingUnitNotFoundException`

**`RentHistoryControllerTest.java`** — MockMvc tests:
- `GET /rents` → 200 with list
- `GET /rents/current` → 200 with current, 204 if none
- `POST /rents` → 201 on success, 400 on validation error, 409 on business rule violation
- `PUT /rents/{id}` → 200 on success, 409 on not found / business rule
- `DELETE /rents/{id}` → 204 on success, 404 if unit not found, 409 if record not found

---

## FRONTEND

### 10. `models/rent.model.ts`

```typescript
export interface RentHistory {
  id: number;
  housingUnitId: number;
  monthlyRent: number;
  effectiveFrom: string;        // ISO date string
  effectiveTo: string | null;   // null = current
  notes: string | null;
  createdAt: string;
  isCurrent: boolean;
  durationMonths: number;
}

export interface SetRentRequest {
  monthlyRent: number;
  effectiveFrom: string;
  notes?: string | null;
}

export interface RentChange {
  amount: number;
  percentage: number;
  isIncrease: boolean;
}

export function computeRentChange(from: number, to: number): RentChange {
  const amount = to - from;
  const percentage = Math.round((amount / from) * 10000) / 100;
  return { amount, percentage, isIncrease: amount >= 0 };
}
```

---

### 11. `core/services/rent.service.ts`

```typescript
getRentHistory(unitId)      → GET  /rents
getCurrentRent(unitId)      → GET  /rents/current
addRent(unitId, req)        → POST /rents
updateRent(unitId, id, req) → PUT  /rents/{id}
deleteRent(unitId, id)      → DELETE /rents/{id}
```

---

### 12. `rent-section.component.ts` (integrated in HousingUnitDetailsComponent)

**Header**:
- `<h3>Rent</h3>` + "+ Set Rent" button (only when no current rent and form closed)

**Current rent card** (when rent exists, form closed):
- Amount badge (blue, e.g. "€900")
- Effective from date
- Last change indicator ↑/↓ with amount and % vs previous
- "View History" link below the card

**Inline form panel** (add or edit mode):
- Title: "Add Rent" or "Edit Rent"
- Monthly rent input + live change preview (edit mode only, vs edited record)
- Effective from date picker
- Notes field + quick-select templates
- "Save" / "Cancel" buttons

**Inline history panel** (expandable):
- Total change summary (first → current)
- Table: Monthly Rent | From | To | Duration | Change | Notes | ✏️ 🗑️
- ✕ button to close
- Delete confirmation inline per row

**State management**:
- `editingRecord: RentHistory | null` — null = add mode, set = edit mode
- `deleteTarget: RentHistory | null` — record pending delete confirmation
- After any save/delete: call `loadCurrentRent()` + `loadHistory()` to resync

---

## BUSINESS RULES

| Rule | Description |
|------|-------------|
| BR-01 | Records can be added, edited, and deleted — adjacent periods auto-recalculated |
| BR-02 | `effective_to = NULL` means current rent; only one per unit |
| BR-03 | On add/edit: previous record `effective_to = new effectiveFrom - 1 day` |
| BR-04 | On delete: previous record inherits deleted record's `effective_to` |
| BR-05 | `monthly_rent > 0` |
| BR-06 | `effective_from` max 1 year in the future |
| BR-07 | Rents are indicative (target amounts), not actual tenant payments |

---

## WHAT NOT TO DO

- Do not use a single `POST /rents` for both create and update — use `POST` for add, `PUT /{id}` for edit
- Do not add dynamic currency support — EUR is fixed in Phase 1
- Do not link rents to actual tenant payments (tracked separately in BACKLOG-004)
- Do not show edit/delete buttons on the current rent card — only in the history table

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] Unit with no rent displays "No rent recorded" + "+ Set Rent" button
- [ ] First rent created with `effective_to = NULL`
- [ ] Adding more recent rent auto-closes previous (`effective_to = newFrom - 1 day`)
- [ ] Inserting in middle correctly sets `effective_to` on new and previous records
- [ ] Edit pre-fills form and recalculates neighbours on save
- [ ] Delete recalculates previous record's `effective_to`
- [ ] Full rent history visible inline with ✏️ 🗑️ per row
- [ ] Amount ≤ 0 → error "Rent must be positive"
- [ ] Date more than 1 year in future → validation error
- [ ] Change displayed as % with green/red color coding
- [ ] Notes optional, stored, visible in history table
- [ ] Current rent card has no edit/delete buttons

---

**Last Updated**: 2026-02-23
**Version**: 2.0
