# ImmoCare — UC008 Manage Meters — Implementation Prompt

I want to implement Use Case UC008 - Manage Meters for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users — follow the same patterns

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US036 | View meters of a housing unit | MUST HAVE | 2 |
| US037 | View meters of a building | MUST HAVE | 2 |
| US038 | Add a meter to a housing unit | MUST HAVE | 3 |
| US039 | Add a meter to a building | MUST HAVE | 3 |
| US040 | Replace a meter | MUST HAVE | 3 |
| US041 | Remove a meter | SHOULD HAVE | 2 |
| US042 | View meter history | SHOULD HAVE | 2 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC008-manage-meters.md` — flows, business rules, test scenarios
- `docs/analysis/user-stories/US036-042` — acceptance criteria per story

---

## METER ENTITY (to create)

```
meter {
  id                   BIGINT PK AUTO_INCREMENT
  type                 VARCHAR(20) NOT NULL    -- enum: WATER, GAS, ELECTRICITY
  meter_number         VARCHAR(50) NOT NULL
  ean_code             VARCHAR(18) NULL        -- required for GAS, ELECTRICITY
  installation_number  VARCHAR(50) NULL        -- required for WATER
  customer_number      VARCHAR(50) NULL        -- required for WATER on BUILDING
  owner_type           VARCHAR(20) NOT NULL    -- enum: HOUSING_UNIT, BUILDING
  owner_id             BIGINT NOT NULL
  start_date           DATE NOT NULL
  end_date             DATE NULL               -- NULL = active
  created_at           TIMESTAMP NOT NULL
}
```

**Pattern**: Append-only. Records are never modified. Closing a meter sets `end_date`. Active meter = `end_date IS NULL`. Multiple active meters of the same type per owner are allowed.

**Enums**:
- `MeterType`: `WATER`, `GAS`, `ELECTRICITY`
- `MeterOwnerType`: `HOUSING_UNIT`, `BUILDING`

**Business rules**:
- `BR-01`: `start_date` cannot be in the future
- `BR-02`: `end_date` must be ≥ `start_date`
- `BR-03`: Multiple active meters of the same type allowed per owner
- `BR-04`: A meter belongs to exactly one owner (no sharing)
- `BR-05`: `ean_code` required for GAS and ELECTRICITY
- `BR-06`: `installation_number` required for WATER
- `BR-07`: `customer_number` required for WATER on BUILDING
- `BR-08`: Replace is atomic — closes old meter and creates new in a single transaction
- `BR-09`: New start_date on replace must be ≥ current meter's start_date

---

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/
│   └── MeterController.java          ← new
├── service/
│   └── MeterService.java             ← new
├── repository/
│   └── MeterRepository.java          ← new
├── model/
│   ├── entity/
│   │   └── Meter.java                ← new
│   └── dto/
│       ├── MeterDTO.java             ← new
│       ├── AddMeterRequest.java      ← new
│       └── ReplaceMeterRequest.java  ← new
├── mapper/
│   └── MeterMapper.java              ← new
└── exception/
    └── MeterNotFoundException.java   ← new

frontend/src/app/
├── core/services/
│   └── meter.service.ts              ← new
├── models/
│   └── meter.model.ts                ← new
└── features/
    ├── housing-unit/components/
    │   └── meter-section/            ← new, integrated into HousingUnitDetailsComponent
    └── building/components/
        └── meter-section/            ← new, integrated into BuildingDetailsComponent
```

---

## BACKEND

### 1. Flyway Migration

File `VXX__create_meter.sql`:
```sql
CREATE TABLE meter (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                VARCHAR(20)  NOT NULL,
    meter_number        VARCHAR(50)  NOT NULL,
    ean_code            VARCHAR(18),
    installation_number VARCHAR(50),
    customer_number     VARCHAR(50),
    owner_type          VARCHAR(20)  NOT NULL,
    owner_id            BIGINT       NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE,
    created_at          TIMESTAMP    NOT NULL
);

CREATE INDEX idx_meter_owner ON meter (owner_type, owner_id);
CREATE INDEX idx_meter_active ON meter (owner_type, owner_id, end_date) WHERE end_date IS NULL;
```

No FK constraint on `owner_id` (polymorphic owner pattern). Integrity enforced at service level.

---

### 2. `model/entity/Meter.java`

- `@Entity`, `@Table(name = "meter")`
- No `@ManyToOne` — owner is polymorphic (`owner_type` + `owner_id`)
- Fields: `id`, `type` (String), `meterNumber`, `eanCode`, `installationNumber`, `customerNumber`, `ownerType` (String), `ownerId`, `startDate`, `endDate`, `createdAt`
- `@PrePersist` sets `createdAt`
- Setters required on: `endDate` (for closing a meter)

---

### 3. `model/dto/MeterDTO.java` (response)

```java
public record MeterDTO(
    Long id,
    String type,               // WATER, GAS, ELECTRICITY
    String meterNumber,
    String eanCode,
    String installationNumber,
    String customerNumber,
    String ownerType,          // HOUSING_UNIT, BUILDING
    Long ownerId,
    LocalDate startDate,
    LocalDate endDate,         // null = active
    LocalDateTime createdAt,
    boolean isActive,          // computed: endDate == null
    long durationMonths        // computed: startDate → endDate (or today)
) {}
```

---

### 4. `model/dto/AddMeterRequest.java`

```java
public record AddMeterRequest(
    @NotBlank String type,            // WATER, GAS, ELECTRICITY
    @NotBlank @Size(max = 50) String meterNumber,
    @Size(max = 18) String eanCode,
    @Size(max = 50) String installationNumber,
    @Size(max = 50) String customerNumber,
    @NotNull LocalDate startDate
) {}
```

Cross-field validation enforced at service level (not via Bean Validation annotations).

---

### 5. `model/dto/ReplaceMeterRequest.java`

```java
public record ReplaceMeterRequest(
    @NotBlank @Size(max = 50) String newMeterNumber,
    @Size(max = 18) String newEanCode,
    @Size(max = 50) String newInstallationNumber,
    @Size(max = 50) String newCustomerNumber,
    @NotNull LocalDate newStartDate,
    String reason                     // optional: BROKEN, END_OF_LIFE, UPGRADE, CALIBRATION_ISSUE, OTHER
) {}
```

---

### 6. `mapper/MeterMapper.java` (MapStruct)

- `Meter → MeterDTO`
- Compute `isActive` (`endDate == null`)
- Compute `durationMonths`: `ChronoUnit.MONTHS.between(startDate, endDate != null ? endDate : LocalDate.now())`

---

### 7. `repository/MeterRepository.java`

```java
// All meters for a given owner (active + closed), newest first
List<Meter> findByOwnerTypeAndOwnerIdOrderByStartDateDesc(String ownerType, Long ownerId);

// Active meters only
List<Meter> findByOwnerTypeAndOwnerIdAndEndDateIsNull(String ownerType, Long ownerId);

// Single active meter by id (for replace / remove)
Optional<Meter> findByIdAndEndDateIsNull(Long id);
```

---

### 8. `service/MeterService.java`

#### `getActiveMeters(ownerType, ownerId)`
- Validates that the owner exists (call `HousingUnitRepository` or `BuildingRepository` depending on `ownerType`)
- Returns active meters sorted by type then start_date DESC

#### `getMeterHistory(ownerType, ownerId)`
- Validates owner exists
- Returns all meters sorted by start_date DESC

#### `addMeter(ownerType, ownerId, request)`
- Validates owner exists
- Validates cross-field rules:
  - GAS or ELECTRICITY → `eanCode` required
  - WATER → `installationNumber` required
  - WATER + BUILDING → `customerNumber` required
- Validates `startDate` not in future (BR-01)
- Saves new meter with `endDate = null`

#### `replaceMeter(ownerType, ownerId, meterId, request)`
- Fetches active meter by `meterId` — throws `MeterNotFoundException` if not found or already closed
- Validates `newStartDate` ≥ current meter's `startDate` (BR-09)
- Validates `newStartDate` not in future (BR-01)
- Validates cross-field rules for new meter
- **Atomic (single transaction)**:
  1. Sets `endDate = newStartDate` on current meter
  2. Creates new meter with same `type`, `ownerType`, `ownerId` and `endDate = null`
- Returns new meter DTO

#### `removeMeter(ownerType, ownerId, meterId, endDate)`
- Fetches active meter by `meterId` — throws `MeterNotFoundException` if not found or already closed
- Validates `endDate` ≥ meter's `startDate` (BR-02)
- Validates `endDate` not in future (BR-01)
- Sets `endDate` on the meter record

---

### 9. `controller/MeterController.java`

Two sets of endpoints, sharing the same service:

```
-- Housing Unit meters --
GET    /api/v1/housing-units/{unitId}/meters               → getMeterHistory()
GET    /api/v1/housing-units/{unitId}/meters?status=active → getActiveMeters()
POST   /api/v1/housing-units/{unitId}/meters               → addMeter()         [201]
PUT    /api/v1/housing-units/{unitId}/meters/{meterId}/replace → replaceMeter() [200]
DELETE /api/v1/housing-units/{unitId}/meters/{meterId}     → removeMeter()      [204]

-- Building meters --
GET    /api/v1/buildings/{buildingId}/meters               → getMeterHistory()
GET    /api/v1/buildings/{buildingId}/meters?status=active → getActiveMeters()
POST   /api/v1/buildings/{buildingId}/meters               → addMeter()         [201]
PUT    /api/v1/buildings/{buildingId}/meters/{meterId}/replace → replaceMeter() [200]
DELETE /api/v1/buildings/{buildingId}/meters/{meterId}     → removeMeter()      [204]
```

All endpoints: `@PreAuthorize("hasRole('ADMIN')")`

The controller resolves `ownerType` from the URL path (HOUSING_UNIT vs BUILDING) and delegates to `MeterService` with the appropriate `ownerType` and `ownerId`.

**HTTP error mapping**:
- Owner not found → 404
- Meter not found or already closed → 409
- Business rule violation → 409 with descriptive message
- Validation error → 400

---

### 10. `exception/MeterNotFoundException.java`

```java
public class MeterNotFoundException extends RuntimeException {
    public MeterNotFoundException(Long id) {
        super("Meter not found or already closed: " + id);
    }
}
```

Add handler in `GlobalExceptionHandler` → 409 Conflict.

---

### 11. Backend Tests

**`MeterServiceTest.java`** — unit tests with Mockito:
- `addMeter` — GAS without eanCode → `IllegalArgumentException`
- `addMeter` — ELECTRICITY without eanCode → `IllegalArgumentException`
- `addMeter` — WATER without installationNumber → `IllegalArgumentException`
- `addMeter` — WATER on BUILDING without customerNumber → `IllegalArgumentException`
- `addMeter` — future startDate → `IllegalArgumentException`
- `addMeter` — valid GAS on HOUSING_UNIT → meter saved with endDate = null
- `addMeter` — two ELECTRICITY meters on same unit → both saved active (no conflict)
- `replaceMeter` — valid replacement → old meter closed, new meter active
- `replaceMeter` — newStartDate before current startDate → `IllegalArgumentException`
- `replaceMeter` — meter already closed → `MeterNotFoundException`
- `removeMeter` — valid removal → endDate set correctly
- `removeMeter` — endDate before startDate → `IllegalArgumentException`
- `removeMeter` — future endDate → `IllegalArgumentException`
- `getActiveMeters` / `getMeterHistory` — unknown owner → owner-specific NotFoundException

**`MeterControllerTest.java`** — MockMvc tests:
- `GET /housing-units/{id}/meters` → 200 with list
- `GET /housing-units/{id}/meters?status=active` → 200 with active only
- `POST /housing-units/{id}/meters` → 201 on valid GAS meter
- `POST /housing-units/{id}/meters` → 400 on missing eanCode for GAS
- `PUT /housing-units/{id}/meters/{mid}/replace` → 200 on valid replace
- `DELETE /housing-units/{id}/meters/{mid}` → 204 on valid removal
- Same scenarios mirrored for `/buildings/{id}/meters`

---

## FRONTEND

### 12. `models/meter.model.ts`

```typescript
export type MeterType = 'WATER' | 'GAS' | 'ELECTRICITY';
export type MeterOwnerType = 'HOUSING_UNIT' | 'BUILDING';
export type ReplacementReason = 'BROKEN' | 'END_OF_LIFE' | 'UPGRADE' | 'CALIBRATION_ISSUE' | 'OTHER';

export interface Meter {
  id: number;
  type: MeterType;
  meterNumber: string;
  eanCode: string | null;
  installationNumber: string | null;
  customerNumber: string | null;
  ownerType: MeterOwnerType;
  ownerId: number;
  startDate: string;          // ISO date YYYY-MM-DD
  endDate: string | null;     // null = active
  createdAt: string;
  isActive: boolean;
  durationMonths: number;
}

export interface AddMeterRequest {
  type: MeterType;
  meterNumber: string;
  eanCode?: string | null;
  installationNumber?: string | null;
  customerNumber?: string | null;
  startDate: string;
}

export interface ReplaceMeterRequest {
  newMeterNumber: string;
  newEanCode?: string | null;
  newInstallationNumber?: string | null;
  newCustomerNumber?: string | null;
  newStartDate: string;
  reason?: ReplacementReason;
}

export const METER_TYPE_LABELS: Record<MeterType, string> = {
  WATER: 'Water',
  GAS: 'Gas',
  ELECTRICITY: 'Electricity',
};

export const REPLACEMENT_REASON_LABELS: Record<ReplacementReason, string> = {
  BROKEN: 'Broken',
  END_OF_LIFE: 'End of life',
  UPGRADE: 'Upgrade',
  CALIBRATION_ISSUE: 'Calibration issue',
  OTHER: 'Other',
};
```

---

### 13. `core/services/meter.service.ts`

```typescript
// Housing Unit endpoints
getUnitMeterHistory(unitId)                      → GET  /housing-units/{unitId}/meters
getUnitActiveMeters(unitId)                      → GET  /housing-units/{unitId}/meters?status=active
addUnitMeter(unitId, req)                        → POST /housing-units/{unitId}/meters
replaceUnitMeter(unitId, meterId, req)           → PUT  /housing-units/{unitId}/meters/{meterId}/replace
removeUnitMeter(unitId, meterId, endDate)        → DELETE /housing-units/{unitId}/meters/{meterId}

// Building endpoints
getBuildingMeterHistory(buildingId)              → GET  /buildings/{buildingId}/meters
getBuildingActiveMeters(buildingId)              → GET  /buildings/{buildingId}/meters?status=active
addBuildingMeter(buildingId, req)                → POST /buildings/{buildingId}/meters
replaceBuildingMeter(buildingId, meterId, req)   → PUT  /buildings/{buildingId}/meters/{meterId}/replace
removeBuildingMeter(buildingId, meterId, endDate)→ DELETE /buildings/{buildingId}/meters/{meterId}
```

For `removeMeter`, pass `endDate` as a JSON body: `{ endDate: string }`.

---

### 14. `meter-section` component (shared logic, two usages)

Create **one shared component** `MeterSectionComponent` with inputs:
```typescript
@Input() ownerType: 'HOUSING_UNIT' | 'BUILDING';
@Input() ownerId!: number;
```

The component delegates to `meter.service.ts` using the appropriate methods based on `ownerType`.

**Layout**:
- Three collapsible blocks, one per meter type (WATER / GAS / ELECTRICITY)
- Each block header: type label + "Add Meter" button
- Each block body: list of active meters as cards, or "No [type] meter assigned" if empty
- Each meter card shows:
  - Meter number (bold)
  - EAN code (GAS/ELECTRICITY) or Installation number (WATER)
  - Customer number (WATER on BUILDING only)
  - Start date + duration in months
  - "Replace" button + "Remove" button
- At section bottom: "View History" link (shown only when at least 1 meter exists)

**Add Meter form** (inline, shown per type block):
- Type (pre-filled from the block, read-only)
- Meter Number (required)
- EAN Code (shown + required for GAS/ELECTRICITY)
- Installation Number (shown + required for WATER)
- Customer Number (shown + required for WATER on BUILDING)
- Start Date (required, default: today)

**Replace form** (inline, replaces meter card):
- Current meter shown read-only
- New Meter Number, new conditional fields, new Start Date
- Reason (optional dropdown)

**Remove confirmation** (inline dialog):
- Meter type + number shown
- Warning message
- End Date picker (default: today)
- Confirm / Cancel

**History panel** (inline, toggled by "View History" link):
- Table columns: Type | Meter Number | EAN / Installation Number | Customer Number (BUILDING only) | Start Date | End Date | Duration | Status badge
- Sorted by start_date DESC
- Active badge: green "Active" — Closed badge: gray "Closed"

---

### 15. Integration into existing detail pages

**`HousingUnitDetailsComponent`**:
- Add `MeterSectionComponent` to `imports`
- Add `<app-meter-section [ownerType]="'HOUSING_UNIT'" [ownerId]="unit.id">` in the template
- Remove any remaining `WaterMeterSectionComponent` references if still present

**`BuildingDetailsComponent`**:
- Add `MeterSectionComponent` to `imports`
- Add `<app-meter-section [ownerType]="'BUILDING'" [ownerId]="building.id">` in the template

---

## BUSINESS RULES SUMMARY

| Rule | Enforcement |
|---|---|
| eanCode required for GAS/ELECTRICITY | Service + frontend conditional validation |
| installationNumber required for WATER | Service + frontend conditional validation |
| customerNumber required for WATER on BUILDING | Service + frontend conditional validation |
| startDate not in future | Service (@PastOrPresent) + frontend |
| endDate ≥ startDate | Service + frontend |
| newStartDate ≥ current startDate on replace | Service + frontend |
| Replace is atomic | @Transactional on service method |
| Multiple active meters of same type allowed | No uniqueness constraint |

---

## IMPLEMENTATION ORDER

1. Flyway migration `VXX__create_meter.sql`
2. `model/entity/Meter.java`
3. `model/dto/MeterDTO.java`, `AddMeterRequest.java`, `ReplaceMeterRequest.java`
4. `mapper/MeterMapper.java`
5. `repository/MeterRepository.java`
6. `service/MeterService.java` + `MeterServiceTest.java`
7. `exception/MeterNotFoundException.java` + `GlobalExceptionHandler` update
8. `controller/MeterController.java` + `MeterControllerTest.java`
9. `models/meter.model.ts`
10. `core/services/meter.service.ts`
11. `meter-section` component (shared)
12. Integrate into `HousingUnitDetailsComponent`
13. Integrate into `BuildingDetailsComponent`

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] GET `/housing-units/{id}/meters` returns full history sorted by start_date DESC
- [ ] GET `/housing-units/{id}/meters?status=active` returns only active meters
- [ ] POST `/housing-units/{id}/meters` — GAS without eanCode → 400
- [ ] POST `/housing-units/{id}/meters` — WATER on BUILDING without customerNumber → 400
- [ ] POST `/housing-units/{id}/meters` — valid → 201, meter active
- [ ] POST — two ELECTRICITY meters on same unit → both saved (no conflict)
- [ ] PUT `/housing-units/{id}/meters/{mid}/replace` — valid → 200, old closed, new active
- [ ] PUT replace — newStartDate before current startDate → 409
- [ ] DELETE `/housing-units/{id}/meters/{mid}` — valid → 204, endDate set
- [ ] DELETE — endDate before startDate → 409
- [ ] All above mirrored for `/buildings/{id}/meters`
- [ ] Angular meter section renders active meters grouped by type
- [ ] Angular add form shows correct conditional fields per type
- [ ] Angular add form for WATER on BUILDING shows customerNumber field
- [ ] Angular replace form shows current meter read-only + new meter fields
- [ ] Angular history table shows all records with correct status badges
- [ ] MeterSectionComponent works identically for HOUSING_UNIT and BUILDING
- [ ] All acceptance criteria from US036 to US042 are met

---

**Last Updated**: 2025-02-23
**Branch**: `develop`
**Status**: Ready for Implementation
