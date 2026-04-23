# ImmoCare — UC004_ESTATE_PLACEHOLDER Manage Estates — Phase 6 Implementation Prompt

I want to implement UC004_ESTATE_PLACEHOLDER - Manage Estates (Phase 6 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Prerequisite**: Phase 5 (V021) must be fully deployed and tested before starting this phase.
- **Flyway**: no new migration in this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 6 of 6** — the final phase of the multi-tenant estate migration.

| Phase | Flyway | Scope |
|---|---|---|
| Phase 1 | V017 | ✅ Done — Estate CRUD, membership, `app_user` migration |
| Phase 2 | V018 | ✅ Done — `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V019 | ✅ Done — `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V020 | ✅ Done — `estate_id` on financial tables; Financial scoped |
| Phase 5 | V021 | ✅ Done — `estate_id` on config tables; per-estate config seeded |
| **Phase 6 (this prompt)** | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

## WHAT CHANGES IN THIS PHASE

- `EstateDashboardDTO` fully populated with real data (all counts, pending alerts)
- VIEWER write-blocking enforced systematically in the frontend via `ActiveEstateService.canEdit()`
- All existing alert computations (boiler, fire extinguisher, lease end, indexation) wired into the dashboard
- Cross-estate access integration tests added
- No database schema changes

---

## BACKEND

### Enriched `EstateService.getDashboard()`

Replace all zero stubs with real queries. All queries must be scoped to `estateId`:

```java
@Transactional(readOnly = true)
public EstateDashboardDTO getDashboard(UUID estateId) {
    Estate estate = estateRepository.findById(estateId)
        .orElseThrow(EstateNotFoundException::new);

    // Counts (all already implemented in their respective repositories)
    int totalBuildings = (int) buildingRepository.countByEstateId(estateId);
    int totalUnits     = (int) housingUnitRepository.countByBuilding_Estate_Id(estateId);
    int activeLeases   = (int) leaseRepository
        .countByHousingUnit_Building_Estate_IdAndStatus(estateId, "ACTIVE");

    // Pending alerts
    int boilerAlerts          = computeBoilerAlerts(estateId);
    int fireExtAlerts         = computeFireExtinguisherAlerts(estateId);
    int leaseEndAlerts        = computeLeaseEndAlerts(estateId);
    int indexationAlerts      = computeIndexationAlerts(estateId);

    return new EstateDashboardDTO(
        estate.getId(), estate.getName(),
        totalBuildings, totalUnits, activeLeases,
        new EstatePendingAlertsDTO(
            boilerAlerts, fireExtAlerts, leaseEndAlerts, indexationAlerts)
    );
}
```

#### Alert computation methods

**`computeBoilerAlerts(UUID estateId)`**
- Load all active boilers across the estate (via `housing_unit → building.estate_id`)
- For each, check service status using `PlatformConfigService.getIntValue(estateId, "boiler.service.alert.threshold.months")`
- Count those with status `EXPIRED` or `EXPIRING_SOON`

```java
private int computeBoilerAlerts(UUID estateId) {
    int threshold = platformConfigService.getIntValue(estateId,
        "boiler.service.alert.threshold.months");
    return (int) boilerRepository
        .findActiveByEstateId(estateId)   // new query — see below
        .stream()
        .filter(b -> {
            ServiceStatus status = boilerManagementService.computeServiceStatus(b, threshold);
            return status == ServiceStatus.EXPIRED || status == ServiceStatus.EXPIRING_SOON;
        })
        .count();
}
```

New repository method needed:
```java
// BoilerRepository
@Query("""
    SELECT b FROM Boiler b
    WHERE b.housingUnit.building.estate.id = :estateId
    AND b.removalDate IS NULL
    """)
List<Boiler> findActiveByEstateId(@Param("estateId") UUID estateId);
```

**`computeFireExtinguisherAlerts(UUID estateId)`**
- Fire extinguisher alert = extinguisher with no revisions OR latest revision older than 1 year
- Load all extinguishers for the estate via `building.estate_id`

```java
private int computeFireExtinguisherAlerts(UUID estateId) {
    LocalDate threshold = LocalDate.now().minusYears(1);
    return (int) fireExtinguisherRepository
        .findByBuildingEstateId(estateId)  // new query — see below
        .stream()
        .filter(ext -> {
            if (ext.getRevisions().isEmpty()) return true;
            LocalDate latest = ext.getRevisions().get(0).getRevisionDate();
            return latest.isBefore(threshold);
        })
        .count();
}
```

New repository method:
```java
// FireExtinguisherRepository
@Query("""
    SELECT e FROM FireExtinguisher e
    LEFT JOIN FETCH e.revisions
    WHERE e.building.estate.id = :estateId
    """)
List<FireExtinguisher> findByBuildingEstateId(@Param("estateId") UUID estateId);
```

**`computeLeaseEndAlerts(UUID estateId)`**
- Reuse existing `LeaseService.getAlerts(estateId)` — filter for `endNoticeAlertActive = true`

```java
private int computeLeaseEndAlerts(UUID estateId) {
    return (int) leaseService.getAlerts(estateId).stream()
        .filter(LeaseAlertDTO::endNoticeAlertActive)
        .count();
}
```

**`computeIndexationAlerts(UUID estateId)`**
- Same — filter for `indexationAlertActive = true`

```java
private int computeIndexationAlerts(UUID estateId) {
    return (int) leaseService.getAlerts(estateId).stream()
        .filter(LeaseAlertDTO::indexationAlertActive)
        .count();
}
```

---

## FRONTEND — VIEWER ENFORCEMENT

### `ActiveEstateService` — verify `canEdit()` signal is used

Confirm the `canEdit()` computed signal is wired into every component that shows action buttons. The pattern to apply consistently across **all** feature components:

```typescript
// In every component that has Create/Edit/Delete buttons:
protected readonly canEdit = inject(ActiveEstateService).canEdit;

// In template:
@if (canEdit()) {
  <button mat-raised-button color="primary" (click)="openCreateForm()">Add</button>
}
@if (canEdit()) {
  <button mat-icon-button (click)="edit(item)"><mat-icon>edit</mat-icon></button>
}
@if (canEdit()) {
  <button mat-icon-button color="warn" (click)="delete(item)"><mat-icon>delete</mat-icon></button>
}
```

### Components to audit and update

Apply the `canEdit()` guard to **every** Create/Edit/Delete button in the following components. Do not regenerate the components — only add the `canEdit()` checks where missing:

| Component | Buttons to guard |
|---|---|
| `BuildingListComponent` | Create, Edit, Delete |
| `BuildingDetailsComponent` | Edit, Delete |
| `HousingUnitListComponent` | Create, Edit, Delete |
| `HousingUnitDetailsComponent` | Edit, Delete |
| `RoomSectionComponent` | Add, Edit, Delete, Batch Add |
| `PebSectionComponent` | Add PEB Score |
| `RentSectionComponent` | Set Rent, Update Rent |
| `MeterSectionComponent` | Add, Replace, Remove |
| `BoilerSectionComponent` | Add, Replace, + Add Service |
| `FireExtinguisherSectionComponent` | Add, Edit, Delete, + Add Revision, Delete Revision |
| `PersonListComponent` | Create, Edit, Delete |
| `PersonDetailsComponent` | Edit, Delete |
| `PersonBankAccountSectionComponent` | Add, Edit, Delete |
| `LeaseSectionComponent` | Create Lease |
| `LeaseDetailsComponent` | Edit, Activate, Finish, Cancel |
| `TenantSectionComponent` | Add Tenant, Remove Tenant |
| `RentAdjustmentSectionComponent` | Add Adjustment |
| `TransactionListComponent` | Create, Edit, Delete, Bulk status change |
| `TransactionImportComponent` | Upload, Confirm Import |
| `TransactionSettingsComponent` | All create/edit/delete in tags and bank accounts |
| `PlatformSettingsComponent` | Edit settings, Add validity rule, Edit mapping |
| `EstateMemberListComponent` | Add Member, Edit Role, Remove (already guarded in Phase 1 — verify) |

### Dashboard component enrichment

`EstateDashboardComponent` — update to display real alert counts with color coding:

```typescript
// Alert count badge colors
// 0 alerts → grey / neutral
// > 0 alerts → orange (warning) for boiler/fireExt/indexation
// > 0 alerts → red (danger) for leaseEnd
```

Add click handler on each alert card to navigate to the relevant filtered list:
- Boiler alerts → not yet routed (show tooltip "View in Housing Units")
- Fire extinguisher alerts → not yet routed (show tooltip "View in Buildings")
- Lease end alerts → `/estates/{estateId}/leases/alerts`
- Indexation alerts → `/estates/{estateId}/leases/alerts`

---

## INTEGRATION TESTS (Backend)

Add the following Spring Boot integration tests using `@SpringBootTest` + `@Transactional`:

### `EstateAccessIntegrationTest`

```java
// Setup: create 2 estates (A and B), 1 MANAGER per estate, 1 shared VIEWER in both
// Test cases:

@Test void managerA_canReadBuildingsInEstateA()
@Test void managerA_cannotReadBuildingsInEstateB()          // expects 403
@Test void managerA_canCreateBuildingInEstateA()
@Test void managerA_cannotCreateBuildingInEstateB()         // expects 403
@Test void viewerA_canReadBuildingsInEstateA()
@Test void viewerA_cannotCreateBuildingInEstateA()          // expects 403
@Test void platformAdmin_canReadBuildingsInEstateA()
@Test void platformAdmin_canReadBuildingsInEstateB()
@Test void platformAdmin_canCreateBuildingInEstateA()
@Test void managerA_cannotReadPersonsInEstateB()            // expects 403
@Test void managerA_cannotReadTransactionsInEstateB()       // expects 403
@Test void transactionReferenceSequenceScopedPerEstate()    // TXN-2026-00001 can exist in both A and B
@Test void fingerprintDuplicationScopedPerEstate()          // same fingerprint = 2 valid imports in 2 estates
@Test void configValuesIndependentPerEstate()               // threshold in A != threshold in B
```

### `EstateCreationIntegrationTest`

```java
@Test void createEstate_seedsAllDefaultConfigEntries()
@Test void createEstate_seedsDefaultBoilerValidityRule()
@Test void createEstate_withFirstManager_assignsManagerRole()
@Test void createEstate_duplicateName_returns409()
@Test void deleteEstate_withBuildings_returns409()
@Test void deleteEstate_empty_cascadesMembers()
```

### `EstateMembershipIntegrationTest`

```java
@Test void removeMember_lastManager_returns409()
@Test void updateMemberRole_selfDemotion_returns409()
@Test void removeMember_self_returns409()
@Test void addMember_alreadyMember_returns409()
@Test void viewer_cannotAccessMemberList_returns403()
```

---

## ACCEPTANCE CRITERIA

- [ ] Dashboard displays real counts for buildings, units, active leases, and all 4 alert types
- [ ] Boiler alert count reflects estates-scoped active boilers with EXPIRED or EXPIRING_SOON status
- [ ] Fire extinguisher alert count reflects extinguishers with no revisions or last revision > 1 year ago
- [ ] Lease end alert count and indexation alert count match `GET /leases/alerts` response
- [ ] Alert count badges: 0 = neutral, >0 = orange/red as appropriate
- [ ] All Create/Edit/Delete buttons hidden for VIEWER across all feature components
- [ ] VIEWER can navigate and read all data but cannot trigger any mutation (buttons absent, not just disabled)
- [ ] All `EstateAccessIntegrationTest` cases pass
- [ ] All `EstateCreationIntegrationTest` cases pass
- [ ] All `EstateMembershipIntegrationTest` cases pass
- [ ] Cross-estate data isolation verified: no data from estate B visible when authenticated as a user of estate A only
- [ ] Transaction reference sequence confirmed independent per estate
- [ ] Fingerprint deduplication confirmed scoped per estate

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
