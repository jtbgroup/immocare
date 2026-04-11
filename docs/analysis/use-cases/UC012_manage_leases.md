# UC010 — Manage Leases

## Overview

| Attribute | Value |
|---|---|
| ID | UC010 |
| Name | Manage Leases |
| Actor | Admin |
| Module | Leases |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Manages rental leases for housing units. A lease links a housing unit to one or more tenants (persons) for a defined period, with rent, charges, deposit, and registration details. Only one ACTIVE or DRAFT lease may exist per unit at a time. Lease status follows a strict transition machine. Rent and charges can be adjusted post-creation via `lease_rent_adjustment`. Automatic alerts are computed for indexation and end-of-lease notice deadlines.

> **Note:** Indexation recording (previously `recordIndexation`) has been removed. Indexation is now tracked via `lease_rent_adjustment` with `field = 'RENT'`. The `IndexationSectionComponent` and `RecordIndexationRequest` are dead code to be removed from the frontend.

---

## User Stories

### US049 — View Lease for Housing Unit

**As an** admin, **I want to** see the current lease status directly on the housing unit details page **so that** I can quickly check occupancy and key lease information.

**Acceptance Criteria:**
- AC1: No active/draft lease → "No active lease" message and "Create Lease" button.
- AC2: Active lease displayed: status badge (ACTIVE green), tenant name(s), monthly rent and charges, start/end dates, "View" and "Edit" buttons.
- AC3: Draft lease displayed with "DRAFT" (grey) status badge.
- AC4: End notice alert: if `endDate − noticePeriodMonths` is within 30 days → orange banner "⚠ Lease ending soon — notice deadline: [date]".
- AC5: Indexation alert: if anniversary is within `indexationNoticeDays` days and no indexation recorded for this year → orange banner "⚠ Indexation due — anniversary: [date]".
- AC6: "View all leases" link shows all leases (all statuses) sorted by startDate DESC.

**Endpoints:**
- `GET /api/v1/housing-units/{unitId}/leases`
- `GET /api/v1/housing-units/{unitId}/leases/active`
- `GET /api/v1/leases/{id}`

---

### US050 — Create Lease (Draft)

**As an** admin, **I want to** create a new lease for a housing unit **so that** I can formalize the rental agreement.

**Acceptance Criteria:**
- AC1: Blocked if unit already has an ACTIVE or DRAFT lease → error "An active or draft lease already exists for this unit. Finish or cancel it first."
- AC2: Form has sections: Dates & Duration / Financial / Indexation / Registration / Guarantee & Insurance / Tenants. Unit is pre-selected and read-only.
- AC3: End date auto-calculated: start date + duration months.
- AC4: Lease type selection pre-fills default notice period (e.g., MAIN_RESIDENCE_9Y → 3 months).
- AC5: At least one PRIMARY tenant required to save.
- AC6: Save as Draft → lease created with status DRAFT, success message "Lease saved as draft".
- AC7: Validation — primary tenant required, duration ≥ 1 month, signature date required, positive monthly rent.
- AC8: PROVISION charges type shows "Charges Description" field; all optional fields (registration, deposit, insurance) are truly optional.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/leases`

---

### US051 — Activate Lease

**As an** admin, **I want to** activate a draft lease **so that** it becomes the official active lease for the unit.

**Acceptance Criteria:**
- AC1: "Activate" button visible on DRAFT lease.
- AC2: Confirmation dialog: "Activate this lease? It will become the official active lease for unit [X]."
- AC3: On confirm: lease status changes to ACTIVE; status badge shows "ACTIVE" on unit details.
- AC4: Blocked if another ACTIVE lease already exists on the unit → error "Activation blocked: another lease is already active for this unit".

**Endpoint:** `PATCH /api/v1/leases/{id}/status` with `{ "targetStatus": "ACTIVE" }`

---

### US052 — Edit Lease

**As an** admin, **I want to** edit an existing lease **so that** I can correct data or add information obtained after signing.

**Acceptance Criteria:**
- AC1: Edit form pre-filled with all current values.
- AC2: Changes saved; new values shown on details.
- AC3: End date recalculates automatically when start date or duration changes.
- AC4: FINISHED and CANCELLED leases are read-only — no "Edit" button shown.
- AC5: Validation same as creation.

**Endpoint:** `PUT /api/v1/leases/{id}`

---

### US053 — Finish Lease

**As an** admin, **I want to** mark a lease as finished **so that** I can record that the tenant has vacated and the unit is available.

**Acceptance Criteria:**
- AC1: "Finish Lease" button shown on ACTIVE lease.
- AC2: Confirmation dialog asks for: Effective end date (required, default today) and Notes (optional).
- AC3: Lease status changes to FINISHED; unit Leases section shows "No active lease" with "Create Lease" button.
- AC4: Finished lease still appears in lease history.

**Endpoint:** `PATCH /api/v1/leases/{id}/status` with `{ "targetStatus": "FINISHED", "effectiveDate": "...", "notes": "..." }`

---

### US054 — Cancel Lease

**As an** admin, **I want to** cancel a draft or active lease **so that** I can record an early termination or a draft that was never used.

**Acceptance Criteria:**
- AC1: "Cancel Lease" button (red/warning) shown on DRAFT and ACTIVE leases.
- AC2: Confirmation dialog: "Are you sure you want to cancel this lease? This action cannot be undone." with Cancellation date and Reason/notes (optional).
- AC3: Lease status changes to CANCELLED; unit becomes available for a new lease.
- AC4: Cancelled lease preserved in history with "CANCELLED" badge (red).

**Endpoint:** `PATCH /api/v1/leases/{id}/status` with `{ "targetStatus": "CANCELLED", "effectiveDate": "...", "notes": "..." }`

---

### US055 — Record Indexation

**As an** admin, **I want to** record a rent indexation on an active lease **so that** the new rent amount is tracked.

**Acceptance Criteria:**
- AC1: "Record Indexation" button shown on ACTIVE lease.
- AC2: Form shows: current rent as "Old Rent (read-only)", Application Date (required), New Index Value (required), New Index Month (required), Applied Rent (required), Notification Sent Date (optional), Notes (optional).
- AC3: On save: indexation record created; lease monthly rent updated to Applied Rent.
- AC4: Cannot record indexation on non-ACTIVE lease — button not visible.
- AC5: Validation — all required fields; Applied Rent must be > 0.

**Endpoint:** `POST /api/v1/leases/{id}/indexations`

---

### US056 — View Indexation History

**As an** admin, **I want to** view the complete indexation history of a lease **so that** I can track all rent changes over time.

**Acceptance Criteria:**
- AC1: Indexation section on lease details shows collapsible table with all records.
- AC2: Columns: Application Date | Old Rent | Index Value | Index Month | Applied Rent | Notification Date | Notes. Sorted by application date DESC.
- AC3: No indexations → "No indexation recorded yet".
- AC4: Summary: "Total indexation: +€120.00 (+15.0%) since lease start".

**Endpoint:** `GET /api/v1/leases/{id}/indexations`

---

### US057 — Add Tenant to Lease

**As an** admin, **I want to** add a tenant (or co-tenant or guarantor) to a lease **so that** all occupants are formally registered.

**Acceptance Criteria:**
- AC1: Tenants section shows all current tenants with role and contact info.
- AC2: Person picker opens with role selector (PRIMARY / CO_TENANT / GUARANTOR); selecting adds the tenant.
- AC3: Multiple tenants of different roles allowed.
- AC4: Adding an already-assigned person → error "This person is already a tenant on this lease".
- AC5: "Create new person" shortcut available in the picker.

**Endpoint:** `POST /api/v1/leases/{id}/tenants`

---

### US058 — Remove Tenant from Lease

**As an** admin, **I want to** remove a tenant from a lease **so that** I can correct errors or record departure of a co-tenant.

**Acceptance Criteria:**
- AC1: Each tenant row has a "Remove" button.
- AC2: Confirmation dialog: "Remove [Name] from this lease?"
- AC3: Can remove CO_TENANT; PRIMARY tenant remains.
- AC4: Cannot remove last PRIMARY tenant → error "Cannot remove the only primary tenant. Add another primary tenant first."
- AC5: Can remove GUARANTOR without affecting other tenants.

**Endpoint:** `DELETE /api/v1/leases/{id}/tenants/{personId}`

---

### US059 — View Lease Alerts

**As an** admin, **I want to** see all upcoming lease deadlines in one place **so that** I don't miss any indexation or end-of-lease notification obligation.

**Acceptance Criteria:**
- AC1: Indexation alert visible on both lease details and unit details when anniversary is within `indexationNoticeDays`.
- AC2: End notice alert: "⚠ Lease ending soon — send notice before [date]" when within 30 days.
- AC3: Global alerts page shows all pending alerts: Unit, Lease ID, Alert type (Indexation / End Notice), Deadline date, sorted ASC.
- AC4: Alert disappears after indexation is recorded for that anniversary year.
- AC5: No active leases → alerts section shows "No pending alerts".

**Endpoint:** `GET /api/v1/leases/alerts`

---

## Data Model

### Table: `lease`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `housing_unit_id` | BIGINT | NOT NULL, FK → `housing_unit.id` ON DELETE RESTRICT |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT', CHECK IN (DRAFT, ACTIVE, FINISHED, CANCELLED) |
| `signature_date` | DATE | NOT NULL |
| `start_date` | DATE | NOT NULL |
| `end_date` | DATE | NOT NULL |
| `lease_type` | VARCHAR(30) | NOT NULL, CHECK IN (SHORT_TERM, MAIN_RESIDENCE_3Y, MAIN_RESIDENCE_6Y, MAIN_RESIDENCE_9Y, STUDENT, GLIDING, COMMERCIAL) |
| `duration_months` | INTEGER | NOT NULL, CHECK > 0 |
| `notice_period_months` | INTEGER | NOT NULL, CHECK > 0 |
| `monthly_rent` | NUMERIC(10,2) | NOT NULL, CHECK > 0 |
| `monthly_charges` | NUMERIC(10,2) | NOT NULL, DEFAULT 0 |
| `charges_type` | VARCHAR(20) | NOT NULL, DEFAULT 'FORFAIT', CHECK IN (FORFAIT, PROVISION) |
| `charges_description` | TEXT | nullable |
| `registration_spf` | VARCHAR(50) | nullable |
| `registration_region` | VARCHAR(50) | nullable |
| `registration_inventory_spf` | VARCHAR(100) | nullable |
| `registration_inventory_region` | VARCHAR(100) | nullable |
| `deposit_amount` | NUMERIC(10,2) | nullable, CHECK ≥ 0 |
| `deposit_type` | VARCHAR(30) | nullable, CHECK IN (BLOCKED_ACCOUNT, BANK_GUARANTEE, CPAS, INSURANCE) |
| `deposit_reference` | VARCHAR(100) | nullable |
| `tenant_insurance_confirmed` | BOOLEAN | NOT NULL, DEFAULT false |
| `tenant_insurance_reference` | VARCHAR(100) | nullable |
| `tenant_insurance_expiry` | DATE | nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

**Index:** `UNIQUE (housing_unit_id) WHERE status IN ('ACTIVE','DRAFT')` — prevents multiple active/draft leases per unit.

### Table: `lease_tenant`

| Column | Type | Constraints |
|---|---|---|
| `lease_id` | BIGINT | PK (composite), FK → `lease.id` ON DELETE CASCADE |
| `person_id` | BIGINT | PK (composite), FK → `person.id` ON DELETE RESTRICT |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'PRIMARY', CHECK IN (PRIMARY, CO_TENANT, GUARANTOR) |

### Table: `lease_rent_adjustment`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `lease_id` | BIGINT | NOT NULL, FK → `lease.id` ON DELETE CASCADE |
| `field` | VARCHAR(10) | NOT NULL, CHECK IN (RENT, CHARGES) |
| `old_value` | NUMERIC(10,2) | NOT NULL |
| `new_value` | NUMERIC(10,2) | NOT NULL |
| `reason` | TEXT | NOT NULL |
| `effective_date` | DATE | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `LeaseSummaryDTO` (list response)
```
id, status, leaseType, startDate, endDate,
monthlyRent, monthlyCharges, totalRent, chargesType,
tenantNames: String[],
indexationAlertActive, indexationAlertDate,
endNoticeAlertActive, endNoticeAlertDate
```

### `LeaseDTO` (detail response)
```
id, housingUnitId, housingUnitNumber, buildingId, buildingName,
status, signatureDate, startDate, endDate,
leaseType, durationMonths, noticePeriodMonths,
monthlyRent, monthlyCharges, totalRent, chargesType, chargesDescription,
registrationSpf, registrationRegion,
registrationInventorySpf, registrationInventoryRegion,
depositAmount, depositType, depositReference,
tenantInsuranceConfirmed, tenantInsuranceReference, tenantInsuranceExpiry,
tenants: LeaseTenantDTO[],
rentAdjustments: LeaseRentAdjustmentDTO[],
indexationAlertActive, indexationAlertDate,
endNoticeAlertActive, endNoticeAlertDate,
createdAt, updatedAt
```

### `LeaseTenantDTO`
```
personId, lastName, firstName, email, gsm, role
```

### `LeaseRentAdjustmentDTO`
```
id, field (RENT|CHARGES), oldValue, newValue, reason, effectiveDate, createdAt
```

### `CreateLeaseRequest` (POST body)
```
housingUnitId*       Long
signatureDate*       LocalDate
startDate*           LocalDate
endDate*             LocalDate
leaseType*           LeaseType
durationMonths*      int > 0
noticePeriodMonths*  int > 0
monthlyRent*         BigDecimal > 0
monthlyCharges       BigDecimal default 0
chargesType          ChargesType default FORFAIT
chargesDescription   String
registrationSpf, registrationRegion,
registrationInventorySpf, registrationInventoryRegion,
depositAmount, depositType, depositReference,
tenantInsuranceConfirmed, tenantInsuranceReference, tenantInsuranceExpiry,
tenants*             AddTenantRequest[]  (min 1 PRIMARY required)
```

### `UpdateLeaseRequest` (PUT body)
Same as `CreateLeaseRequest` minus `housingUnitId` and `tenants`.

### `AddTenantRequest`
```
personId*  Long
role*      TenantRole (PRIMARY, CO_TENANT, GUARANTOR)
```

### `AdjustRentRequest`
```
field*         RentField (RENT, CHARGES)
newValue*      BigDecimal
reason*        String
effectiveDate* LocalDate
```

---

## Enums

### `LeaseStatus`: `DRAFT`, `ACTIVE`, `FINISHED`, `CANCELLED`
### `LeaseType`: `SHORT_TERM`, `MAIN_RESIDENCE_3Y`, `MAIN_RESIDENCE_6Y`, `MAIN_RESIDENCE_9Y`, `STUDENT`, `GLIDING`, `COMMERCIAL`
### `ChargesType`: `FORFAIT`, `PROVISION`
### `DepositType`: `BLOCKED_ACCOUNT`, `BANK_GUARANTEE`, `CPAS`, `INSURANCE`
### `TenantRole`: `PRIMARY`, `CO_TENANT`, `GUARANTOR`
### `RentField`: `RENT`, `CHARGES`

### Default duration by type (months)
| LeaseType | durationMonths | noticePeriodMonths |
|---|---|---|
| SHORT_TERM | 3 | 1 |
| MAIN_RESIDENCE_3Y | 36 | 3 |
| MAIN_RESIDENCE_6Y | 72 | 3 |
| MAIN_RESIDENCE_9Y | 108 | 3 |
| STUDENT | 12 | 1 |
| GLIDING | 12 | 3 |
| COMMERCIAL | 108 | 6 |

---

## Alert Logic

### Indexation Alert
- Applies to ACTIVE leases only.
- Anniversary date = same month/day as `startDate` in the current year (advance to next year if past).
- Trigger = `anniversary - 30 days`.
- If today ≥ trigger: alert is active **unless** a `lease_rent_adjustment` with `field = 'RENT'` already exists for the anniversary year.

### End Notice Alert
- `noticeDeadline = endDate - noticePeriodMonths`.
- Alert is active when today ≥ `noticeDeadline`.

---

## Status Transitions

```
DRAFT → ACTIVE
DRAFT → CANCELLED
ACTIVE → FINISHED
ACTIVE → CANCELLED
```
All other transitions are invalid (HTTP 422).

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC010-01 | Only one ACTIVE or DRAFT lease per unit at a time |
| BR-UC010-02 | At least one PRIMARY tenant required on create |
| BR-UC010-03 | Only DRAFT/ACTIVE leases are editable |
| BR-UC010-04 | Status transitions follow the state machine |
| BR-UC010-05 | Rent adjustments only on ACTIVE leases |
| BR-UC010-06 | `totalRent = monthlyRent + monthlyCharges` (computed) |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Lease not found | 404 | `NOT_FOUND` |
| Lease overlap (unit has ACTIVE/DRAFT) | 409 | `LEASE_OVERLAP` |
| Lease not editable | 422 | `LEASE_NOT_EDITABLE` |
| Invalid status transition | 422 | `INVALID_STATUS_TRANSITION` |

---

## Dead Code to Remove (Frontend)

The following frontend artifacts are dead code and must be deleted:
- `IndexationSectionComponent` (`indexation-section.component.ts/.html/.scss`)
- `RecordIndexationRequest` interface in `lease.model.ts`
- Fields `baseIndexValue`, `baseIndexMonth`, `indexationAnniversaryMonth` in `Lease` interface
- Field `indexations: LeaseIndexation[]` in `Lease` interface
- `LeaseIndexation` interface in `lease.model.ts`
- Call to `leaseService.recordIndexation()` in `IndexationSectionComponent`

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC010 — Manage Leases in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, Lombok, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS, ReactiveFormsModule
- Database: Flyway V001 already contains `lease`, `lease_tenant`, `lease_rent_adjustment` tables — do NOT generate a migration
- Branch: develop

Backend classes to generate:
1. Enums: `LeaseStatus`, `LeaseType`, `ChargesType`, `DepositType`, `TenantRole`, `RentField`
2. Entities:
   - `Lease` — table `lease`, all columns. OneToMany tenants (cascade ALL, orphanRemoval), OneToMany rentAdjustments (@OrderBy effectiveDate DESC). @PrePersist/@PreUpdate.
   - `LeaseTenant` — table `lease_tenant`, composite PK (lease_id + person_id) via @IdClass LeaseTenantId. Enum TenantRole.
   - `LeaseRentAdjustment` — table `lease_rent_adjustment`. Fields: id, lease (ManyToOne), field (String RENT|CHARGES), oldValue, newValue, reason, effectiveDate, createdAt.
3. DTOs: `LeaseSummaryDTO`, `LeaseDTO`, `LeaseTenantDTO`, `LeaseRentAdjustmentDTO`, `CreateLeaseRequest` (validated, @Valid List<AddTenantRequest>), `UpdateLeaseRequest`, `AddTenantRequest`, `AdjustRentRequest`, `ChangeLeaseStatusRequest`, `LeaseAlertDTO`
4. Repositories: `LeaseRepository` (findByHousingUnitIdOrderByStartDateDesc, existsByHousingUnitIdAndStatusIn), `LeaseTenantRepository`, `LeaseRentAdjustmentRepository` (findByLeaseIdOrderByEffectiveDateDescCreatedAtDesc, existsRentAdjustmentForYear @Query)
5. Exceptions: `LeaseNotFoundException`, `LeaseOverlapException`, `LeaseNotEditableException`, `LeaseStatusTransitionException`
6. Service: `LeaseService`:
   - getByUnit(unitId): List<LeaseSummaryDTO> with computed alerts
   - getById(id): LeaseDTO with alerts
   - create(req, activate): validate PRIMARY tenant, check overlap, create entity
   - update(id, req): check isEditable(), apply fields
   - changeStatus(id, req): enforce state machine
   - addTenant(id, req): check editable, no duplicate
   - removeTenant(id, personId): check editable, check PRIMARY not last
   - adjustRent(id, req): ACTIVE only, create adjustment record, update lease rent/charges
   - getAlerts(): all ACTIVE leases, compute both alert types
   Alert logic constants: INDEXATION_NOTICE_DAYS = 30

7. Controller: `LeaseController` (@RequiredArgsConstructor):
   - GET /api/v1/housing-units/{unitId}/leases
   - GET /api/v1/leases/{id}
   - POST /api/v1/leases?activate=false
   - PUT /api/v1/leases/{id}
   - PATCH /api/v1/leases/{id}/status
   - POST /api/v1/leases/{id}/tenants
   - DELETE /api/v1/leases/{id}/tenants/{personId}
   - POST /api/v1/leases/{id}/rent-adjustments
   - GET /api/v1/leases/alerts

Frontend classes to generate:
1. Model: `lease.model.ts` — all enums (LeaseStatus, LeaseType, ChargesType, DepositType, TenantRole, RentField), LEASE_TYPE_LABELS, LEASE_DURATION_MONTHS, DEFAULT_NOTICE_MONTHS maps, all interfaces (Lease, LeaseSummary, LeaseAlert, LeaseTenant, LeaseRentAdjustment, CreateLeaseRequest, UpdateLeaseRequest, AddTenantRequest, AdjustRentRequest, ChangeLeaseStatusRequest). DO NOT include: RecordIndexationRequest, LeaseIndexation, baseIndexValue/baseIndexMonth/indexationAnniversaryMonth fields, indexations[] array.
2. Service: `LeaseService` — getByUnit(unitId), getById(id), create(req, activate), update(id, req), changeStatus(id, req), addTenant(id, req), removeTenant(id, personId), adjustRent(id, req), getAlerts()
3. Components (standalone):
   a. `LeaseSectionComponent` ([unitId] input, inside HousingUnitDetailsComponent)
      - Shows ACTIVE/DRAFT lease as a card with alert banners, tenant names, rent, period
      - Shows past leases toggle
      - "New Lease" button (navigates to form)
   b. `LeaseFormComponent` (routed: /housing-units/:unitId/leases/new and /leases/:id/edit)
      - Full reactive form for all lease fields
      - LeaseType selector auto-fills durationMonths and noticePeriodMonths (editable)
      - endDate auto-calculated from startDate + durationMonths (bidirectional)
      - Tenant picker section: list of pending tenants, add via PersonPickerComponent, role selector, prevent duplicate
      - Conditional deposit and insurance fields
      - activate checkbox on create
   c. `LeaseDetailsComponent` (routed: /leases/:id)
      - Full read view with all sections (dates, rent, tenants, registration, deposit, insurance)
      - Alert banners when active
      - Status change button (with transition validation)
      - Sub-components: TenantSectionComponent, RentAdjustmentSectionComponent
   d. `TenantSectionComponent` ([lease] input, (leaseUpdated) output)
      - Lists tenants with role badge
      - Add tenant (PersonPicker + role) on ACTIVE/DRAFT leases
      - Remove tenant button (disabled if last PRIMARY)
   e. `RentAdjustmentSectionComponent` ([lease] input, (leaseUpdated) output)
      - Lists rent adjustments (RENT and CHARGES)
      - Add adjustment form (field selector, newValue, reason, effectiveDate) on ACTIVE leases
   f. `AlertsComponent` (routed: /leases/alerts)
      - Table of all pending alerts with type badge (INDEXATION / END_NOTICE), unit, building, deadline, tenants

Business rules to enforce in frontend:
- BR-UC010-01: navigate away with error if unit already has ACTIVE/DRAFT lease
- BR-UC010-02: disable submit if no PRIMARY tenant
- BR-UC010-03: disable Edit button on FINISHED/CANCELLED leases
- BR-UC010-04: show only valid transitions in status change dropdown
- Alert banners: orange for END_NOTICE, yellow for INDEXATION
```
