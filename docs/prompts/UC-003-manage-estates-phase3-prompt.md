# ImmoCare — UC004_ESTATE_PLACEHOLDER Manage Estates — Phase 3 Implementation Prompt

I want to implement UC004_ESTATE_PLACEHOLDER - Manage Estates (Phase 3 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Prerequisite**: Phase 2 (V018) must be fully deployed and tested before starting this phase.
- **Flyway**: last migration is V018. Use **V019** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 3 of 6** of the multi-tenant estate migration.

| Phase | Flyway | Scope |
|---|---|---|
| Phase 1 | V017 | ✅ Done — Estate CRUD, membership, `app_user` migration |
| Phase 2 | V018 | ✅ Done — `estate_id` on `building`; Buildings & Housing Units scoped |
| **Phase 3 (this prompt)** | V019 | `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V020 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| Phase 5 | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

## WHAT CHANGES IN THIS PHASE

- `estate_id UUID NOT NULL` added to `person`
- `PersonController` routes migrated to `/api/v1/estates/{estateId}/persons/**`
- `PersonService` all queries filtered by `estateId`
- `LeaseController` routes migrated to `/api/v1/estates/{estateId}/leases/**`
- `LeaseService` all queries filtered by `estateId` via housing unit → building → estate
- `person_bank_account` scoped via `person.estate_id`
- Frontend `PersonService` and `LeaseService` updated to use `estateId`
- `EstateDashboardDTO` count for `activeLeases` now populated

## WHAT DOES NOT CHANGE

- `lease`, `lease_tenant`, `lease_rent_adjustment` — no new columns; estate scope derived via `housing_unit → building.estate_id`
- `person_bank_account` — no new column; scoped via `person.estate_id`
- `FinancialTransactionController` — untouched (Phase 4)
- Phase 1 and Phase 2 components and services — untouched

---

## DATABASE MIGRATION — `V019__estate_scope_persons.sql`

```sql
-- Use case: UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 3)

-- Add estate_id to person
ALTER TABLE person
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_person_estate ON person(estate_id);
```

> `lease` derives its estate scope via `housing_unit → building.estate_id` — no column added.

---

## BACKEND

### Modified Entity: `Person`

Add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL). No other changes.

### Modified Repository: `PersonRepository`

Replace all queries with estate-scoped versions:

```java
@Query("""
    SELECT p FROM Person p WHERE p.estate.id = :estateId
    AND (LOWER(p.lastName) LIKE LOWER(CONCAT('%',:search,'%'))
      OR LOWER(p.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
      OR LOWER(p.email) LIKE LOWER(CONCAT('%',:search,'%')))
    """)
Page<Person> searchByEstate(@Param("estateId") UUID estateId,
                             @Param("search") String search,
                             Pageable pageable);

Page<Person> findByEstateIdOrderByLastNameAsc(UUID estateId, Pageable pageable);

List<Person> findTop10ByEstateIdAndLastNameContainingIgnoreCaseOrEstateIdAndFirstNameContainingIgnoreCase(
    UUID estateId1, String lastName, UUID estateId2, String firstName);

boolean existsByEstateIdAndNationalIdIgnoreCase(UUID estateId, String nationalId);
boolean existsByEstateIdAndNationalIdIgnoreCaseAndIdNot(UUID estateId, String nationalId, Long id);

boolean existsByEstateIdAndId(UUID estateId, Long personId);

long countByEstateId(UUID estateId);
```

### Modified Service: `PersonService`

All methods receive `estateId` as first parameter:

```java
Page<PersonSummaryDTO> getPersons(UUID estateId, String search, Pageable pageable);
PersonDTO getPersonById(UUID estateId, Long id);
List<PersonSummaryDTO> searchForPicker(UUID estateId, String q);
PersonDTO createPerson(UUID estateId, CreatePersonRequest req);
PersonDTO updatePerson(UUID estateId, Long id, UpdatePersonRequest req);
void deletePerson(UUID estateId, Long id);

// Person bank accounts — scoped via person
List<PersonBankAccountDTO> getBankAccounts(UUID estateId, Long personId);
PersonBankAccountDTO addBankAccount(UUID estateId, Long personId, SavePersonBankAccountRequest req);
PersonBankAccountDTO updateBankAccount(UUID estateId, Long personId, Long accountId, SavePersonBankAccountRequest req);
void deleteBankAccount(UUID estateId, Long personId, Long accountId);
```

Add `verifyPersonBelongsToEstate(UUID estateId, Long personId)` helper.

### Modified Controller: `PersonController`

Change all routes to `/api/v1/estates/{estateId}/persons/**`:

| Method | Path |
|--------|------|
| GET | `/api/v1/estates/{estateId}/persons` |
| GET | `/api/v1/estates/{estateId}/persons/{id}` |
| GET | `/api/v1/estates/{estateId}/persons/picker` |
| POST | `/api/v1/estates/{estateId}/persons` |
| PUT | `/api/v1/estates/{estateId}/persons/{id}` |
| DELETE | `/api/v1/estates/{estateId}/persons/{id}` |
| GET | `/api/v1/estates/{estateId}/persons/{personId}/bank-accounts` |
| POST | `/api/v1/estates/{estateId}/persons/{personId}/bank-accounts` |
| PUT | `/api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}` |
| DELETE | `/api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}` |

Add `@PreAuthorize("@security.isMemberOf(#estateId)")` at class level. Override with `isManagerOf` on POST, PUT, DELETE methods.

### Modified Repository: `LeaseRepository`

Add estate filter via housing unit → building join:

```java
@Query("""
    SELECT l FROM Lease l
    WHERE l.housingUnit.building.estate.id = :estateId
    AND l.housingUnit.id = :unitId
    ORDER BY l.startDate DESC
    """)
List<Lease> findByEstateIdAndUnitId(@Param("estateId") UUID estateId,
                                     @Param("unitId") Long unitId);

@Query("SELECT l FROM Lease l WHERE l.housingUnit.building.estate.id = :estateId AND l.id = :id")
Optional<Lease> findByEstateIdAndId(@Param("estateId") UUID estateId, @Param("id") Long id);

@Query("""
    SELECT l FROM Lease l
    WHERE l.housingUnit.building.estate.id = :estateId
    AND l.status = 'ACTIVE'
    """)
List<Lease> findActiveByEstateId(@Param("estateId") UUID estateId);

long countByHousingUnit_Building_Estate_IdAndStatus(UUID estateId, String status);
```

### Modified Service: `LeaseService`

All methods receive `estateId` as first parameter. Add `verifyLeaseBelongsToEstate(UUID estateId, Long leaseId)` helper.

Key method signatures:
```java
List<LeaseSummaryDTO> getByUnit(UUID estateId, Long unitId);
LeaseDTO getById(UUID estateId, Long id);
LeaseDTO create(UUID estateId, Long unitId, CreateLeaseRequest req);
LeaseDTO update(UUID estateId, Long id, UpdateLeaseRequest req);
LeaseDTO changeStatus(UUID estateId, Long id, ChangeLeaseStatusRequest req);
LeaseDTO addTenant(UUID estateId, Long id, AddTenantRequest req);
void removeTenant(UUID estateId, Long id, Long personId);
LeaseDTO adjustRent(UUID estateId, Long id, AdjustRentRequest req);
List<LeaseAlertDTO> getAlerts(UUID estateId);
```

### Modified Controller: `LeaseController`

Change all routes to `/api/v1/estates/{estateId}/leases/**` and `/api/v1/estates/{estateId}/housing-units/{unitId}/leases/**`:

| Method | Path |
|--------|------|
| GET | `/api/v1/estates/{estateId}/housing-units/{unitId}/leases` |
| GET | `/api/v1/estates/{estateId}/leases/{id}` |
| POST | `/api/v1/estates/{estateId}/housing-units/{unitId}/leases` |
| PATCH | `/api/v1/estates/{estateId}/leases/{id}/status` |
| PUT | `/api/v1/estates/{estateId}/leases/{id}` |
| POST | `/api/v1/estates/{estateId}/leases/{id}/tenants` |
| DELETE | `/api/v1/estates/{estateId}/leases/{id}/tenants/{personId}` |
| POST | `/api/v1/estates/{estateId}/leases/{id}/rent-adjustments` |
| GET | `/api/v1/estates/{estateId}/leases/alerts` |

### Updated `EstateDashboardDTO` population

In `EstateService.getDashboard()`:

```java
int activeLeases = (int) leaseRepository
    .countByHousingUnit_Building_Estate_IdAndStatus(estateId, "ACTIVE");
// pendingAlerts remain 0 until Phase 6
```

### Lease suggestion algorithm update (`PersonService` / `TransactionImportService`)

The IBAN lookup for lease suggestion (UC016) must now be scoped:
- When called during import, the `estateId` of the active import context must be passed
- `PersonBankAccountRepository.findByIban(iban)` must verify the matched person belongs to the same estate as the transaction being imported

---

## FRONTEND

### Modified Service: `PersonService`

Inject `ActiveEstateService`. Update all URLs:

```typescript
private get estateId(): string {
  return this.activeEstateService.activeEstateId()!;
}

getPersons(page, size, sort?, search?): Observable<Page<PersonSummary>>
  → GET /api/v1/estates/{estateId}/persons

getPersonById(id): Observable<Person>
  → GET /api/v1/estates/{estateId}/persons/{id}

search(q: string): Observable<PersonSummary[]>
  → GET /api/v1/estates/{estateId}/persons/picker?q={q}

createPerson(req): Observable<Person>
  → POST /api/v1/estates/{estateId}/persons

updatePerson(id, req): Observable<Person>
  → PUT /api/v1/estates/{estateId}/persons/{id}

deletePerson(id): Observable<void>
  → DELETE /api/v1/estates/{estateId}/persons/{id}

// Bank accounts
getBankAccounts(personId): Observable<PersonBankAccount[]>
  → GET /api/v1/estates/{estateId}/persons/{personId}/bank-accounts

addBankAccount(personId, req): Observable<PersonBankAccount>
  → POST /api/v1/estates/{estateId}/persons/{personId}/bank-accounts

updateBankAccount(personId, id, req): Observable<PersonBankAccount>
  → PUT /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}

deleteBankAccount(personId, id): Observable<void>
  → DELETE /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}
```

### Modified Service: `LeaseService`

Same pattern — inject `ActiveEstateService`, update all URLs:

```typescript
getByUnit(unitId): Observable<LeaseSummary[]>
  → GET /api/v1/estates/{estateId}/housing-units/{unitId}/leases

getById(id): Observable<Lease>
  → GET /api/v1/estates/{estateId}/leases/{id}

create(unitId, req, activate?): Observable<Lease>
  → POST /api/v1/estates/{estateId}/housing-units/{unitId}/leases

update(id, req): Observable<Lease>
  → PUT /api/v1/estates/{estateId}/leases/{id}

changeStatus(id, req): Observable<Lease>
  → PATCH /api/v1/estates/{estateId}/leases/{id}/status

addTenant(id, req): Observable<Lease>
  → POST /api/v1/estates/{estateId}/leases/{id}/tenants

removeTenant(id, personId): Observable<Lease>
  → DELETE /api/v1/estates/{estateId}/leases/{id}/tenants/{personId}

adjustRent(id, req): Observable<Lease>
  → POST /api/v1/estates/{estateId}/leases/{id}/rent-adjustments

getAlerts(): Observable<LeaseAlert[]>
  → GET /api/v1/estates/{estateId}/leases/alerts
```

### Routing updates

```typescript
{ path: 'estates/:estateId/persons',
  loadChildren: () => import('./features/person/person.routes') },

{ path: 'estates/:estateId/leases',
  loadChildren: () => import('./features/lease/lease.routes') },
```

Update all `routerLink` and `router.navigate()` in person and lease components to include `estateId`.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Person must belong to the estate in the URL | Backend: `verifyPersonBelongsToEstate()` |
| Lease must belong to the estate in the URL | Backend: `verifyLeaseBelongsToEstate()` via housing unit → building |
| Tenant (person) must belong to the same estate as the lease | Backend: check in `LeaseService.addTenant()` |
| IBAN lookup scoped to estate during import | Backend: `PersonBankAccountRepository` + estate filter |
| VIEWER cannot create/edit/delete persons or leases | Backend: `@PreAuthorize("@security.isManagerOf(#estateId)")` |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify `FinancialTransactionController`, `BankAccountController`, `TagController`
- Do NOT add `estate_id` to any table other than `person`
- Do NOT modify Phase 1 or Phase 2 controllers or services beyond what is listed above

---

## ACCEPTANCE CRITERIA

- [ ] V019: `person.estate_id` column added with FK and index
- [ ] All person endpoints moved to `/api/v1/estates/{estateId}/persons/**`
- [ ] All lease endpoints moved to `/api/v1/estates/{estateId}/leases/**`
- [ ] All person bank account endpoints moved under `/api/v1/estates/{estateId}/persons/**`
- [ ] Accessing a person from the wrong estate → HTTP 403
- [ ] Accessing a lease whose housing unit belongs to a different estate → HTTP 403
- [ ] Adding a tenant (person) from a different estate to a lease → HTTP 403
- [ ] Dashboard `activeLeases` count returns real value
- [ ] IBAN lookup during import scoped to the estate of the transaction
- [ ] Frontend `PersonService` and `LeaseService` use `estateId` from `ActiveEstateService`
- [ ] All `routerLink` and `router.navigate()` calls in person/lease components include `estateId`
- [ ] VIEWER cannot create, edit, or delete persons or leases (403 from backend)

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
