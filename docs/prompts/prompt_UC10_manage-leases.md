# ImmoCare — UC010 Manage Leases — Implementation Prompt

I want to implement Use Case UC010 - Manage Leases for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 4.x (backend) + Angular 19+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons — follow the same patterns
- **Prerequisite**: UC009 (Manage Persons) must be fully implemented before starting this UC

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US049 | View lease for housing unit | MUST HAVE | 2 |
| US050 | Create lease (draft) | MUST HAVE | 8 |
| US051 | Activate lease | MUST HAVE | 2 |
| US052 | Edit lease | MUST HAVE | 3 |
| US053 | Finish lease | MUST HAVE | 2 |
| US054 | Cancel lease | MUST HAVE | 2 |
| US055 | Record indexation | MUST HAVE | 3 |
| US056 | View indexation history | MUST HAVE | 2 |
| US057 | Add tenant to lease | MUST HAVE | 3 |
| US058 | Remove tenant from lease | MUST HAVE | 2 |
| US059 | View lease alerts | SHOULD HAVE | 3 |

---

## REFERENCE DOCUMENTS

- `docs/analysis/use-cases/UC010-manage-leases.md` — flows, business rules, test scenarios
- `docs/analysis/user-stories/US049-059` — acceptance criteria per story
- `docs/analysis/data-model.md` — data model reference

---

## NEW ENTITIES

### LEASE

```sql
CREATE TABLE lease (
    id                            BIGSERIAL     PRIMARY KEY,
    housing_unit_id               BIGINT        NOT NULL REFERENCES housing_unit(id) ON DELETE RESTRICT,
    status                        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    signature_date                DATE          NOT NULL,
    start_date                    DATE          NOT NULL,
    end_date                      DATE          NOT NULL,      -- calculated, stored
    lease_type                    VARCHAR(30)   NOT NULL,
    duration_months               INTEGER       NOT NULL CHECK (duration_months > 0),
    notice_period_months          INTEGER       NOT NULL CHECK (notice_period_months > 0),
    indexation_notice_days        INTEGER       NOT NULL DEFAULT 30,
    indexation_anniversary_month  INTEGER       NULL CHECK (indexation_anniversary_month BETWEEN 1 AND 12),
    monthly_rent                  NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    monthly_charges               NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monthly_charges >= 0),
    charges_type                  VARCHAR(20)   NOT NULL DEFAULT 'FORFAIT',
    charges_description           TEXT          NULL,
    base_index_value              NUMERIC(8,4)  NULL,
    base_index_month              DATE          NULL,
    registration_spf              VARCHAR(50)   NULL,
    registration_region           VARCHAR(50)   NULL,
    deposit_amount                NUMERIC(10,2) NULL CHECK (deposit_amount >= 0),
    deposit_type                  VARCHAR(30)   NULL,
    deposit_reference             VARCHAR(100)  NULL,
    tenant_insurance_confirmed    BOOLEAN       NOT NULL DEFAULT FALSE,
    tenant_insurance_reference    VARCHAR(100)  NULL,
    tenant_insurance_expiry       DATE          NULL,
    created_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_lease_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'FINISHED', 'CANCELLED')),
    CONSTRAINT chk_lease_type
        CHECK (lease_type IN ('SHORT_TERM','MAIN_RESIDENCE_3Y','MAIN_RESIDENCE_6Y',
                              'MAIN_RESIDENCE_9Y','STUDENT','GLIDING','COMMERCIAL')),
    CONSTRAINT chk_charges_type
        CHECK (charges_type IN ('FORFAIT','PROVISION')),
    CONSTRAINT chk_deposit_type
        CHECK (deposit_type IN ('BLOCKED_ACCOUNT','BANK_GUARANTEE','CPAS','INSURANCE') OR deposit_type IS NULL),
    CONSTRAINT chk_end_after_start
        CHECK (end_date >= start_date)
);

CREATE INDEX idx_lease_housing_unit ON lease(housing_unit_id);
CREATE INDEX idx_lease_status       ON lease(status);
-- Partial unique index: only one ACTIVE or DRAFT lease per unit at a time
CREATE UNIQUE INDEX uq_lease_active_per_unit
    ON lease(housing_unit_id)
    WHERE status IN ('ACTIVE', 'DRAFT');
```

### LEASE_TENANT

```sql
CREATE TABLE lease_tenant (
    lease_id    BIGINT     NOT NULL REFERENCES lease(id) ON DELETE CASCADE,
    person_id   BIGINT     NOT NULL REFERENCES person(id) ON DELETE RESTRICT,
    tenant_role VARCHAR(20) NOT NULL,
    PRIMARY KEY (lease_id, person_id),
    CONSTRAINT chk_tenant_role CHECK (tenant_role IN ('PRIMARY','CO_TENANT','GUARANTOR'))
);

CREATE INDEX idx_lease_tenant_person ON lease_tenant(person_id);
```

### LEASE_INDEXATION_HISTORY

```sql
CREATE TABLE lease_indexation_history (
    id                     BIGSERIAL      PRIMARY KEY,
    lease_id               BIGINT         NOT NULL REFERENCES lease(id) ON DELETE CASCADE,
    calculation_date       DATE           NOT NULL,
    application_date       DATE           NOT NULL,
    old_rent               NUMERIC(10,2)  NOT NULL,
    new_index_value        NUMERIC(8,4)   NOT NULL,
    new_index_month        DATE           NOT NULL,
    applied_rent           NUMERIC(10,2)  NOT NULL CHECK (applied_rent > 0),
    notification_sent_date DATE           NULL,
    notes                  TEXT           NULL,
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_indexation_lease ON lease_indexation_history(lease_id, application_date DESC);
```

Put all three in a single migration: `V011__create_lease_tables.sql`

---

## PROJECT STRUCTURE

```
backend/src/main/java/com/immocare/
├── controller/
│   └── LeaseController.java                    ← new
├── service/
│   └── LeaseService.java                       ← new
├── repository/
│   ├── LeaseRepository.java                    ← new
│   ├── LeaseTenantRepository.java              ← new
│   └── LeaseIndexationRepository.java          ← new
├── model/
│   ├── entity/
│   │   ├── Lease.java                          ← new
│   │   ├── LeaseTenant.java                    ← new (composite PK)
│   │   └── LeaseIndexationHistory.java         ← new
│   └── dto/
│       ├── LeaseDTO.java                       ← new (full details)
│       ├── LeaseSummaryDTO.java                ← new (for unit details section)
│       ├── CreateLeaseRequest.java             ← new
│       ├── UpdateLeaseRequest.java             ← new
│       ├── ChangeLeaseStatusRequest.java       ← new
│       ├── AddTenantRequest.java               ← new
│       ├── RecordIndexationRequest.java        ← new
│       └── LeaseAlertDTO.java                  ← new
├── mapper/
│   ├── LeaseMapper.java                        ← new (MapStruct)
│   └── LeaseIndexationMapper.java              ← new
└── exception/
    ├── LeaseNotFoundException.java             ← new
    ├── LeaseOverlapException.java              ← new
    └── LeaseNotEditableException.java          ← new

frontend/src/app/
├── core/services/
│   └── lease.service.ts                        ← new
├── models/
│   └── lease.model.ts                          ← new
└── features/
    └── lease/                                  ← new feature module
        ├── lease.module.ts
        ├── lease-routing.module.ts
        └── components/
            ├── lease-section/                  ← embedded in housing-unit-details
            ├── lease-list/                     ← full history page
            ├── lease-form/                     ← create + edit (multi-section)
            ├── lease-details/                  ← full details page
            ├── lease-status-actions/           ← activate/finish/cancel buttons
            ├── indexation-section/             ← indexation history + record form
            └── tenant-section/                 ← tenant management
```

---

## BACKEND

### 1. Enums (Java)

```java
public enum LeaseStatus   { DRAFT, ACTIVE, FINISHED, CANCELLED }
public enum LeaseType     { SHORT_TERM, MAIN_RESIDENCE_3Y, MAIN_RESIDENCE_6Y,
                            MAIN_RESIDENCE_9Y, STUDENT, GLIDING, COMMERCIAL }
public enum ChargesType   { FORFAIT, PROVISION }
public enum DepositType   { BLOCKED_ACCOUNT, BANK_GUARANTEE, CPAS, INSURANCE }
public enum TenantRole    { PRIMARY, CO_TENANT, GUARANTOR }
```

---

### 2. `model/entity/Lease.java`

- `@Entity`, `@Table(name = "lease")`
- `@ManyToOne(fetch=LAZY)` → `HousingUnit`
- `@OneToMany(mappedBy="lease", cascade=ALL, orphanRemoval=true)` → `List<LeaseTenant>`
- `@OneToMany(mappedBy="lease")` → `List<LeaseIndexationHistory>` (append-only, no cascade)
- All fields as per schema
- `@PrePersist` sets `createdAt`; `@PreUpdate` sets `updatedAt`
- `end_date` is stored (calculated and set by service on create/update, never set by client)

---

### 3. `model/entity/LeaseTenant.java`

```java
@Entity
@Table(name = "lease_tenant")
@IdClass(LeaseTenantId.class)
public class LeaseTenant {
    @Id @ManyToOne Lease lease;
    @Id @ManyToOne Person person;
    @Enumerated(STRING) TenantRole tenantRole;
}

public class LeaseTenantId implements Serializable {
    Long lease;
    Long person;
}
```

---

### 4. `model/entity/LeaseIndexationHistory.java`

- `@Entity`, `@Table(name = "lease_indexation_history")`
- `@ManyToOne` → `Lease`
- All fields, `@PrePersist` only (append-only)

---

### 5. DTOs

#### `LeaseDTO.java` (full response)
```java
{
  Long id;
  Long housingUnitId;
  String housingUnitNumber;   // convenience
  String buildingName;         // convenience
  LeaseStatus status;
  LocalDate signatureDate;
  LocalDate startDate;
  LocalDate endDate;
  LeaseType leaseType;
  int durationMonths;
  int noticePeriodMonths;
  int indexationNoticeDays;
  Integer indexationAnniversaryMonth;
  BigDecimal monthlyRent;
  BigDecimal monthlyCharges;
  ChargesType chargesType;
  String chargesDescription;
  BigDecimal baseIndexValue;
  LocalDate baseIndexMonth;
  String registrationSpf;
  String registrationRegion;
  BigDecimal depositAmount;
  DepositType depositType;
  String depositReference;
  boolean tenantInsuranceConfirmed;
  String tenantInsuranceReference;
  LocalDate tenantInsuranceExpiry;
  List<LeaseTenantDTO> tenants;
  List<LeaseIndexationDTO> indexations;
  // Computed alerts:
  boolean indexationAlertActive;
  LocalDate indexationAlertDate;
  boolean endNoticeAlertActive;
  LocalDate endNoticeAlertDate;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
```

#### `LeaseSummaryDTO.java` (for housing-unit details section)
```java
{
  Long id;
  LeaseStatus status;
  LocalDate startDate;
  LocalDate endDate;
  BigDecimal monthlyRent;
  BigDecimal monthlyCharges;
  List<String> tenantNames;  // "Dupont Jean, Martin Marie"
  boolean indexationAlertActive;
  boolean endNoticeAlertActive;
}
```

#### `CreateLeaseRequest.java` / `UpdateLeaseRequest.java`
All lease fields except: `id`, `status`, `endDate` (calculated), `createdAt`, `updatedAt`.
Include `List<AddTenantRequest> tenants` for initial tenant assignment on create.

#### `ChangeLeaseStatusRequest.java`
```java
{
  @NotNull LeaseStatus targetStatus;
  LocalDate effectiveDate;  // required for FINISHED and CANCELLED
  String notes;
}
```

#### `AddTenantRequest.java`
```java
{ @NotNull Long personId; @NotNull TenantRole role; }
```

#### `RecordIndexationRequest.java`
```java
{
  @NotNull LocalDate applicationDate;
  @NotNull @Positive BigDecimal newIndexValue;
  @NotNull LocalDate newIndexMonth;
  @NotNull @Positive BigDecimal appliedRent;
  LocalDate notificationSentDate;
  String notes;
}
```

#### `LeaseAlertDTO.java`
```java
{
  Long leaseId;
  Long housingUnitId;
  String housingUnitNumber;
  String buildingName;
  String alertType;   // "INDEXATION" or "END_NOTICE"
  LocalDate deadline;
  List<String> tenantNames;
}
```

---

### 6. `LeaseService.java`

#### Key methods:

**`createLease(Long unitId, CreateLeaseRequest request)`**
1. Check no ACTIVE or DRAFT lease exists for unit → throw `LeaseOverlapException` if so
2. Calculate `endDate = startDate + durationMonths` (use `start_date.plusMonths(duration_months)`)
3. Validate at least one PRIMARY tenant in request.tenants
4. Persist Lease (status=DRAFT or ACTIVE per request)
5. Persist LeaseTenant records
6. Return `LeaseDTO`

**`updateLease(Long leaseId, UpdateLeaseRequest request)`**
1. Check lease is DRAFT or ACTIVE → throw `LeaseNotEditableException` if FINISHED/CANCELLED
2. Recalculate `endDate` from new startDate + durationMonths
3. Validate tenant list still has one PRIMARY
4. Update and persist

**`changeStatus(Long leaseId, ChangeLeaseStatusRequest request)`**
Allowed transitions:
- DRAFT → ACTIVE: check no other ACTIVE/DRAFT for same unit (excluding self after recheck)
- DRAFT → CANCELLED
- ACTIVE → FINISHED
- ACTIVE → CANCELLED
Any other transition: throw `IllegalStateException`

**`recordIndexation(Long leaseId, RecordIndexationRequest request)`**
1. Check lease is ACTIVE
2. Capture current `lease.monthlyRent` as `oldRent`
3. Create `LeaseIndexationHistory` record
4. Update `lease.monthlyRent` to `request.appliedRent`
5. Persist both in one transaction

**`addTenant(Long leaseId, AddTenantRequest request)`**
1. Check lease is DRAFT or ACTIVE
2. Check person not already on lease → throw if duplicate
3. Persist `LeaseTenant`

**`removeTenant(Long leaseId, Long personId)`**
1. Check remaining PRIMARY count > 1 if removing a PRIMARY → throw if not
2. Delete `LeaseTenant`

**`getAlerts()`** → `List<LeaseAlertDTO>`
Query all ACTIVE leases where:
- `indexationAnniversaryMonth IS NOT NULL` AND today >= (this year's anniversary date − indexationNoticeDays) AND no indexation recorded this anniversary year
- OR today >= (endDate − noticePeriodMonths months)

Return sorted by deadline ASC.

#### Alert computation helpers (private):
```java
private boolean isIndexationAlertActive(Lease lease) {
    if (lease.getIndexationAnniversaryMonth() == null) return false;
    LocalDate anniversary = LocalDate.of(LocalDate.now().getYear(),
                                         lease.getIndexationAnniversaryMonth(), 1);
    LocalDate alertDate = anniversary.minusDays(lease.getIndexationNoticeDays());
    boolean alreadyIndexedThisYear = leaseIndexationRepository
        .existsByLeaseIdAndApplicationDateBetween(
            lease.getId(), anniversary.withDayOfMonth(1), anniversary.plusMonths(1));
    return !alreadyIndexedThisYear && !LocalDate.now().isBefore(alertDate);
}

private boolean isEndNoticeAlertActive(Lease lease) {
    LocalDate noticeDeadline = lease.getEndDate()
        .minusMonths(lease.getNoticePeriodMonths());
    return !LocalDate.now().isBefore(noticeDeadline);
}
```

---

### 7. `LeaseRepository.java`

```java
Optional<Lease> findByHousingUnitIdAndStatusIn(Long unitId, List<LeaseStatus> statuses);
List<Lease> findByHousingUnitId(Long unitId);
boolean existsByHousingUnitIdAndStatusIn(Long unitId, List<LeaseStatus> statuses);
// For alerts:
List<Lease> findByStatus(LeaseStatus status);
```

---

### 8. `LeaseController.java`

```
GET    /api/v1/housing-units/{unitId}/leases          → list all leases for unit
GET    /api/v1/housing-units/{unitId}/leases/active   → active lease summary (204 if none)
GET    /api/v1/leases/{id}                            → full lease details
POST   /api/v1/housing-units/{unitId}/leases          → create lease
PUT    /api/v1/leases/{id}                            → update lease
PATCH  /api/v1/leases/{id}/status                     → change status
GET    /api/v1/leases/{id}/indexations                → indexation history
POST   /api/v1/leases/{id}/indexations                → record indexation
GET    /api/v1/leases/{id}/tenants                    → list tenants
POST   /api/v1/leases/{id}/tenants                    → add tenant
DELETE /api/v1/leases/{id}/tenants/{personId}         → remove tenant
GET    /api/v1/leases/alerts                          → all active alerts
```

---

### 9. Default notice periods by lease type

Implement as a static map in `LeaseService` or a constant class:

```java
public static final Map<LeaseType, Integer> DEFAULT_NOTICE_MONTHS = Map.of(
    SHORT_TERM,          1,
    MAIN_RESIDENCE_3Y,   3,
    MAIN_RESIDENCE_6Y,   3,
    MAIN_RESIDENCE_9Y,   3,
    STUDENT,             1,
    GLIDING,             1,
    COMMERCIAL,          6
);
```

---

### 10. Exception handling — update `GlobalExceptionHandler`

- `LeaseNotFoundException` → 404
- `LeaseOverlapException` → 409 with body:
```json
{
  "error": "LEASE_OVERLAP",
  "message": "An active or draft lease already exists for this unit.",
  "existingLeaseId": 42,
  "existingLeaseStatus": "ACTIVE",
  "existingLeaseStart": "2024-01-01",
  "existingLeaseEnd": "2027-01-01"
}
```
- `LeaseNotEditableException` → 422 "Lease with status FINISHED cannot be edited"

---

### 11. Tests

- `LeaseServiceTest.java`:
  - Create lease (happy path, overlap blocked, no primary tenant)
  - Calculate end_date correctly
  - Status transitions (valid + invalid)
  - Record indexation (rent updated + history created)
  - Alert computation (indexation due, end notice)
  - Add/remove tenant (success + last PRIMARY blocked)
- `LeaseControllerTest.java`: all endpoints, 409 overlap, 204 no active lease

---

## FRONTEND

### 12. `lease.model.ts`

```typescript
export type LeaseStatus = 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED';
export type LeaseType = 'SHORT_TERM' | 'MAIN_RESIDENCE_3Y' | 'MAIN_RESIDENCE_6Y'
                      | 'MAIN_RESIDENCE_9Y' | 'STUDENT' | 'GLIDING' | 'COMMERCIAL';
export type ChargesType = 'FORFAIT' | 'PROVISION';
export type DepositType = 'BLOCKED_ACCOUNT' | 'BANK_GUARANTEE' | 'CPAS' | 'INSURANCE';
export type TenantRole  = 'PRIMARY' | 'CO_TENANT' | 'GUARANTOR';

export interface LeaseSummary { ... }  // matches LeaseSummaryDTO
export interface Lease { ... }         // matches LeaseDTO
export interface LeaseTenant { ... }   // personId, fullName, role
export interface LeaseIndexation { ... }
export interface LeaseAlert { ... }
```

---

### 13. `lease.service.ts`

Methods:
- `getActive(unitId)` → `Observable<LeaseSummary | null>`
- `getAllForUnit(unitId)` → `Observable<LeaseSummary[]>`
- `getById(id)` → `Observable<Lease>`
- `create(unitId, request)` → `Observable<Lease>`
- `update(id, request)` → `Observable<Lease>`
- `changeStatus(id, request)` → `Observable<Lease>`
- `getIndexations(id)` → `Observable<LeaseIndexation[]>`
- `recordIndexation(id, request)` → `Observable<LeaseIndexation>`
- `getTenants(id)` → `Observable<LeaseTenant[]>`
- `addTenant(id, request)` → `Observable<LeaseTenant>`
- `removeTenant(id, personId)` → `Observable<void>`
- `getAlerts()` → `Observable<LeaseAlert[]>`

---

### 14. `features/lease/` components

#### `lease-section` (embedded in housing-unit-details)

Layout when no active lease:
```
┌────────────────────────────────────────────┐
│  Lease                      [Create Lease] │
│  No active lease                           │
└────────────────────────────────────────────┘
```

Layout with active lease:
```
┌────────────────────────────────────────────┐
│  Lease                    [View] [Edit]    │
│  ⚠ Indexation due — anniversary 2026-04-01 │
│  Status: ACTIVE   Type: Résidence 9 ans    │
│  Tenants: Dupont Jean (primary)            │
│  Rent: 850€ + 50€ charges (forfait)        │
│  Period: 2024-01-01 → 2033-01-01           │
│  [View all leases]                         │
└────────────────────────────────────────────┘
```

Alert banners: orange background, warning icon, dismissible per session.

#### `lease-form` (create + edit)

Multi-section form with Angular Reactive Forms:
- **Section 1 — General**: signature_date*, start_date*, lease_type* (auto-fills notice_period_months), duration_months*, end_date (read-only computed), notice_period_months (editable)
- **Section 2 — Financial**: monthly_rent*, monthly_charges*, charges_type* (FORFAIT/PROVISION), charges_description
- **Section 3 — Indexation**: base_index_value, base_index_month, indexation_anniversary_month, indexation_notice_days
- **Section 4 — Registration**: registration_spf, registration_region
- **Section 5 — Guarantee**: deposit_amount, deposit_type, deposit_reference
- **Section 6 — Insurance**: tenant_insurance_confirmed (toggle), tenant_insurance_reference (shown if confirmed), tenant_insurance_expiry
- **Section 7 — Tenants**: list of added tenants, PersonPicker to add new, role selector (PRIMARY/CO_TENANT/GUARANTOR)

Bottom actions: "Save as Draft" | "Save and Activate" | "Cancel"

On lease_type change → auto-populate `notice_period_months` from DEFAULT_NOTICE_MONTHS map (client-side).
On start_date or duration_months change → auto-compute end_date (client-side).

#### `lease-details` (full page)

- Breadcrumb: Building > Unit > Lease
- Status badge with color: DRAFT (grey) / ACTIVE (green) / FINISHED (blue) / CANCELLED (red)
- Alert banners (indexation, end notice)
- All sections in read-only card layout
- Action bar: Edit (if DRAFT/ACTIVE) | Activate (if DRAFT) | Finish (if ACTIVE) | Cancel (if DRAFT/ACTIVE)
- Tenants section with "Manage Tenants" (inline panel)
- Indexation section with "Record Indexation" button and history table

#### `indexation-section`

```
┌─────────────────────────────────────────────────────────────────┐
│ Indexation                              [Record Indexation]     │
│ Base index: 121.14 (Jan 2024)  Anniversary: April              │
│                                                                 │
│ Application Date | Old Rent | Index Value | Applied Rent | Note │
│ 2025-04-01       | 850.00€  | 125.40      | 880.00€      | ...  │
│                                                                 │
│ Total since start: +30.00€ (+3.5%)                             │
└─────────────────────────────────────────────────────────────────┘
```

Record Indexation inline form (collapsed by default):
- Application date (required, date picker)
- Old rent (read-only, pre-filled)
- New index value (required, decimal)
- New index month (required, month picker)
- Applied rent (required, decimal)
- Notification sent date (optional)
- Notes (optional)
- "Save" | "Cancel"

#### `tenant-section`

```
┌─────────────────────────────────────────┐
│ Tenants                    [Add Tenant] │
│ Dupont Jean    PRIMARY  📧 gsm          │
│ Martin Marie   CO_TENANT 📧 gsm    [x] │
└─────────────────────────────────────────┘
```

- PersonPicker to add tenant + role selector
- Remove button per non-primary tenant
- Remove primary only if another primary exists
- Error message if trying to remove last primary

#### `lease-list` (full history for a unit)

- Table: Status | Type | Start | End | Tenant(s) | Rent | Actions
- Sorted by start_date DESC
- Click → navigates to `lease-details`

---

### 15. Alerts page / dashboard section

Route: `/leases/alerts`

Table:
| Unit | Building | Alert Type | Deadline | Tenant(s) |
|------|----------|------------|----------|-----------|
| A101 | Résidence Soleil | 🔔 Indexation | 2026-03-15 | Dupont Jean |
| B202 | Tour Bleue | ⚠ End Notice | 2026-04-01 | Martin Marie |

Sorted by deadline ASC. Alert disappears once action is taken (indexation recorded / lease finished).
Add "Alerts" badge in main navigation menu showing count of active alerts.

---

### 16. Routing

```
/housing-units/:unitId/leases/new    → LeaseFormComponent (create)
/leases/:id                          → LeaseDetailsComponent
/leases/:id/edit                     → LeaseFormComponent (edit)
/leases/alerts                       → AlertsComponent
```

`lease-section` is embedded (no route) inside `housing-unit-details`.

---

## BUSINESS RULES SUMMARY

| Rule | Enforcement |
|------|-------------|
| No overlapping ACTIVE/DRAFT per unit | DB partial unique index + service check |
| end_date = start_date + duration_months | Service calculates, stores; client displays only |
| At least one PRIMARY tenant required | Service validation on create/update/remove |
| Cannot edit FINISHED/CANCELLED lease | Service → LeaseNotEditableException → 422 |
| Indexation only on ACTIVE lease | Service check |
| Indexation updates lease.monthly_rent | Transactional update in recordIndexation() |
| Indexation does NOT modify rent_history | No link, separate concern |
| Default notice period by lease type | Static map in service; client auto-fills |
| Alerts computed, not stored | Computed at query time in getAlerts() |

---

## IMPLEMENTATION ORDER

1. `V011__create_lease_tables.sql`
2. `model/entity/Lease.java`
3. `model/entity/LeaseTenant.java` + `LeaseTenantId.java`
4. `model/entity/LeaseIndexationHistory.java`
5. DTOs: `LeaseDTO`, `LeaseSummaryDTO`, `CreateLeaseRequest`, `UpdateLeaseRequest`, `ChangeLeaseStatusRequest`, `AddTenantRequest`, `RecordIndexationRequest`, `LeaseAlertDTO`, `LeaseTenantDTO`, `LeaseIndexationDTO`
6. `mapper/LeaseMapper.java` + `LeaseIndexationMapper.java`
7. `repository/LeaseRepository.java`, `LeaseTenantRepository.java`, `LeaseIndexationRepository.java`
8. `service/LeaseService.java` + `LeaseServiceTest.java`
9. `exception/LeaseNotFoundException.java`, `LeaseOverlapException.java`, `LeaseNotEditableException.java`
10. Update `GlobalExceptionHandler`
11. `controller/LeaseController.java` + `LeaseControllerTest.java`
12. `models/lease.model.ts`
13. `core/services/lease.service.ts`
14. `features/lease/lease-form/` component
15. `features/lease/lease-details/` component
16. `features/lease/indexation-section/` component
17. `features/lease/tenant-section/` component
18. `features/lease/lease-section/` component (embedded in unit-details)
19. Integrate `lease-section` into `HousingUnitDetailsComponent`
20. `features/lease/lease-list/` component
21. Alerts page + navigation badge
22. `lease-routing.module.ts` + routing update

---

## ACCEPTANCE CRITERIA CHECKLIST

- [ ] Housing unit details shows active lease summary with alerts
- [ ] Cannot create lease if ACTIVE/DRAFT already exists on unit
- [ ] End date auto-calculated from start + duration
- [ ] Default notice period auto-populated by lease type
- [ ] At least one PRIMARY tenant required to save
- [ ] DRAFT → ACTIVE transition works
- [ ] ACTIVE → FINISHED works with effective date
- [ ] ACTIVE → CANCELLED works
- [ ] FINISHED/CANCELLED leases are read-only
- [ ] Indexation recorded updates lease rent + creates history entry
- [ ] Indexation history shows total change since lease start
- [ ] Cannot record indexation on non-ACTIVE lease
- [ ] Cannot remove last PRIMARY tenant
- [ ] Indexation alert shown when anniversary approaches
- [ ] End notice alert shown when deadline approaches
- [ ] Alert disappears after indexation is recorded
- [ ] Alerts page lists all pending alerts sorted by deadline
- [ ] Navigation badge shows alert count

---

## DEFINITION OF DONE

- [ ] Flyway migration `V011` executed and validated
- [ ] All backend entities, DTOs, mappers, repos, service, controller implemented
- [ ] Unit tests: LeaseService (> 80% coverage)
- [ ] Integration tests: LeaseController (all endpoints)
- [ ] All frontend components implemented and integrated
- [ ] All US049–US059 acceptance criteria verified manually
- [ ] No regression on existing features (buildings, units, persons)
- [ ] Code reviewed and merged to `develop`

---

**Last Updated**: 2026-02-24
**Branch**: `develop`
**Status**: Ready for Implementation
