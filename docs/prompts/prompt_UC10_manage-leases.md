# ImmoCare — UC010 Manage Leases — Implementation Prompt

I want to implement Use Case UC010 - Manage Leases for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons
- **Prerequisite**: UC009 (Manage Persons) must be fully implemented before starting this UC

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US049 | View Lease for Housing Unit | MUST HAVE | 2 |
| US050 | Create Lease (Draft) | MUST HAVE | 8 |
| US051 | Activate Lease | MUST HAVE | 2 |
| US052 | Edit Lease | MUST HAVE | 3 |
| US053 | Finish Lease | MUST HAVE | 2 |
| US054 | Cancel Lease | MUST HAVE | 2 |
| US055 | Record Indexation | MUST HAVE | 3 |
| US056 | View Indexation History | MUST HAVE | 2 |
| US057 | Add Tenant to Lease | MUST HAVE | 3 |
| US058 | Remove Tenant from Lease | MUST HAVE | 2 |
| US059 | View Lease Alerts | SHOULD HAVE | 3 |

---

## NEW ENTITIES — Migration `V011__create_lease_tables.sql`

### LEASE

```sql
CREATE TABLE lease (
    id                            BIGSERIAL     PRIMARY KEY,
    housing_unit_id               BIGINT        NOT NULL REFERENCES housing_unit(id) ON DELETE RESTRICT,
    status                        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    signature_date                DATE          NOT NULL,
    start_date                    DATE          NOT NULL,
    end_date                      DATE          NOT NULL,
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
    deposit_amount                NUMERIC(10,2) NULL,
    deposit_type                  VARCHAR(30)   NULL,
    deposit_reference             VARCHAR(100)  NULL,
    tenant_insurance_confirmed    BOOLEAN       NOT NULL DEFAULT FALSE,
    tenant_insurance_reference    VARCHAR(100)  NULL,
    tenant_insurance_expiry       DATE          NULL,
    created_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_lease_status   CHECK (status IN ('DRAFT','ACTIVE','FINISHED','CANCELLED')),
    CONSTRAINT chk_lease_type     CHECK (lease_type IN ('SHORT_TERM','MAIN_RESIDENCE_3Y','MAIN_RESIDENCE_6Y','MAIN_RESIDENCE_9Y','STUDENT','GLIDING','COMMERCIAL')),
    CONSTRAINT chk_charges_type   CHECK (charges_type IN ('FORFAIT','PROVISION'))
);

CREATE UNIQUE INDEX uq_lease_active_per_unit ON lease(housing_unit_id)
    WHERE status IN ('ACTIVE','DRAFT');
```

### LEASE_TENANT

```sql
CREATE TABLE lease_tenant (
    lease_id    BIGINT     NOT NULL REFERENCES lease(id) ON DELETE CASCADE,
    person_id   BIGINT     NOT NULL REFERENCES person(id) ON DELETE RESTRICT,
    tenant_role VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    PRIMARY KEY (lease_id, person_id),
    CONSTRAINT chk_tenant_role CHECK (tenant_role IN ('PRIMARY','CO_TENANT','GUARANTOR'))
);
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
```

---

## LEASE TYPE DEFAULT NOTICE PERIODS

| Lease Type | Notice Months |
|------------|---------------|
| MAIN_RESIDENCE_9Y | 3 |
| MAIN_RESIDENCE_6Y | 3 |
| MAIN_RESIDENCE_3Y | 3 |
| SHORT_TERM | 1 |
| STUDENT | 1 |
| GLIDING | 3 |
| COMMERCIAL | 6 |

---

## BACKEND

1. Entities: `Lease`, `LeaseTenant` (composite key), `LeaseIndexationHistory`
2. DTOs: `LeaseDTO`, `LeaseSummaryDTO`, `CreateLeaseRequest`, `UpdateLeaseRequest`, `LeaseStatusChangeRequest`, `RecordIndexationRequest`, `AddTenantRequest`, `IndexationHistoryDTO`, `LeaseAlertDTO`
3. `LeaseMapper` (MapStruct)
4. Repositories: `LeaseRepository`, `LeaseTenantRepository`, `LeaseIndexationHistoryRepository`
5. `LeaseService`:
   - `getLeasesForUnit(unitId)` → `List<LeaseSummaryDTO>` + alert flags — US049
   - `getLeaseById(id)` → `LeaseDTO` with tenants + indexation summary
   - `createLease(unitId, req)` → checks no ACTIVE/DRAFT exists; status=DRAFT — US050
   - `changeStatus(id, req)` → DRAFT→ACTIVE (US051), ACTIVE→FINISHED (US053), DRAFT/ACTIVE→CANCELLED (US054)
   - `updateLease(id, req)` → only DRAFT/ACTIVE; FINISHED/CANCELLED → 422 — US052
   - `recordIndexation(id, req)` → creates `LeaseIndexationHistory` record AND updates `lease.monthly_rent` — US055
   - `getIndexationHistory(id)` → US056
   - `addTenant(id, req)` → check not duplicate; US057
   - `removeTenant(id, personId)` → check not last PRIMARY; US058
   - `getAlerts()` → all leases approaching anniversary or end notice — US059
6. `LeaseController`:

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/housing-units/{unitId}/leases | US049 |
| GET | /api/v1/leases/{id} | US049 |
| POST | /api/v1/housing-units/{unitId}/leases | US050 |
| PATCH | /api/v1/leases/{id}/status | US051, US053, US054 |
| PUT | /api/v1/leases/{id} | US052 |
| POST | /api/v1/leases/{id}/indexations | US055 |
| GET | /api/v1/leases/{id}/indexations | US056 |
| POST | /api/v1/leases/{id}/tenants | US057 |
| DELETE | /api/v1/leases/{id}/tenants/{personId} | US058 |
| GET | /api/v1/leases/alerts | US059 |

---

## FRONTEND

7. `lease.model.ts` — all TS interfaces; `LEASE_TYPE_LABELS`, `DEFAULT_NOTICE_MONTHS` map
8. `lease.service.ts`
9. `lease-section` component (embedded in `HousingUnitDetailsComponent`):
   - No lease → "No active lease" + "Create Lease" button
   - Active/Draft lease → status badge, tenant names, rent, dates, alert banners (US049)
   - "View all leases" link
10. `lease-form` (create + edit) — 7 sections (General / Financial / Indexation / Registration / Guarantee / Insurance / Tenants):
    - `end_date` auto-computed from `start_date + duration_months` (client-side)
    - `notice_period_months` auto-filled on `lease_type` change from `DEFAULT_NOTICE_MONTHS`
    - `PersonPickerComponent` in Tenants section with role selector
    - At least one PRIMARY tenant required to save (US050 AC5)
11. `lease-details` — read-only for FINISHED/CANCELLED; activate/finish/cancel action buttons
12. `indexation-section` — "Record Indexation" button (ACTIVE only); collapsible history table with total summary (US055, US056)
13. `tenant-section` — list of tenants with role; "Add Tenant" + "Remove" per row (US057, US058)
14. `lease-list` — full lease history for unit (all statuses)
15. Alerts page — `/alerts` — all pending alerts sorted by deadline; nav badge with count (US059)

---

## ACCEPTANCE CRITERIA

- [ ] Only one ACTIVE/DRAFT lease per unit (unique index enforced)
- [ ] end_date auto-calculated and stored
- [ ] FINISHED/CANCELLED leases are read-only (422 on PUT)
- [ ] Indexation: updates `lease.monthly_rent` AND creates history record
- [ ] Cannot remove last PRIMARY tenant (400)
- [ ] Alert disappears after indexation recorded for that anniversary year
- [ ] All US049–US059 acceptance criteria verified

---

**Last Updated**: 2026-02-27
**Branch**: `develop`
**Status**: 📋 Ready for Implementation
