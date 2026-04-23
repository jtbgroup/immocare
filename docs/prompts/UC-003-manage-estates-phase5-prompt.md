# ImmoCare — UC004_ESTATE_PLACEHOLDER Manage Estates — Phase 5 Implementation Prompt

I want to implement UC004_ESTATE_PLACEHOLDER - Manage Estates (Phase 5 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Prerequisite**: Phase 4 (V020) must be fully deployed and tested before starting this phase.
- **Flyway**: last migration is V020. Use **V021** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 5 of 6** of the multi-tenant estate migration.

| Phase | Flyway | Scope |
|---|---|---|
| Phase 1 | V017 | ✅ Done — Estate CRUD, membership, `app_user` migration |
| Phase 2 | V018 | ✅ Done — `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V019 | ✅ Done — `estate_id` on `person`; Persons & Leases scoped |
| Phase 4 | V020 | ✅ Done — `estate_id` on financial tables; Financial scoped |
| **Phase 5 (this prompt)** | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

## WHAT CHANGES IN THIS PHASE

- `estate_id UUID NOT NULL` added to `boiler_service_validity_rule`
- `estate_id UUID NOT NULL` added to `platform_config` — PK changes from `config_key` to `(estate_id, config_key)`
- `PlatformConfigController` routes migrated to `/api/v1/estates/{estateId}/config/**`
- `PlatformConfigService` and `BoilerServiceValidityRuleService` all queries filtered by `estateId`
- `EstateService.createEstate()` now seeds default config entries for every new estate
- `BoilerManagementService` reads alert threshold from estate-scoped config
- `AssetTypeMappingService` reads mappings from estate-scoped config
- Frontend `PlatformConfigService` updated to use `estateId`

## WHAT DOES NOT CHANGE

- `import_parser` — global, never estate-scoped. `PlatformConfigController` keeps a separate endpoint `/api/v1/import-parsers` unchanged.
- Phase 1–4 controllers and services — untouched.

---

## DATABASE MIGRATION — `V021__estate_scope_config.sql`

```sql
-- Use case: UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 5)

-- 1. Add estate_id to boiler_service_validity_rule
ALTER TABLE boiler_service_validity_rule
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE CASCADE;

CREATE INDEX idx_bsvr_estate ON boiler_service_validity_rule(estate_id);

-- Drop old unique constraint on valid_from alone; add estate-scoped uniqueness
ALTER TABLE boiler_service_validity_rule
    DROP CONSTRAINT IF EXISTS boiler_service_validity_rule_valid_from_key;

ALTER TABLE boiler_service_validity_rule
    ADD CONSTRAINT uq_bsvr_estate_valid_from UNIQUE (estate_id, valid_from);

-- 2. Migrate platform_config: add estate_id, change PK to (estate_id, config_key)
ALTER TABLE platform_config
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE CASCADE;

ALTER TABLE platform_config DROP CONSTRAINT platform_config_pkey;

ALTER TABLE platform_config
    ADD PRIMARY KEY (estate_id, config_key);

CREATE INDEX idx_platform_config_estate ON platform_config(estate_id);
```

> There is no existing data to migrate — the application starts fresh.
> Default config entries are seeded per estate at creation time (see EstateService below).

---

## DEFAULT CONFIG SEED PER ESTATE

Every new estate must receive the following config entries on creation. This seed is applied in `EstateService.createEstate()` inside the same `@Transactional` call, **not** in Flyway.

```java
private static final List<PlatformConfigSeed> DEFAULT_CONFIG = List.of(
    new PlatformConfigSeed("boiler.service.alert.threshold.months", "3", "INTEGER",
        "Months before service expiry to display a warning alert"),
    new PlatformConfigSeed("asset.type.subcategory.mapping.BOILER", "", "STRING",
        "Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping)"),
    new PlatformConfigSeed("asset.type.subcategory.mapping.FIRE_EXTINGUISHER", "", "STRING",
        "Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added"),
    new PlatformConfigSeed("asset.type.subcategory.mapping.METER", "", "STRING",
        "Subcategory ID to pre-fill when a METER asset link is added"),
    new PlatformConfigSeed("csv.import.delimiter", ";", "STRING", "CSV column delimiter"),
    new PlatformConfigSeed("csv.import.date_format", "dd/MM/yyyy", "STRING", "Date format in CSV"),
    new PlatformConfigSeed("csv.import.skip_header_rows", "1", "INTEGER", "Header rows to skip"),
    new PlatformConfigSeed("csv.import.col.date", "0", "INTEGER", "Column index for date"),
    new PlatformConfigSeed("csv.import.col.amount", "1", "INTEGER", "Column index for amount"),
    new PlatformConfigSeed("csv.import.col.description", "2", "INTEGER", "Column index for description"),
    new PlatformConfigSeed("csv.import.col.counterparty_account", "3", "INTEGER", "Column index for counterparty IBAN"),
    new PlatformConfigSeed("csv.import.col.external_reference", "4", "INTEGER", "Column index for bank reference"),
    new PlatformConfigSeed("csv.import.col.bank_account", "5", "INTEGER", "Column index for own IBAN"),
    new PlatformConfigSeed("csv.import.col.value_date", "-1", "INTEGER", "Column index for value date (-1 = absent)"),
    new PlatformConfigSeed("csv.import.suggestion.confidence.threshold", "3", "INTEGER",
        "Min confidence for tag suggestion")
);
```

Also seed the default boiler service validity rule per estate:
```java
// On estate creation, insert:
BoilerServiceValidityRule defaultRule = new BoilerServiceValidityRule();
defaultRule.setEstate(estate);
defaultRule.setValidFrom(LocalDate.of(1900, 1, 1));
defaultRule.setValidityDurationMonths(24);
defaultRule.setDescription("Default — 2 years");
boilerServiceValidityRuleRepository.save(defaultRule);
```

---

## BACKEND

### Modified Entity: `PlatformConfig`

- Remove `@Id` from `configKey` alone.
- Add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL).
- Change `@Id` to composite PK via `@IdClass PlatformConfigId`:

```java
@IdClass(PlatformConfigId.class)
public class PlatformConfig {
    @Id
    @ManyToOne
    @JoinColumn(name = "estate_id")
    private Estate estate;

    @Id
    private String configKey;

    // remaining fields unchanged
}

public class PlatformConfigId implements Serializable {
    private UUID estate;   // matches estate.id type
    private String configKey;
}
```

### Modified Entity: `BoilerServiceValidityRule`

Add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL). No other changes.

### Modified Repository: `PlatformConfigRepository`

```java
// PK is now (estateId, configKey)
Optional<PlatformConfig> findByEstateIdAndConfigKey(UUID estateId, String configKey);
List<PlatformConfig> findByEstateIdOrderByConfigKeyAsc(UUID estateId);
boolean existsByEstateIdAndConfigKey(UUID estateId, String configKey);
```

### Modified Repository: `BoilerServiceValidityRuleRepository`

```java
List<BoilerServiceValidityRule> findByEstateIdOrderByValidFromDesc(UUID estateId);
boolean existsByEstateIdAndValidFrom(UUID estateId, LocalDate validFrom);

Optional<BoilerServiceValidityRule> findTopByEstateIdAndValidFromLessThanEqualOrderByValidFromDesc(
    UUID estateId, LocalDate date);
```

### Modified Service: `PlatformConfigService`

All methods receive `estateId` as first parameter:

```java
List<PlatformConfigDTO> getAllConfigs(UUID estateId);
PlatformConfigDTO getConfig(UUID estateId, String key);
int getIntValue(UUID estateId, String key);   // used by BoilerManagementService

@Transactional
PlatformConfigDTO updateConfig(UUID estateId, String key,
    UpdatePlatformConfigRequest req, AppUser currentUser);

List<AssetTypeMappingDTO> getAssetTypeMappings(UUID estateId);

@Transactional
AssetTypeMappingDTO updateAssetTypeMapping(UUID estateId, String assetType,
    UpdateAssetTypeMappingRequest req, AppUser currentUser);
```

### Modified Service: `BoilerServiceValidityRuleService`

All methods receive `estateId`:

```java
List<BoilerServiceValidityRuleDTO> getAllRules(UUID estateId);

@Transactional
BoilerServiceValidityRuleDTO addRule(UUID estateId,
    AddBoilerServiceValidityRuleRequest req, AppUser currentUser);

LocalDate calculateValidUntil(UUID estateId, LocalDate serviceDate);
// findTopByEstateIdAndValidFromLessThanEqualOrderByValidFromDesc(estateId, serviceDate)
```

### Modified Service: `BoilerManagementService`

Update call to `PlatformConfigService.getIntValue()` to pass `estateId`:

```java
// Resolve estateId from boiler → housingUnit → building → estate
UUID estateId = boiler.getHousingUnit().getBuilding().getEstate().getId();
int threshold = platformConfigService.getIntValue(estateId,
    "boiler.service.alert.threshold.months");
```

### Modified Service: `EstateService.createEstate()`

After saving the estate entity, seed config and default validity rule in the same transaction:

```java
@Transactional
EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId) {
    // ... existing validation and save ...

    // Seed default platform config
    DEFAULT_CONFIG.forEach(seed -> {
        PlatformConfig config = new PlatformConfig();
        config.setEstate(estate);
        config.setConfigKey(seed.key());
        config.setConfigValue(seed.value());
        config.setValueType(seed.valueType());
        config.setDescription(seed.description());
        platformConfigRepository.save(config);
    });

    // Seed default boiler validity rule
    BoilerServiceValidityRule rule = new BoilerServiceValidityRule();
    rule.setEstate(estate);
    rule.setValidFrom(LocalDate.of(1900, 1, 1));
    rule.setValidityDurationMonths(24);
    rule.setDescription("Default — 2 years");
    boilerServiceValidityRuleRepository.save(rule);

    // ... assign firstManager if present ...
}
```

### Modified Controller: `PlatformConfigController`

Change all routes to `/api/v1/estates/{estateId}/config/**`:

| Method | Path | Story |
|--------|------|-------|
| GET | `/api/v1/estates/{estateId}/config/settings` | UC013.001 |
| PUT | `/api/v1/estates/{estateId}/config/settings/{key}` | UC013.004 |
| GET | `/api/v1/estates/{estateId}/config/boiler-validity-rules` | UC013.003 |
| POST | `/api/v1/estates/{estateId}/config/boiler-validity-rules` | UC013.002 |
| GET | `/api/v1/estates/{estateId}/config/asset-type-mappings` | UC012.001 |
| PUT | `/api/v1/estates/{estateId}/config/asset-type-mappings/{assetType}` | UC012.001 |

Add `@PreAuthorize("@security.isManagerOf(#estateId)")` on all mutating methods.
Add `@PreAuthorize("@security.isMemberOf(#estateId)")` on GET methods.

**Keep `/api/v1/import-parsers` unchanged and in a separate controller** — it is global and PLATFORM_ADMIN managed.

---

## FRONTEND

### Modified Service: `PlatformConfigService`

Inject `ActiveEstateService`. Update all URLs:

```typescript
private get estateId(): string {
  return this.activeEstateService.activeEstateId()!;
}

getSettings(): Observable<PlatformConfigDTO[]>
  → GET /api/v1/estates/{estateId}/config/settings

updateSetting(key, req): Observable<PlatformConfigDTO>
  → PUT /api/v1/estates/{estateId}/config/settings/{key}

getValidityRules(): Observable<BoilerServiceValidityRuleDTO[]>
  → GET /api/v1/estates/{estateId}/config/boiler-validity-rules

addValidityRule(req): Observable<BoilerServiceValidityRuleDTO>
  → POST /api/v1/estates/{estateId}/config/boiler-validity-rules

getAssetTypeMappings(): Observable<AssetTypeMappingDTO[]>
  → GET /api/v1/estates/{estateId}/config/asset-type-mappings

updateAssetTypeMapping(assetType, req): Observable<AssetTypeMappingDTO>
  → PUT /api/v1/estates/{estateId}/config/asset-type-mappings/{assetType}
```

### Routing updates

```typescript
{ path: 'estates/:estateId/admin/platform-settings',
  component: PlatformSettingsComponent,
  canActivate: [EstateGuard] },
```

Update `PlatformSettingsComponent` navigation link in sidebar from `/admin/platform-settings` to `/estates/{estateId}/admin/platform-settings`.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Config key scoped to estate — `(estate_id, config_key)` is the unique key | Backend: composite PK |
| Boiler validity rule unique per `(estate_id, valid_from)` | Backend: `uq_bsvr_estate_valid_from` constraint |
| Boiler alert threshold read from estate-scoped config | Backend: `BoilerManagementService` passes `estateId` |
| Asset type mapping stored in estate-scoped platform_config | Backend: `PlatformConfigService` passes `estateId` |
| New estate always has default config seeded atomically | Backend: `EstateService.createEstate()` @Transactional |
| Default validity rule seeded for every new estate | Backend: same transaction as config seed |
| VIEWER cannot edit config or validity rules | Backend: `@PreAuthorize("@security.isManagerOf(#estateId)")` |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify `ImportParserController` — it remains global
- Do NOT modify Phase 1–4 controllers or services beyond what is listed above
- Do NOT add `estate_id` to any table not listed in V021

---

## ACCEPTANCE CRITERIA

- [ ] V021: `estate_id` added to `boiler_service_validity_rule` with scoped unique constraint
- [ ] V021: `estate_id` added to `platform_config`; PK changed to `(estate_id, config_key)`
- [ ] All platform config endpoints moved to `/api/v1/estates/{estateId}/config/**`
- [ ] Creating a new estate automatically seeds all default config entries and the default validity rule
- [ ] Boiler alert threshold is read from the estate-scoped config (not a global value)
- [ ] Valid-until calculation uses the estate-scoped validity rule
- [ ] Two estates can have different boiler alert thresholds independently
- [ ] Asset type mappings are per-estate and independent between estates
- [ ] VIEWER cannot update config or add validity rules (403 from backend)
- [ ] Frontend `PlatformConfigService` uses `estateId` from `ActiveEstateService`
- [ ] `/api/v1/import-parsers` remains unchanged and globally accessible

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
