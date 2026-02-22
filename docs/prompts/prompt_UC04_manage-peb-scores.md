# ImmoCare — UC004 Manage PEB Scores — Implementation Prompt

I want to implement Use Case UC004 - Manage PEB Scores for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US017 | Add PEB Score to Housing Unit | MUST HAVE | 3 |
| US018 | View PEB Score History | SHOULD HAVE | 2 |
| US019 | Check PEB Certificate Validity | SHOULD HAVE | 2 |
| US020 | Track PEB Score Improvements | COULD HAVE | 3 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC004-manage-peb-scores.md` — detailed flows, business rules, test scenarios
- `docs/analysis/user-stories/US017-020` — acceptance criteria per story
- `docs/analysis/data-model.md` — PEB_SCORE_HISTORY entity definition
- `docs/analysis/data-dictionary.md` — attribute constraints and validation rules

---

## PEB_SCORE_HISTORY ENTITY (to create)

```
peb_score_history {
  id               BIGINT PK AUTO_INCREMENT
  housing_unit_id  BIGINT FK NOT NULL → housing_unit
  peb_score        VARCHAR(10) NOT NULL   -- enum: A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G
  score_date       DATE NOT NULL
  certificate_number VARCHAR(50) NULL
  valid_until      DATE NULL
  created_at       TIMESTAMP NOT NULL
}
```

**Pattern**: Append-only. No updates, no deletes. Current score = record with most recent `score_date`. Status is computed (not stored): `CURRENT`, `HISTORICAL`, or `EXPIRED` (when `valid_until` < today).

**PEB Score Enum** (Java: `PebScore`):
`A_PLUS_PLUS` (A++), `A_PLUS` (A+), `A`, `B`, `C`, `D`, `E`, `F`, `G`

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
    └── housing-unit/   ← integrate the PEB section into HousingUnitDetailsComponent
```

---

## BACKEND

### 1. Flyway Migration

File `VXX__create_peb_score_history.sql`:
- Create table `peb_score_history` with the columns defined above
- Index on `(housing_unit_id, score_date DESC)` for current score retrieval
- Index on `(housing_unit_id, valid_until)` for validity checks
- No check constraints needed (enum enforced at application level)

---

### 2. `model/entity/PebScoreHistory.java`

- `@Entity`, `@Table(name = "peb_score_history")`
- `@ManyToOne` → `HousingUnit`
- Fields: `id`, `housingUnit`, `pebScore` (String), `scoreDate`, `certificateNumber`, `validUntil`, `createdAt`
- `@PrePersist` to set `createdAt`
- No `updatedAt` (append-only entity)

---

### 3. `model/dto/PebScoreDTO.java` (response)

Fields:
- `id`, `housingUnitId`, `pebScore`, `scoreDate`, `certificateNumber`, `validUntil`, `createdAt`
- Computed `status`: `"CURRENT"` | `"HISTORICAL"` | `"EXPIRED"`
  - `EXPIRED`: `validUntil != null && validUntil < today`
  - `CURRENT`: record with the most recent `scoreDate` for the unit (and not expired)
  - `HISTORICAL`: any other
- Computed `expiryWarning`: `"EXPIRED"` | `"EXPIRING_SOON"` | `"VALID"` | `"NO_DATE"`
  - `EXPIRING_SOON`: `validUntil` within 3 months from today

---

### 4. `model/dto/CreatePebScoreRequest.java`

Fields with Bean Validation:
- `housingUnitId` — `@NotNull`
- `pebScore` — `@NotBlank`, must be valid enum value
- `scoreDate` — `@NotNull`, `@PastOrPresent`
- `certificateNumber` — optional, `@Size(max = 50)`, alphanumeric + hyphens
- `validUntil` — optional, must be after `scoreDate` (custom cross-field validation)

---

### 5. `mapper/PebScoreMapper.java` (MapStruct)

- `PebScoreHistory → PebScoreDTO`
- Computed fields (`status`, `expiryWarning`) set in service layer, not mapper

---

### 6. `repository/PebScoreRepository.java`

Spring Data JPA — no `@Query` needed except for current score:

```java
List<PebScoreHistory> findByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);

Optional<PebScoreHistory> findFirstByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);
```

---

### 7. `service/PebScoreService.java`

Methods:
- `addScore(CreatePebScoreRequest request)` → validates, saves, returns `PebScoreDTO`
- `getHistory(Long housingUnitId)` → returns list of `PebScoreDTO`, sorted newest first, with computed status
- `getCurrentScore(Long housingUnitId)` → returns single `PebScoreDTO` or empty
- `getImprovementSummary(Long housingUnitId)` → returns `PebImprovementDTO` (for US020)

**Business rules to enforce**:
- `BR-UC004-01`: Append-only — no update or delete methods
- `BR-UC004-02`: Current score = most recent `scoreDate`
- `BR-UC004-03`: `scoreDate` cannot be in the future
- `BR-UC004-04`: `validUntil` must be after `scoreDate` if provided
- `BR-UC004-06`: `pebScore` must be a valid enum value

---

### 8. `controller/PebScoreController.java`

```
POST   /api/v1/housing-units/{unitId}/peb-scores          → addScore
GET    /api/v1/housing-units/{unitId}/peb-scores          → getHistory
GET    /api/v1/housing-units/{unitId}/peb-scores/current  → getCurrentScore
GET    /api/v1/housing-units/{unitId}/peb-scores/improvements → getImprovementSummary (US020)
```

All endpoints require `ROLE_ADMIN`.

---

### 9. `model/dto/PebImprovementDTO.java` (US020)

Fields:
- `firstScore` (PebScore), `firstScoreDate`
- `currentScore` (PebScore), `currentScoreDate`
- `gradesImproved` (int, positive = improvement, negative = degradation, 0 = no change)
- `yearsCovered` (int)
- `history` — list of `PebScoreStepDTO` with `{ fromScore, toScore, direction: IMPROVED | DEGRADED | UNCHANGED, date }`

---

### 10. Exception handling

Add to `GlobalExceptionHandler`:
- `HousingUnitNotFoundException` (already exists — reuse)
- `InvalidPebScoreDateException` → HTTP 400
- `InvalidValidityPeriodException` → HTTP 400

---

## FRONTEND

### 11. `models/peb-score.model.ts`

```typescript
export type PebScore = 'A_PLUS_PLUS' | 'A_PLUS' | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G';
export type PebStatus = 'CURRENT' | 'HISTORICAL' | 'EXPIRED';
export type ExpiryWarning = 'EXPIRED' | 'EXPIRING_SOON' | 'VALID' | 'NO_DATE';

export interface PebScoreDTO { ... }
export interface CreatePebScoreRequest { ... }
export interface PebImprovementDTO { ... }

export const PEB_SCORE_DISPLAY: Record<PebScore, { label: string; color: string }> = {
  A_PLUS_PLUS: { label: 'A++', color: '#1a7a1a' },
  A_PLUS:      { label: 'A+',  color: '#2d9e2d' },
  A:           { label: 'A',   color: '#4caf50' },
  B:           { label: 'B',   color: '#8bc34a' },
  C:           { label: 'C',   color: '#ffeb3b' },
  D:           { label: 'D',   color: '#ff9800' },
  E:           { label: 'E',   color: '#f44336' },
  F:           { label: 'F',   color: '#d32f2f' },
  G:           { label: 'G',   color: '#7b1fa2' },
};
```

---

### 12. `core/services/peb-score.service.ts`

Methods calling REST API:
- `addScore(unitId, request)` → `POST /api/v1/housing-units/{unitId}/peb-scores`
- `getHistory(unitId)` → `GET /api/v1/housing-units/{unitId}/peb-scores`
- `getCurrentScore(unitId)` → `GET /api/v1/housing-units/{unitId}/peb-scores/current`
- `getImprovements(unitId)` → `GET /api/v1/housing-units/{unitId}/peb-scores/improvements`

---

### 13. `features/housing-unit/components/peb-section/`

Replace the existing placeholder in `HousingUnitDetailsComponent` with a real `PebSectionComponent`.

**`peb-section.component.html` layout**:

```
┌─────────────────────────────────────────────┐
│ Energy Performance (PEB)      [Add PEB Score]│
│                                              │
│  Current Score: [B badge]  2024-01-15        │
│  Valid until: 2034-01-15   [VALID / ⚠ badge] │
│  Certificate: PEB-2024-123456                │
│                              [View History]  │
└─────────────────────────────────────────────┘
```

- If no score: show "No PEB score recorded"
- Color-coded score badge using `PEB_SCORE_DISPLAY`
- Expiry warning badge: green / orange / red

**Add PEB Score form** (modal or inline):
- Score dropdown with color indicators
- Score date (date picker, max = today)
- Certificate number (optional)
- Valid until (optional date picker)
- Save / Cancel buttons
- Real-time validation feedback

---

### 14. `features/housing-unit/components/peb-history/`

**`peb-history.component.html`** (modal or page):

Table columns:
| Score | Score Date | Certificate | Valid Until | Status | Improvement |

- Color-coded badges per score
- Status badge: Current (blue) / Historical (grey) / Expired (red)
- Improvement indicator per row (US020): ↑ green / ↓ red / − grey
- Summary at top: "Improved by X grades over Y years" (US020)
- Sort by date (default: newest first)

---

## ACCEPTANCE CRITERIA SUMMARY

### US017 — Add PEB Score
- AC1: Form displayed on "Add PEB Score" click
- AC2: Dropdown shows all 9 scores with color coding
- AC3: First score saved and displayed as current
- AC4: Newer score replaces current, old becomes historical
- AC5: All fields (score, date, certificate, valid until) stored correctly
- AC6: Error if score date empty
- AC7: Error if score date in future

### US018 — View PEB Score History
- AC1: Current score badge shown in unit details (score + date + "View History" link)
- AC2: History modal/table shows all scores sorted newest first
- AC3: Table columns: Score, Score Date, Certificate Number, Valid Until, Status

### US019 — Check PEB Certificate Validity
- AC1: "Expired" warning in red when `valid_until` < today
- AC2: "Expires soon" warning in orange when `valid_until` < today + 3 months
- AC3: No warning shown when certificate is valid
- AC4: "Validity period not specified" when no `valid_until` date

### US020 — Track PEB Score Improvements
- AC1: ↑ "Improved" indicator (green) when score got better between consecutive entries
- AC2: ↓ "Degraded" indicator (red) when score got worse
- AC3: − "No change" indicator (grey) for equal consecutive scores
- AC4: Summary shows total grades improved and years covered

---

## BUSINESS RULES

- `BR-UC004-01`: Append-only — no update or delete of PEB records
- `BR-UC004-02`: Current score = record with most recent `score_date`
- `BR-UC004-03`: `score_date` cannot be in the future
- `BR-UC004-04`: `valid_until` must be after `score_date` if provided
- `BR-UC004-05`: Multiple scores on same date allowed (correction use case)
- `BR-UC004-06`: Only values A++ to G are valid
- `BR-UC004-07`: `certificate_number` is optional

---

## PERFORMANCE REQUIREMENTS

- PEB section loads with unit details: < 500ms
- Add score: < 500ms
- History loads: < 500ms

---

## DEFINITION OF DONE

- [ ] Flyway migration created and tested
- [ ] Backend entity, DTO, mapper, repository, service, controller implemented
- [ ] Improvement summary endpoint implemented (US020)
- [ ] Unit tests for service layer (including computed status and expiry logic)
- [ ] Integration tests for controller layer
- [ ] Angular service and models created
- [ ] PEB section integrated into `HousingUnitDetailsComponent` (placeholder removed)
- [ ] Add PEB Score form implemented with validation (US017)
- [ ] PEB history modal/table implemented (US018)
- [ ] Expiry warning badges implemented (US019)
- [ ] Improvement indicators implemented (US020)
- [ ] All acceptance criteria manually verified
- [ ] Code reviewed

---

**Last Updated**: 2026-02-22
**Branch**: `develop`
**Status**: Ready for Implementation