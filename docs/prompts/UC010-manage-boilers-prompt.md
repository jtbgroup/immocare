# ImmoCare — UC012 Manage Boilers — Implementation Prompt

I want to implement Use Case UC012 - Manage Boilers for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Platform Config
- **Prerequisite**: UC013 (Manage Platform Configuration) must be fully implemented before starting this UC (boiler alert threshold is read from platform_config)

---

## USER STORIES

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US060 | Add Boiler to Housing Unit | MUST HAVE | 3 |
| US061 | View Active Boiler | MUST HAVE | 2 |
| US062 | Replace Boiler | MUST HAVE | 3 |
| US063 | View Boiler History | SHOULD HAVE | 2 |
| US064 | Add Boiler Service Record | MUST HAVE | 3 |
| US065 | View Boiler Service History | MUST HAVE | 2 |
| US066 | View Boiler Service Validity Alert | MUST HAVE | 2 |

---

## DATABASE MIGRATION — `V012__create_boiler.sql`

> Do NOT include platform_config or boiler_service_validity_rule tables here — those are in UC013 migration.

```sql
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
```

---

## BACKEND

### Enums

```java
public enum FuelType    { GAS, OIL, WOOD, PELLET, ELECTRIC, HEAT_PUMP, OTHER }
public enum ServiceStatus { VALID, EXPIRING_SOON, EXPIRED, NO_SERVICE }
```

### Entities

**`Boiler`** — table `boiler`:
- Fields: `id`, `housingUnit` (@ManyToOne HousingUnit, NOT NULL), `brand`, `model`, `fuelType` (String), `installationDate` (LocalDate), `removalDate` (LocalDate, nullable), `createdAt` (@PrePersist), `createdBy` (@ManyToOne AppUser, nullable).
- No @PreUpdate.

**`BoilerServiceRecord`** — table `boiler_service` (class name avoids collision with Spring @Service):
- Fields: `id`, `boiler` (@ManyToOne Boiler, NOT NULL), `serviceDate` (LocalDate), `validUntil` (LocalDate), `notes`, `createdAt` (@PrePersist), `createdBy` (@ManyToOne AppUser, nullable).
- No @PreUpdate.

### DTOs

**`BoilerDTO`**:
```java
record BoilerDTO(
    Long id, Long housingUnitId, String brand, String model, String fuelType,
    LocalDate installationDate, LocalDate removalDate,
    boolean isActive,
    BoilerServiceRecordDTO latestService,
    ServiceStatus serviceStatus,
    LocalDateTime createdAt
) {}
```

**`BoilerServiceRecordDTO`**:
```java
record BoilerServiceRecordDTO(
    Long id, Long boilerId,
    LocalDate serviceDate, LocalDate validUntil,
    String notes, ServiceStatus status,
    LocalDateTime createdAt
) {}
```

**`AddBoilerRequest`**:
```java
record AddBoilerRequest(
    @NotBlank String brand,
    String model,
    String fuelType,
    @NotNull LocalDate installationDate
) {}
// Validate: installationDate not future
```

**`ReplaceBoilerRequest`**:
```java
record ReplaceBoilerRequest(
    @NotNull LocalDate removalDate,
    @NotBlank String newBrand,
    String newModel,
    String newFuelType,
    @NotNull LocalDate newInstallationDate
) {}
// Validate: removalDate >= current boiler installationDate
//           newInstallationDate >= removalDate
```

**`AddBoilerServiceRecordRequest`**:
```java
record AddBoilerServiceRecordRequest(
    @NotNull LocalDate serviceDate,
    LocalDate validUntil,   // nullable override; if null → auto-calculated
    String notes
) {}
// Validate: serviceDate not future
```

### Repositories

**`BoilerRepository`** extends `JpaRepository<Boiler, Long>`:
```java
List<Boiler> findByHousingUnitIdOrderByInstallationDateDesc(Long unitId);
Optional<Boiler> findByHousingUnitIdAndRemovalDateIsNull(Long unitId);
```

**`BoilerServiceRecordRepository`** extends `JpaRepository<BoilerServiceRecord, Long>`:
```java
List<BoilerServiceRecord> findByBoilerIdOrderByServiceDateDesc(Long boilerId);
Optional<BoilerServiceRecord> findTopByBoilerIdOrderByServiceDateDesc(Long boilerId);
```

### Service: `BoilerManagementService`

```java
Optional<BoilerDTO> getActiveBoiler(Long unitId);          // US061
List<BoilerDTO> getAllBoilers(Long unitId);                 // US063
BoilerDTO addBoiler(Long unitId, AddBoilerRequest req, AppUser currentUser);       // US060
BoilerDTO replaceBoiler(Long unitId, ReplaceBoilerRequest req, AppUser currentUser); // US062
List<BoilerServiceRecordDTO> getServiceRecords(Long boilerId);                     // US065
BoilerServiceRecordDTO addServiceRecord(Long boilerId, AddBoilerServiceRecordRequest req, AppUser currentUser); // US064
```

**Business rules:**
- `addBoiler`: check no active boiler exists → throw `ActiveBoilerAlreadyExistsException`
- `replaceBoiler`: @Transactional — close active (set removalDate) + create new in single transaction
- `addServiceRecord`: if `validUntil` is null → call `BoilerServiceValidityRuleService.calculateValidUntil(serviceDate)`
- `computeServiceStatus(latestService)`: reads threshold from `PlatformConfigService.getIntValue("boiler.service.alert.threshold.months")`
  - null latestService → NO_SERVICE
  - validUntil < today → EXPIRED
  - validUntil within threshold months → EXPIRING_SOON
  - otherwise → VALID
- Private `toBoilerDTO(Boiler)`: enriches with latestService + serviceStatus

### Exceptions

- `BoilerNotFoundException` → 404
- `ActiveBoilerAlreadyExistsException` → 409
- `NoActiveBoilerException` → 409
- `BoilerBusinessRuleException` → 400

### Controller: `BoilerController`

No @RequestMapping prefix; full path per method. Security handled globally.

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| GET | `/api/v1/housing-units/{unitId}/boilers/active` | — | `BoilerDTO` 200 or 204 | US061 |
| GET | `/api/v1/housing-units/{unitId}/boilers` | — | `List<BoilerDTO>` 200 | US063 |
| POST | `/api/v1/housing-units/{unitId}/boilers` | `AddBoilerRequest` | `BoilerDTO` 201 | US060 |
| PUT | `/api/v1/housing-units/{unitId}/boilers/active/replace` | `ReplaceBoilerRequest` | `BoilerDTO` 200 | US062 |
| GET | `/api/v1/boilers/{boilerId}/services` | — | `List<BoilerServiceRecordDTO>` 200 | US065 |
| POST | `/api/v1/boilers/{boilerId}/services` | `AddBoilerServiceRecordRequest` | `BoilerServiceRecordDTO` 201 | US064 |
| GET | `/api/v1/boilers/{boilerId}/transaction-count` | — | `long` 200 | US083 |

The last endpoint delegates to `TransactionAssetLinkRepository.countByAssetTypeAndAssetId(AssetType.BOILER, boilerId)`.

### GlobalExceptionHandler — add these handlers

```java
@ExceptionHandler(BoilerNotFoundException.class)
public ResponseEntity<ErrorResponse> handleBoilerNotFound(BoilerNotFoundException ex) {
    return notFound("Boiler not found", ex.getMessage());
}

@ExceptionHandler(ActiveBoilerAlreadyExistsException.class)
public ResponseEntity<ErrorResponse> handleActiveBoilerExists(ActiveBoilerAlreadyExistsException ex) {
    return conflict("Active boiler already exists", ex.getMessage());
}

@ExceptionHandler(NoActiveBoilerException.class)
public ResponseEntity<ErrorResponse> handleNoActiveBoiler(NoActiveBoilerException ex) {
    return conflict("No active boiler", ex.getMessage());
}

@ExceptionHandler(BoilerBusinessRuleException.class)
public ResponseEntity<ErrorResponse> handleBoilerBusinessRule(BoilerBusinessRuleException ex) {
    return badRequest("Boiler business rule violation", ex.getMessage());
}
```

---

## FRONTEND

### Models — `boiler.model.ts`

```typescript
export type FuelType      = 'GAS' | 'OIL' | 'WOOD' | 'PELLET' | 'ELECTRIC' | 'HEAT_PUMP' | 'OTHER';
export type ServiceStatus = 'VALID' | 'EXPIRING_SOON' | 'EXPIRED' | 'NO_SERVICE';

export interface BoilerServiceRecord {
  id: number; boilerId: number;
  serviceDate: string; validUntil: string;
  notes: string | null; status: ServiceStatus;
  createdAt: string;
}

export interface Boiler {
  id: number; housingUnitId: number;
  brand: string; model: string | null; fuelType: FuelType | null;
  installationDate: string; removalDate: string | null;
  isActive: boolean;
  latestService: BoilerServiceRecord | null;
  serviceStatus: ServiceStatus;
  createdAt: string;
}

export interface AddBoilerRequest    { brand: string; model?: string; fuelType?: FuelType; installationDate: string; }
export interface ReplaceBoilerRequest { removalDate: string; newBrand: string; newModel?: string; newFuelType?: FuelType; newInstallationDate: string; }
export interface AddBoilerServiceRecordRequest { serviceDate: string; validUntil?: string; notes?: string; }

export const FUEL_TYPE_LABELS: Record<FuelType, string> = {
  GAS: 'Natural Gas', OIL: 'Oil', WOOD: 'Wood', PELLET: 'Pellet',
  ELECTRIC: 'Electric', HEAT_PUMP: 'Heat Pump', OTHER: 'Other'
};
```

### Service — `boiler.service.ts`

```typescript
getActiveBoiler(unitId: number): Observable<Boiler | null>
getAllBoilers(unitId: number): Observable<Boiler[]>
addBoiler(unitId: number, req: AddBoilerRequest): Observable<Boiler>
replaceBoiler(unitId: number, req: ReplaceBoilerRequest): Observable<Boiler>
getServiceRecords(boilerId: number): Observable<BoilerServiceRecord[]>
addServiceRecord(boilerId: number, req: AddBoilerServiceRecordRequest): Observable<BoilerServiceRecord>
getTransactionCount(boilerId: number): Observable<number>
```

### Component — `BoilerSectionComponent`

Standalone, input: `[unitId]: number`. Embedded in `HousingUnitDetailsComponent`.

**Panel states:** `loading | idle | add | replace | history`

**Idle state:**
- Active boiler card: brand, model, fuel type, installation date, validity badge (always visible, color-coded by ServiceStatus), "+ Add Service" button, "View Service History" toggle, "Replace Boiler" button.
- "Related expenses (N)" badge → navigates to `/transactions?tab=list&assetType=BOILER&assetId={id}`. Count loaded from `getTransactionCount()`.
- No active boiler: "No boiler registered" + "Add Boiler" button.

**Add state:** inline form (AddBoilerRequest). On save: reload active boiler.

**Replace state:** current boiler info read-only + ReplaceBoilerRequest form. Validate dates client-side before submit.

**Service history:** collapsible table. "+ Add Service" opens inline form.
- On service date change → compute valid_until preview client-side: find rule with highest validFrom ≤ serviceDate from `GET /api/v1/config/boiler-validity-rules`, add validityDurationMonths months. User can override.

**History toggle:** loads all boilers via `getAllBoilers()`, shows history table with Active/Removed badges.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Only one active boiler per unit (removal_date IS NULL) | Backend: check before addBoiler |
| Removal date ≥ boiler installation date | Backend + frontend validation |
| New installation date ≥ removal date | Backend + frontend validation |
| Service date not future | Backend + frontend (max = today) |
| valid_until auto-calc uses rule in effect on service_date | Backend: BoilerServiceValidityRuleService.calculateValidUntil(); Frontend: preview |
| Alert threshold from platform_config | Backend: PlatformConfigService.getIntValue('boiler.service.alert.threshold.months') |

---

## ACCEPTANCE CRITERIA

- [ ] Add boiler: brand required, installation date not future, only one active allowed
- [ ] Replace: atomic (old closed + new active), date validations enforced
- [ ] Boiler history: all boilers listed, Active/Removed badges correct
- [ ] Add service: valid_until auto-calculated from correct rule, override allowed
- [ ] Validity alert: correct color badge on boiler card at all times
- [ ] Transaction count badge shown on boiler card
- [ ] All US060–US066 acceptance criteria verified

**Last Updated:** 2026-04-04 | **Branch:** `develop` | **Status:** ✅ Implemented
