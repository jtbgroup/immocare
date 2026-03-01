# ImmoCare — UC011 + UC012 Manage Boilers & Platform Configuration — Implementation Prompt

I want to implement Use Cases UC011 (Manage Boilers) and UC012 (Manage Platform Configuration) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases
- **Implement UC012 first** (platform config + validity rules), then UC011 (boilers) which depends on it

---

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US067 | View Platform Settings | MUST HAVE | 2 |
| US068 | Add Boiler Service Validity Rule | MUST HAVE | 3 |
| US069 | View Boiler Service Validity Rules History | MUST HAVE | 2 |
| US070 | Update General Alert Settings | MUST HAVE | 2 |
| US060 | Add Boiler to Housing Unit | MUST HAVE | 3 |
| US061 | View Active Boiler | MUST HAVE | 2 |
| US062 | Replace Boiler | MUST HAVE | 3 |
| US063 | View Boiler History | SHOULD HAVE | 2 |
| US064 | Add Boiler Service Record | MUST HAVE | 3 |
| US065 | View Boiler Service History | MUST HAVE | 2 |
| US066 | View Boiler Service Validity Alert | MUST HAVE | 2 |

---

## DATABASE MIGRATION — `V012__create_boiler_and_platform_config.sql`

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

-- Boiler per housing unit (append-only history)
CREATE TABLE boiler (
    id                BIGSERIAL PRIMARY KEY,
    housing_unit_id   BIGINT       NOT NULL REFERENCES housing_unit(id) ON DELETE RESTRICT,
    brand             VARCHAR(100) NOT NULL,
    model             VARCHAR(100),
    fuel_type         VARCHAR(50),
    installation_date DATE         NOT NULL,
    removal_date      DATE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by        BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_boiler_housing_unit ON boiler(housing_unit_id);
CREATE INDEX idx_boiler_active ON boiler(housing_unit_id) WHERE removal_date IS NULL;

-- Boiler service records (append-only)
CREATE TABLE boiler_service (
    id           BIGSERIAL PRIMARY KEY,
    boiler_id    BIGINT    NOT NULL REFERENCES boiler(id) ON DELETE RESTRICT,
    service_date DATE      NOT NULL,
    valid_until  DATE      NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT    REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_boiler_service_boiler ON boiler_service(boiler_id);

-- Seeds
INSERT INTO platform_config (config_key, config_value, value_type, description)
VALUES ('boiler.service.alert.threshold.months', '3', 'INTEGER',
        'Number of months before service expiry to display a warning alert');

INSERT INTO boiler_service_validity_rule (valid_from, validity_duration_months, description)
VALUES ('1900-01-01', 24, 'Default — 2 years (current regulation)');
```

---

## PART 1 — UC012: PLATFORM CONFIGURATION

### Backend

1. **Entity: `PlatformConfig`** — table `platform_config`.
   - Fields: `configKey` (String, @Id), `configValue` (String), `valueType` (String), `description` (String), `updatedAt` (@PrePersist + @PreUpdate), `updatedBy` (@ManyToOne AppUser).
   - No `@PreUpdate` auto-set on updatedAt — set manually in service to also capture updatedBy.

2. **Entity: `BoilerServiceValidityRule`** — table `boiler_service_validity_rule`.
   - Fields: id, validFrom (LocalDate), validityDurationMonths (Integer), description, createdAt (@PrePersist), createdBy (@ManyToOne AppUser).
   - No @PreUpdate.

3. **DTOs:**
   - `PlatformConfigDTO` — `{ configKey, configValue, valueType, description, updatedAt, updatedByUsername }`
   - `UpdatePlatformConfigRequest` — `{ @NotBlank configValue }`
   - `BoilerServiceValidityRuleDTO` — `{ id, validFrom, validityDurationMonths, description, isCurrent, isScheduled, createdAt, createdByUsername }`
   - `AddBoilerServiceValidityRuleRequest` — `{ @NotNull validFrom, @NotNull @Min(1) validityDurationMonths, description }`

4. **Repositories:**
   - `PlatformConfigRepository` extends `JpaRepository<PlatformConfig, String>`
   - `BoilerServiceValidityRuleRepository`:
     - `findAllByOrderByValidFromDesc()`
     - `existsByValidFrom(LocalDate)`
     - `findTopByValidFromLessThanEqualOrderByValidFromDesc(LocalDate date)` — finds rule in effect on a given date

5. **Service: `PlatformConfigService`**
   - `getAllConfigs()` → `List<PlatformConfigDTO>`
   - `getConfig(key)` → `PlatformConfigDTO` — throws `PlatformConfigNotFoundException` if not found
   - `updateConfig(key, req, currentUser)` — validate value matches valueType; update configValue, updatedAt, updatedBy
   - `getIntValue(key)` → `int` — utility used by other services (e.g., BoilerService reads alert threshold)

6. **Service: `BoilerServiceValidityRuleService`**
   - `getAllRules()` → `List<BoilerServiceValidityRuleDTO>` — isCurrent = (validFrom ≤ today AND is most recent); isScheduled = (validFrom > today)
   - `addRule(req, currentUser)` → `BoilerServiceValidityRuleDTO` — check uniqueness on validFrom
   - `getRuleForDate(LocalDate serviceDate)` → `BoilerServiceValidityRule` — returns rule with highest validFrom ≤ serviceDate; throws if none found (should never happen given seed)
   - `calculateValidUntil(LocalDate serviceDate)` → `LocalDate` — delegates to getRuleForDate, adds validityDurationMonths

7. **Controller: `PlatformConfigController`** (no @RequestMapping prefix)

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/config/settings | US067 |
| PUT | /api/v1/config/settings/{key} | US070 |
| GET | /api/v1/config/boiler-validity-rules | US069 |
| POST | /api/v1/config/boiler-validity-rules | US068 |

8. **Exceptions:** `PlatformConfigNotFoundException`, `BoilerServiceValidityRuleException` (duplicate validFrom), `InvalidConfigValueException`

---

## PART 2 — UC011: BOILERS

### Backend

9. **Entity: `Boiler`** — table `boiler`.
   - Fields: id, housingUnit (@ManyToOne, FK), brand, model, fuelType, installationDate (LocalDate), removalDate (LocalDate, nullable), createdAt (@PrePersist), createdBy (@ManyToOne AppUser).
   - No @PreUpdate.

10. **Entity: `BoilerService`** — table `boiler_service` (name conflict: use `BoilerServiceRecord` for the entity class to avoid collision with Spring `@Service`).
    - Fields: id, boiler (@ManyToOne), serviceDate (LocalDate), validUntil (LocalDate), notes, createdAt (@PrePersist), createdBy (@ManyToOne AppUser).

11. **Enum: `FuelType`** — `GAS, OIL, WOOD, PELLET, ELECTRIC, HEAT_PUMP, OTHER`

12. **Enum: `ServiceStatus`** — `VALID, EXPIRING_SOON, EXPIRED, NO_SERVICE`

13. **DTOs:**
    - `BoilerDTO` — `{ id, housingUnitId, brand, model, fuelType, installationDate, removalDate, isActive, latestService: BoilerServiceRecordDTO, serviceStatus: ServiceStatus, createdAt }`
    - `BoilerServiceRecordDTO` — `{ id, boilerId, serviceDate, validUntil, notes, status: ServiceStatus, createdAt }`
    - `AddBoilerRequest` — `{ @NotBlank brand, model, fuelType, @NotNull installationDate }` — validate installationDate not future
    - `ReplaceBoilerRequest` — `{ @NotNull removalDate, @NotBlank newBrand, newModel, newFuelType, @NotNull newInstallationDate }`
    - `AddBoilerServiceRecordRequest` — `{ @NotNull serviceDate, validUntil (nullable override), notes }`

14. **Repositories:**
    - `BoilerRepository`:
      - `findByHousingUnitIdOrderByInstallationDateDesc(Long unitId)`
      - `findByHousingUnitIdAndRemovalDateIsNull(Long unitId)` — active boiler
    - `BoilerServiceRecordRepository`:
      - `findByBoilerIdOrderByServiceDateDesc(Long boilerId)`
      - `findTopByBoilerIdOrderByServiceDateDesc(Long boilerId)` — latest service

15. **Service: `BoilerManagementService`**
    - `getActiveBoiler(unitId)` → `Optional<BoilerDTO>` — US061
    - `getAllBoilers(unitId)` → `List<BoilerDTO>` — US063
    - `addBoiler(unitId, req, currentUser)` → `BoilerDTO` — check no active boiler exists; validate installationDate not future — US060
    - `replaceBoiler(unitId, req, currentUser)` → `BoilerDTO` — atomic: close active + create new; validate dates — US062
    - `getServiceRecords(boilerId)` → `List<BoilerServiceRecordDTO>` — US065
    - `addServiceRecord(boilerId, req, currentUser)` → `BoilerServiceRecordDTO` — calculate validUntil if not overridden — US064
    - `computeServiceStatus(BoilerServiceRecordDTO latestService)` → `ServiceStatus` — reads threshold from PlatformConfigService — US066
    - Private helper `toBoilerDTO(Boiler)` — enriches with latestService + serviceStatus

16. **Controller: `BoilerController`** (no @RequestMapping prefix)

| Method | Endpoint | Story |
|--------|----------|-------|
| GET | /api/v1/housing-units/{unitId}/boilers/active | US061 |
| GET | /api/v1/housing-units/{unitId}/boilers | US063 |
| POST | /api/v1/housing-units/{unitId}/boilers | US060 |
| PUT | /api/v1/housing-units/{unitId}/boilers/active/replace | US062 |
| GET | /api/v1/boilers/{boilerId}/services | US065 |
| POST | /api/v1/boilers/{boilerId}/services | US064 |

17. **Exceptions:** `BoilerNotFoundException`, `ActiveBoilerAlreadyExistsException`, `NoActiveBoilerException`, `BoilerBusinessRuleException`

---

## FRONTEND

### Models — `boiler.model.ts`

```typescript
export type FuelType = 'GAS' | 'OIL' | 'WOOD' | 'PELLET' | 'ELECTRIC' | 'HEAT_PUMP' | 'OTHER';
export type ServiceStatus = 'VALID' | 'EXPIRING_SOON' | 'EXPIRED' | 'NO_SERVICE';

export interface BoilerServiceRecordDTO { id, boilerId, serviceDate, validUntil, notes, status: ServiceStatus, createdAt }
export interface BoilerDTO { id, housingUnitId, brand, model, fuelType, installationDate, removalDate, isActive, latestService, serviceStatus, createdAt }
export interface AddBoilerRequest { brand, model?, fuelType?, installationDate }
export interface ReplaceBoilerRequest { removalDate, newBrand, newModel?, newFuelType?, newInstallationDate }
export interface AddBoilerServiceRecordRequest { serviceDate, validUntil?, notes? }

export const FUEL_TYPE_LABELS: Record<FuelType, string> = {
  GAS: 'Natural Gas', OIL: 'Oil', WOOD: 'Wood', PELLET: 'Pellet',
  ELECTRIC: 'Electric', HEAT_PUMP: 'Heat Pump', OTHER: 'Other'
};
```

### Models — `platform-config.model.ts`

```typescript
export interface PlatformConfigDTO { configKey, configValue, valueType, description, updatedAt, updatedByUsername }
export interface BoilerServiceValidityRuleDTO { id, validFrom, validityDurationMonths, description, isCurrent, isScheduled, createdAt, createdByUsername }
export interface UpdatePlatformConfigRequest { configValue: string }
export interface AddBoilerServiceValidityRuleRequest { validFrom, validityDurationMonths, description? }
```

### Services

18. **`BoilerService`** (`boiler.service.ts`):
    - `getActiveBoiler(unitId)` → `Observable<BoilerDTO | null>`
    - `getAllBoilers(unitId)` → `Observable<BoilerDTO[]>`
    - `addBoiler(unitId, req)` → `Observable<BoilerDTO>`
    - `replaceBoiler(unitId, req)` → `Observable<BoilerDTO>`
    - `getServiceRecords(boilerId)` → `Observable<BoilerServiceRecordDTO[]>`
    - `addServiceRecord(boilerId, req)` → `Observable<BoilerServiceRecordDTO>`

19. **`PlatformConfigService`** (`platform-config.service.ts`):
    - `getSettings()` → `Observable<PlatformConfigDTO[]>`
    - `updateSetting(key, req)` → `Observable<PlatformConfigDTO>`
    - `getValidityRules()` → `Observable<BoilerServiceValidityRuleDTO[]>`
    - `addValidityRule(req)` → `Observable<BoilerServiceValidityRuleDTO>`

### Components

20. **`BoilerSectionComponent`** (standalone, input: `[unitId]: number`) — embedded in `HousingUnitDetailsComponent`:
    - Panel states: `loading | idle | add | replace | history`
    - **Idle state**: active boiler card with brand, model, fuel type, installation date, validity badge, "+ Add Service" button, "View Service History" toggle, "Replace Boiler" button. If no active boiler: "No boiler registered" + "Add Boiler" button.
    - **Validity badge**: color-coded by ServiceStatus. Always visible on the card without expanding.
    - **Add state**: inline form (AddBoilerRequest). On save: reload active boiler.
    - **Replace state**: current boiler info (read-only) + ReplaceBoilerRequest form. Atomic save.
    - **Service history**: collapsible table. "+ Add Service" opens inline form. On service date change → call backend `GET /api/v1/config/boiler-validity-rules` to compute valid_until preview client-side (take rule with highest validFrom ≤ serviceDate, add months). User can override the displayed valid_until.
    - **History toggle**: loads all boilers, shows history table with Active/Removed badges.

21. **`PlatformSettingsComponent`** (standalone, routed at `/admin/platform-settings`):
    - Two sections: "Boiler Service Validity Rules" and "General Settings".
    - **Boiler Service Validity Rules section**:
      - Table: validFrom, duration (months), description, created by, created at, badge (Current / Scheduled).
      - "Add Rule" button → inline form.
    - **General Settings section**:
      - List of config entries. Each row: label, current value, "Edit" button.
      - "Edit" opens inline form with value pre-filled. Save → update via PUT.
      - Show updated at + updated by.

22. **Navigation**: Add "Platform Settings" link in the Administration section of the sidebar, alongside the existing "Users" link. Route: `/admin/platform-settings`.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where enforced |
|---|---|
| Only one active boiler per unit | Backend: check before addBoiler |
| Removal date ≥ boiler installation date | Backend + frontend validation |
| New installation date ≥ removal date | Backend + frontend validation |
| Service date not future | Backend + frontend (max = today) |
| valid_until auto-calc uses rule in effect on service_date | Backend: BoilerServiceValidityRuleService.calculateValidUntil(); Frontend: preview calculation |
| Alert threshold from platform_config | Backend: PlatformConfigService.getIntValue('boiler.service.alert.threshold.months') |
| Validity rules append-only (no edit/delete) | Backend: no PUT/DELETE on rules endpoint |
| platform_config: validate value against valueType before save | Backend: InvalidConfigValueException |

---

## ACCEPTANCE CRITERIA SUMMARY

- [ ] Add boiler: brand required, installation date not future, only one active allowed
- [ ] Replace: atomic (old closed + new active), date validations enforced
- [ ] Boiler history: all boilers listed, Active/Removed badges correct
- [ ] Add service: valid_until auto-calculated from correct rule, override allowed
- [ ] Validity alert: correct color badge on boiler card at all times
- [ ] Platform Settings page accessible from admin nav
- [ ] Add validity rule: uniqueness on valid_from enforced, no retroactive recalculation
- [ ] Rules history: Current / Scheduled badges correct
- [ ] Update alert threshold: integer validation, takes effect immediately
- [ ] All US060–US070 acceptance criteria verified

**Last Updated:** 2026-03-01 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
