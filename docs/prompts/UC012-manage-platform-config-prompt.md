# ImmoCare — UC012 Manage Platform Configuration — Implementation Prompt

I want to implement Use Case UC012 - Manage Platform Configuration for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases
- **Implement this UC before UC011** — boiler alert threshold and validity rules are read from platform_config

---

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US067 | View Platform Settings | MUST HAVE | 2 |
| US068 | Add Boiler Service Validity Rule | MUST HAVE | 3 |
| US069 | View Boiler Service Validity Rules History | MUST HAVE | 2 |
| US070 | Update General Settings | MUST HAVE | 3 |

---

## DATABASE MIGRATION — `V012__create_platform_config.sql`

```sql
-- Platform configuration (key-value store)
CREATE TABLE platform_config (
    config_key    VARCHAR(100) PRIMARY KEY,
    config_value  VARCHAR(500) NOT NULL,
    value_type    VARCHAR(20)  NOT NULL, -- STRING, INTEGER, BOOLEAN, DECIMAL
    description   TEXT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

-- Boiler service validity rules (temporal, append-only)
CREATE TABLE boiler_service_validity_rule (
    id                        BIGSERIAL PRIMARY KEY,
    valid_from                DATE         NOT NULL UNIQUE,
    validity_duration_months  INTEGER      NOT NULL CHECK (validity_duration_months > 0),
    description               TEXT,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by                BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

-- Seeds: general settings
INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('boiler.service.alert.threshold.months', '3', 'INTEGER',
     'Number of months before service expiry to display a warning alert'),
    ('asset.type.subcategory.mapping.BOILER', '', 'STRING',
     'Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping)'),
    ('asset.type.subcategory.mapping.FIRE_EXTINGUISHER', '', 'STRING',
     'Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added (empty = no mapping)'),
    ('asset.type.subcategory.mapping.METER', '', 'STRING',
     'Subcategory ID to pre-fill when a METER asset link is added (empty = no mapping)');

-- Seed: default validity rule
INSERT INTO boiler_service_validity_rule (valid_from, validity_duration_months, description)
VALUES ('1900-01-01', 24, 'Default — 2 years (current regulation)');
```

---

## BACKEND

### Entities

**`PlatformConfig`** — table `platform_config`:
- Fields: `configKey` (String, @Id), `configValue` (String), `valueType` (String), `description` (String), `updatedAt`, `updatedBy` (@ManyToOne AppUser, nullable).
- No @PreUpdate auto-set — set manually in service to capture updatedBy.

**`BoilerServiceValidityRule`** — table `boiler_service_validity_rule`:
- Fields: `id`, `validFrom` (LocalDate), `validityDurationMonths` (Integer), `description`, `createdAt` (@PrePersist), `createdBy` (@ManyToOne AppUser, nullable).
- No @PreUpdate.

### DTOs

**`PlatformConfigDTO`**:
```java
record PlatformConfigDTO(
    String configKey, String configValue, String valueType,
    String description, LocalDateTime updatedAt, String updatedByUsername
) {}
```

**`UpdatePlatformConfigRequest`**:
```java
record UpdatePlatformConfigRequest(@NotBlank String configValue) {}
```

**`BoilerServiceValidityRuleDTO`**:
```java
record BoilerServiceValidityRuleDTO(
    Long id, LocalDate validFrom, Integer validityDurationMonths,
    String description, boolean isCurrent, boolean isScheduled,
    LocalDateTime createdAt, String createdByUsername
) {}
// isCurrent  = validFrom <= today AND no newer rule exists
// isScheduled = validFrom > today
```

**`AddBoilerServiceValidityRuleRequest`**:
```java
record AddBoilerServiceValidityRuleRequest(
    @NotNull LocalDate validFrom,
    @NotNull @Min(1) Integer validityDurationMonths,
    String description
) {}
```

**`AssetTypeMappingDTO`**:
```java
record AssetTypeMappingDTO(
    String assetType,
    String assetTypeLabel,
    Long subcategoryId,       // null if not mapped
    String subcategoryName,   // null if not mapped
    Long categoryId,          // null if not mapped
    String categoryName       // null if not mapped
) {}
```

**`UpdateAssetTypeMappingRequest`**:
```java
record UpdateAssetTypeMappingRequest(
    Long subcategoryId   // null = clear mapping
) {}
```

### Repositories

**`PlatformConfigRepository`** extends `JpaRepository<PlatformConfig, String>`.

**`BoilerServiceValidityRuleRepository`** extends `JpaRepository<BoilerServiceValidityRule, Long>`:
```java
List<BoilerServiceValidityRule> findAllByOrderByValidFromDesc();
boolean existsByValidFrom(LocalDate validFrom);
Optional<BoilerServiceValidityRule> findTopByValidFromLessThanEqualOrderByValidFromDesc(LocalDate date);
```

### Services

**`PlatformConfigService`** (`@Service`, `@Transactional(readOnly=true)`):
```java
List<PlatformConfigDTO> getAllConfigs();
PlatformConfigDTO getConfig(String key);         // throws PlatformConfigNotFoundException
int getIntValue(String key);                     // utility used by BoilerManagementService

@Transactional
PlatformConfigDTO updateConfig(String key, UpdatePlatformConfigRequest req, AppUser currentUser);
// validate value matches valueType; update configValue, updatedAt, updatedBy

List<AssetTypeMappingDTO> getAssetTypeMappings();
// reads asset.type.subcategory.mapping.* keys
// resolves subcategory name + category from TagSubcategoryRepository if value non-empty

@Transactional
AssetTypeMappingDTO updateAssetTypeMapping(String assetType, UpdateAssetTypeMappingRequest req, AppUser currentUser);
// assetType must be one of: BOILER, FIRE_EXTINGUISHER, METER
// if subcategoryId null → set configValue = ''
// if subcategoryId set → validate subcategory direction is EXPENSE or BOTH
//                     → set configValue = subcategoryId.toString()
// updates updatedAt + updatedBy
```

**`BoilerServiceValidityRuleService`** (`@Service`, `@Transactional(readOnly=true)`):
```java
List<BoilerServiceValidityRuleDTO> getAllRules();

@Transactional
BoilerServiceValidityRuleDTO addRule(AddBoilerServiceValidityRuleRequest req, AppUser currentUser);
// check uniqueness on validFrom → throw BoilerServiceValidityRuleException if duplicate

BoilerServiceValidityRule getRuleForDate(LocalDate serviceDate);
// findTopByValidFromLessThanEqualOrderByValidFromDesc(serviceDate)
// throws if none found (should never happen given seed)

LocalDate calculateValidUntil(LocalDate serviceDate);
// getRuleForDate(serviceDate) → serviceDate.plusMonths(validityDurationMonths)
```

### Exceptions

- `PlatformConfigNotFoundException` → 404
- `BoilerServiceValidityRuleException` (duplicate validFrom) → 409
- `InvalidConfigValueException` → 400
- `InvalidAssetTypeMappingException` (subcategory direction incompatible) → 400

### Controller: `PlatformConfigController`

No @RequestMapping prefix; full path per method.

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| GET | `/api/v1/config/settings` | — | `List<PlatformConfigDTO>` 200 | US067, US070 |
| PUT | `/api/v1/config/settings/{key}` | `UpdatePlatformConfigRequest` | `PlatformConfigDTO` 200 | US070 |
| GET | `/api/v1/config/boiler-validity-rules` | — | `List<BoilerServiceValidityRuleDTO>` 200 | US069 |
| POST | `/api/v1/config/boiler-validity-rules` | `AddBoilerServiceValidityRuleRequest` | `BoilerServiceValidityRuleDTO` 201 | US068 |
| GET | `/api/v1/config/asset-type-mappings` | — | `List<AssetTypeMappingDTO>` 200 | US070 |
| PUT | `/api/v1/config/asset-type-mappings/{assetType}` | `UpdateAssetTypeMappingRequest` | `AssetTypeMappingDTO` 200 | US070 |

### GlobalExceptionHandler — add these handlers

```java
@ExceptionHandler(PlatformConfigNotFoundException.class)
public ResponseEntity<ErrorResponse> handlePlatformConfigNotFound(PlatformConfigNotFoundException ex) {
    return notFound("Configuration key not found", ex.getMessage());
}

@ExceptionHandler(BoilerServiceValidityRuleException.class)
public ResponseEntity<ErrorResponse> handleBoilerValidityRuleDuplicate(BoilerServiceValidityRuleException ex) {
    return conflict("Validity rule conflict", ex.getMessage());
}

@ExceptionHandler(InvalidConfigValueException.class)
public ResponseEntity<ErrorResponse> handleInvalidConfigValue(InvalidConfigValueException ex) {
    return badRequest("Invalid config value", ex.getMessage());
}

@ExceptionHandler(InvalidAssetTypeMappingException.class)
public ResponseEntity<ErrorResponse> handleInvalidAssetTypeMapping(InvalidAssetTypeMappingException ex) {
    return badRequest("Invalid asset type mapping", ex.getMessage());
}
```

---

## FRONTEND

### Models — `platform-config.model.ts`

```typescript
export type AssetType = 'BOILER' | 'FIRE_EXTINGUISHER' | 'METER';

export interface PlatformConfigDTO {
  configKey: string; configValue: string; valueType: string;
  description: string | null; updatedAt: string; updatedByUsername: string | null;
}

export interface BoilerServiceValidityRuleDTO {
  id: number; validFrom: string; validityDurationMonths: number;
  description: string | null; isCurrent: boolean; isScheduled: boolean;
  createdAt: string; createdByUsername: string | null;
}

export interface AssetTypeMappingDTO {
  assetType: AssetType; assetTypeLabel: string;
  subcategoryId: number | null; subcategoryName: string | null;
  categoryId: number | null; categoryName: string | null;
}

export interface UpdatePlatformConfigRequest { configValue: string; }
export interface AddBoilerServiceValidityRuleRequest {
  validFrom: string; validityDurationMonths: number; description?: string;
}
export interface UpdateAssetTypeMappingRequest { subcategoryId: number | null; }

export const ASSET_TYPE_LABELS: Record<AssetType, string> = {
  BOILER: 'Boiler', FIRE_EXTINGUISHER: 'Fire Extinguisher', METER: 'Meter'
};
```

### Service — `platform-config.service.ts`

```typescript
getSettings(): Observable<PlatformConfigDTO[]>
updateSetting(key: string, req: UpdatePlatformConfigRequest): Observable<PlatformConfigDTO>
getValidityRules(): Observable<BoilerServiceValidityRuleDTO[]>
addValidityRule(req: AddBoilerServiceValidityRuleRequest): Observable<BoilerServiceValidityRuleDTO>
getAssetTypeMappings(): Observable<AssetTypeMappingDTO[]>
updateAssetTypeMapping(assetType: AssetType, req: UpdateAssetTypeMappingRequest): Observable<AssetTypeMappingDTO>
```

### Component — `PlatformSettingsComponent`

Standalone, routed at `/admin/platform-settings`.

**Three sections:**

**1. Boiler Service Validity Rules (US068, US069):**
- Table: validFrom, duration (months), description, created by, created at, badge (Current / Scheduled).
- "Add Rule" button → inline form: validFrom date picker + duration + description.
- Rules are read-only after creation — no edit/delete buttons.

**2. General Settings (US070 AC1–AC6):**
- List of config entries excluding `asset.type.subcategory.mapping.*` keys (those are shown in section 3).
- Each row: label (derived from description), current value, "Edit" button, updated at + updated by.
- "Edit" → inline form, value pre-filled. Save → PUT. Cancel → restore.
- Integer validation client-side for INTEGER type configs.

**3. Asset Type Mappings (US070 AC7–AC13):**
- Table with one row per asset type (BOILER, FIRE_EXTINGUISHER, METER).
- Columns: asset type label, mapped subcategory ("Category / Subcategory" or "Not mapped" in italic).
- "Edit" button per row → inline form:
  - Category dropdown (all categories, ordered alphabetically)
  - Subcategory dropdown (filtered to selected category, direction EXPENSE or BOTH only)
  - "Clear mapping" button
- Save → PUT `/api/v1/config/asset-type-mappings/{assetType}`.
- Cancel → restore original value.

**Navigation:** Add "Platform Settings" link in Administration section of sidebar alongside "Users". Route: `/admin/platform-settings`.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Validity rules append-only (no edit/delete) | Backend: no PUT/DELETE on rules endpoint |
| valid_from must be unique | Backend: existsByValidFrom check |
| validityDurationMonths > 0 | Backend: @Min(1) + DB CHECK |
| Alert threshold must be positive integer | Backend: InvalidConfigValueException + frontend validation |
| Asset mapping subcategory direction must be EXPENSE or BOTH | Backend: InvalidAssetTypeMappingException |
| Asset mapping configValue stores subcategoryId as string | Backend: cast to Long at runtime; empty string = no mapping |

---

## ACCEPTANCE CRITERIA

- [ ] Platform Settings page accessible from admin nav with three sections
- [ ] Add validity rule: uniqueness on valid_from enforced, no retroactive recalculation
- [ ] Rules history: Current / Scheduled badges correct, seed rule visible
- [ ] Update alert threshold: integer validation, takes effect immediately
- [ ] Asset type mappings: all three asset types shown, subcategory filtered to EXPENSE/BOTH
- [ ] Mapping cleared → shows "Not mapped"
- [ ] Mapping saved → pre-fills subcategory when asset link added to transaction (verified via UC014)
- [ ] All US067–US070 acceptance criteria verified

**Last Updated:** 2026-04-04 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
