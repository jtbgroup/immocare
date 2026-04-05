# ImmoCare — UC013 Manage Fire Extinguishers — Implementation Prompt

I want to implement Use Case UC013 - Manage Fire Extinguishers for ImmoCare.

---

## CONTEXT

- **Project**: ImmoCare (property management system)
- **Stack**: Spring Boot 3.x (backend) + Angular 17+ (frontend) + PostgreSQL 16
- **Architecture**: API-First, mono-repo
- **Auth**: Session-based, already implemented (ADMIN only)
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Boilers, Platform Config

---

## USER STORIES TO IMPLEMENT

| Story | Title | Priority | Points |
|-------|-------|----------|--------|
| US071 | Add Fire Extinguisher to Building | MUST HAVE | 3 |
| US072 | Edit Fire Extinguisher | MUST HAVE | 2 |
| US073 | Delete Fire Extinguisher | MUST HAVE | 2 |
| US074 | View Fire Extinguishers List | MUST HAVE | 2 |
| US075 | Add Revision Record | MUST HAVE | 3 |
| US076 | View Revision History | MUST HAVE | 2 |
| US077 | Delete Revision Record | SHOULD HAVE | 1 |

---

## DATABASE MIGRATION

Create `backend/src/main/resources/db/migration/V002__fire_extinguishers.sql`

> Do **NOT** modify V001. The full baseline is already in V001.

```sql
-- ─── fire_extinguisher ────────────────────────────────────────────────────────

CREATE TABLE fire_extinguisher (
    id                    BIGSERIAL    PRIMARY KEY,
    building_id           BIGINT       NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    unit_id               BIGINT       NULL     REFERENCES housing_unit (id) ON DELETE SET NULL,
    identification_number VARCHAR(50)  NOT NULL,
    notes                 TEXT         NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_extinguisher_number UNIQUE (building_id, identification_number)
);

CREATE INDEX idx_fire_extinguisher_building ON fire_extinguisher (building_id);
CREATE INDEX idx_fire_extinguisher_unit     ON fire_extinguisher (unit_id);

COMMENT ON TABLE  fire_extinguisher                       IS 'Fire extinguishers attached to a building';
COMMENT ON COLUMN fire_extinguisher.identification_number IS 'Unique identifier within the building';
COMMENT ON COLUMN fire_extinguisher.unit_id               IS 'Optional FK — housing unit where the extinguisher is located';

-- ─── fire_extinguisher_revision ───────────────────────────────────────────────

CREATE TABLE fire_extinguisher_revision (
    id                    BIGSERIAL  PRIMARY KEY,
    fire_extinguisher_id  BIGINT     NOT NULL REFERENCES fire_extinguisher (id) ON DELETE CASCADE,
    revision_date         DATE       NOT NULL,
    notes                 TEXT       NULL,
    created_at            TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fire_ext_revision_extinguisher ON fire_extinguisher_revision (fire_extinguisher_id);

COMMENT ON TABLE fire_extinguisher_revision IS 'Revision history for a fire extinguisher';
```

---

## BACKEND

### Entities

**`FireExtinguisher`** — table `fire_extinguisher`:
- Fields: `id`, `building` (ManyToOne `Building`, not null), `unit` (ManyToOne `HousingUnit`, nullable), `identificationNumber` (VARCHAR 50), `notes` (TEXT), `createdAt`, `updatedAt`
- `@PrePersist` sets `createdAt` and `updatedAt`; `@PreUpdate` sets `updatedAt`
- `@OneToMany(mappedBy = "fireExtinguisher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)` for revisions — annotate with `@OrderBy("revisionDate DESC, createdAt DESC")`

**`FireExtinguisherRevision`** — table `fire_extinguisher_revision`:
- Fields: `id`, `fireExtinguisher` (ManyToOne `FireExtinguisher`, not null), `revisionDate` (LocalDate), `notes` (TEXT), `createdAt`
- `@PrePersist` sets `createdAt`

### DTOs

**`FireExtinguisherDTO`** (response — Java record):
```java
record FireExtinguisherDTO(
    Long id,
    Long buildingId,
    Long unitId,            // null if not assigned
    String unitNumber,      // from housing_unit.unit_number, null if not assigned
    String identificationNumber,
    String notes,
    List<FireExtinguisherRevisionDTO> revisions,  // ordered revisionDate DESC
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**`FireExtinguisherRevisionDTO`** (response — Java record):
```java
record FireExtinguisherRevisionDTO(
    Long id,
    Long fireExtinguisherId,
    LocalDate revisionDate,
    String notes,
    LocalDateTime createdAt
) {}
```

**`SaveFireExtinguisherRequest`** (POST + PUT body — Java record):
```java
record SaveFireExtinguisherRequest(
    @NotBlank @Size(max = 50) String identificationNumber,
    Long unitId,
    @Size(max = 2000) String notes
) {}
```

**`AddRevisionRequest`** (POST body — Java record):
```java
record AddRevisionRequest(
    @NotNull LocalDate revisionDate,
    @Size(max = 2000) String notes
) {}
```

### Exceptions

- **`FireExtinguisherNotFoundException`** — message: `"Fire extinguisher not found: " + id` → HTTP 404
- **`FireExtinguisherRevisionNotFoundException`** — message: `"Revision record not found: " + id` → HTTP 404
- **`FireExtinguisherDuplicateNumberException`** — message: `"An extinguisher with this identification number already exists in this building"` → HTTP 409

### Repositories

**`FireExtinguisherRepository`** extends `JpaRepository<FireExtinguisher, Long>`:
```java
List<FireExtinguisher> findByBuildingIdOrderByIdentificationNumberAsc(Long buildingId);

boolean existsByBuildingIdAndIdentificationNumberIgnoreCase(Long buildingId, String identificationNumber);

@Query("SELECT COUNT(e) > 0 FROM FireExtinguisher e " +
       "WHERE e.building.id = :buildingId " +
       "AND LOWER(e.identificationNumber) = LOWER(:num) " +
       "AND e.id <> :excludeId")
boolean existsByBuildingIdAndNumberIgnoreCaseExcluding(
    @Param("buildingId") Long buildingId,
    @Param("num") String num,
    @Param("excludeId") Long excludeId);
```

**`FireExtinguisherRevisionRepository`** extends `JpaRepository<FireExtinguisherRevision, Long>`:
```java
// No extra methods needed — revisions are loaded via the entity's @OneToMany
```

### Service: `FireExtinguisherService`

`@Service` + `@Transactional(readOnly = true)`

```java
List<FireExtinguisherDTO> getByBuilding(Long buildingId);
FireExtinguisherDTO getById(Long id);

@Transactional
FireExtinguisherDTO create(Long buildingId, SaveFireExtinguisherRequest req);

@Transactional
FireExtinguisherDTO update(Long id, SaveFireExtinguisherRequest req);

@Transactional
void delete(Long id);

@Transactional
FireExtinguisherDTO addRevision(Long extinguisherId, AddRevisionRequest req);

@Transactional
void deleteRevision(Long extinguisherId, Long revisionId);
```

**Business rules to enforce in service**:

| Rule | Implementation |
|---|---|
| BR-UC013-01 | On create: `existsByBuildingIdAndIdentificationNumberIgnoreCase` → throw `FireExtinguisherDuplicateNumberException`. On update: use `existsByBuildingIdAndNumberIgnoreCaseExcluding`. |
| BR-UC013-02 | If `req.unitId()` not null: load `HousingUnit`, verify `unit.building.id == buildingId`, else throw `IllegalArgumentException("The specified unit does not belong to this building")` |
| BR-UC013-03 | `revisionDate` must not be after `LocalDate.now()`, else throw `IllegalArgumentException("Revision date cannot be in the future")` |
| BR-UC013-04 | Cascade on entity handles deletion of revisions — no extra logic needed |

**`toDTO` helper** — maps entity to `FireExtinguisherDTO`. Set `unitNumber` from `entity.getUnit() != null ? entity.getUnit().getUnitNumber() : null`. Map revisions list from the `@OneToMany` collection (already sorted by `@OrderBy`).

### Controller: `FireExtinguisherController`

`@RestController` — no `@RequestMapping` prefix; use full path per method.  
Security is handled globally — no `@PreAuthorize` needed.

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| GET | `/api/v1/buildings/{buildingId}/fire-extinguishers` | — | `List<FireExtinguisherDTO>` 200 | US074 |
| GET | `/api/v1/fire-extinguishers/{id}` | — | `FireExtinguisherDTO` 200 | US074 |
| POST | `/api/v1/buildings/{buildingId}/fire-extinguishers` | `SaveFireExtinguisherRequest` | `FireExtinguisherDTO` 201 | US071 |
| PUT | `/api/v1/fire-extinguishers/{id}` | `SaveFireExtinguisherRequest` | `FireExtinguisherDTO` 200 | US072 |
| DELETE | `/api/v1/fire-extinguishers/{id}` | — | 204 | US073 |
| POST | `/api/v1/fire-extinguishers/{id}/revisions` | `AddRevisionRequest` | `FireExtinguisherDTO` 201 | US075 |
| DELETE | `/api/v1/fire-extinguishers/{extId}/revisions/{revId}` | — | 204 | US077 |

### GlobalExceptionHandler — add these handlers

```java
// ─── UC013 - Fire Extinguishers ───────────────────────────────────────────────

@ExceptionHandler(FireExtinguisherNotFoundException.class)
public ResponseEntity<ErrorResponse> handleFireExtinguisherNotFound(FireExtinguisherNotFoundException ex) {
    return notFound("Fire extinguisher not found", ex.getMessage());
}

@ExceptionHandler(FireExtinguisherRevisionNotFoundException.class)
public ResponseEntity<ErrorResponse> handleFireExtinguisherRevisionNotFound(FireExtinguisherRevisionNotFoundException ex) {
    return notFound("Revision record not found", ex.getMessage());
}

@ExceptionHandler(FireExtinguisherDuplicateNumberException.class)
public ResponseEntity<ErrorResponse> handleFireExtinguisherDuplicate(FireExtinguisherDuplicateNumberException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "DUPLICATE", ex.getMessage(), LocalDateTime.now()));
}
```

---

## FRONTEND

### Model: `fire-extinguisher.model.ts`

Location: `frontend/src/app/models/fire-extinguisher.model.ts`

```typescript
export interface FireExtinguisherRevision {
  id: number;
  fireExtinguisherId: number;
  revisionDate: string;   // ISO date 'yyyy-MM-dd'
  notes: string | null;
  createdAt: string;
}

export interface FireExtinguisher {
  id: number;
  buildingId: number;
  unitId: number | null;
  unitNumber: string | null;
  identificationNumber: string;
  notes: string | null;
  revisions: FireExtinguisherRevision[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveFireExtinguisherRequest {
  identificationNumber: string;
  unitId: number | null;
  notes: string | null;
}

export interface AddRevisionRequest {
  revisionDate: string;   // ISO date 'yyyy-MM-dd'
  notes: string | null;
}
```

### Service: `FireExtinguisherService`

Location: `frontend/src/app/core/services/fire-extinguisher.service.ts`

`@Injectable({ providedIn: 'root' })`  
Base URL: `private readonly api = '/api/v1'`  
All calls use the default credentials interceptor (no explicit `withCredentials`).

```typescript
getByBuilding(buildingId: number): Observable<FireExtinguisher[]>
  → GET /api/v1/buildings/{buildingId}/fire-extinguishers

getById(id: number): Observable<FireExtinguisher>
  → GET /api/v1/fire-extinguishers/{id}

create(buildingId: number, req: SaveFireExtinguisherRequest): Observable<FireExtinguisher>
  → POST /api/v1/buildings/{buildingId}/fire-extinguishers

update(id: number, req: SaveFireExtinguisherRequest): Observable<FireExtinguisher>
  → PUT /api/v1/fire-extinguishers/{id}

delete(id: number): Observable<void>
  → DELETE /api/v1/fire-extinguishers/{id}

addRevision(extId: number, req: AddRevisionRequest): Observable<FireExtinguisher>
  → POST /api/v1/fire-extinguishers/{extId}/revisions

deleteRevision(extId: number, revId: number): Observable<void>
  → DELETE /api/v1/fire-extinguishers/{extId}/revisions/{revId}
```

### Component: `FireExtinguisherSectionComponent`

Location:
```
frontend/src/app/features/building/components/fire-extinguisher-section/
├── fire-extinguisher-section.component.ts
├── fire-extinguisher-section.component.html
└── fire-extinguisher-section.component.scss
```

**Decorator**:
```typescript
@Component({
  selector: 'app-fire-extinguisher-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, AppDatePipe],
  ...
})
```

**Inputs**:
```typescript
@Input() buildingId!: number;
@Input() buildingUnits: { id: number; unitNumber: string }[] = [];
```

**Component state**:
```typescript
extinguishers: FireExtinguisher[] = [];
loading = false;
error: string | null = null;

// Add/Edit form
showForm = false;
editingId: number | null = null;
saving = false;
saveError: string | null = null;
form!: FormGroup;

// Delete extinguisher confirmation
deleteConfirmId: number | null = null;

// Revision panel (open/closed per extinguisher id)
revisionPanelOpen: Record<number, boolean> = {};

// Add revision form (open per extinguisher id)
revisionFormOpenId: number | null = null;
revisionForm!: FormGroup;
savingRevision = false;
revisionFormError: string | null = null;

// Delete revision confirmation (revisionId)
deleteRevisionConfirmId: number | null = null;
deleteRevisionExtId: number | null = null;

today = new Date().toISOString().split('T')[0];
```

**Methods**:
- `ngOnInit()` — builds forms, calls `loadExtinguishers()`
- `loadExtinguishers()` — calls `service.getByBuilding(buildingId)`, populates `extinguishers`
- `openAddForm()` — resets form, sets `showForm = true`, `editingId = null`
- `openEditForm(ext)` — patches form with ext data, sets `editingId = ext.id`, `showForm = true`
- `cancelForm()` — `showForm = false`, clears errors
- `saveForm()` — on valid: calls `create` or `update` based on `editingId`, reloads list on success
- `confirmDelete(id)` — sets `deleteConfirmId = id`
- `cancelDelete()` — clears `deleteConfirmId`
- `doDelete()` — calls `service.delete(deleteConfirmId)`, reloads list
- `toggleRevisionPanel(extId)` — toggles `revisionPanelOpen[extId]`
- `openRevisionForm(extId)` — resets revision form, sets `revisionFormOpenId = extId`
- `cancelRevisionForm()` — clears `revisionFormOpenId`
- `saveRevision(extId)` — calls `service.addRevision`, updates `extinguishers` in place from response
- `confirmDeleteRevision(extId, revId)` — sets both `deleteRevisionExtId` and `deleteRevisionConfirmId`
- `cancelDeleteRevision()` — clears both
- `doDeleteRevision()` — calls `service.deleteRevision`, reloads list
- `latestRevisionDate(ext)` — returns `ext.revisions[0]?.revisionDate ?? null`

**Form definitions**:
```typescript
// Extinguisher form
this.form = this.fb.group({
  identificationNumber: ['', [Validators.required, Validators.maxLength(50)]],
  unitId: [null],
  notes: ['', Validators.maxLength(2000)]
});

// Revision form
this.revisionForm = this.fb.group({
  revisionDate: ['', Validators.required],
  notes: ['', Validators.maxLength(2000)]
});
```

**Template structure** (mirrors `BoilerSectionComponent` pattern):

```
<section>
  <!-- Header: "🧯 Fire Extinguishers (N)" + "Add" button -->
  <div class="section-header">
    <h3>🧯 Fire Extinguishers ({{ extinguishers.length }})</h3>
    <button *ngIf="!showForm" (click)="openAddForm()">Add</button>
  </div>

  <!-- loading / error / empty state -->

  <!-- Add/Edit inline form panel (shown when showForm = true) -->
  <div class="ext-form-panel" *ngIf="showForm">
    <h4>{{ editingId ? 'Edit Extinguisher' : 'Add Extinguisher' }}</h4>
    <form [formGroup]="form">
      <!-- identificationNumber: required, maxlength 50 -->
      <!-- unitId: select dropdown from buildingUnits, first option "— No unit —" (value null) -->
      <!-- notes: textarea, optional -->
      <!-- Save / Cancel buttons -->
    </form>
    <div class="error-banner" *ngIf="saveError">{{ saveError }}</div>
  </div>

  <!-- Extinguisher cards list -->
  <div class="ext-list" *ngIf="!loading && extinguishers.length > 0">
    <div class="ext-card" *ngFor="let ext of extinguishers">

      <!-- Card header: id number + unit + actions -->
      <div class="ext-card__header">
        <div class="ext-card__identity">
          <span class="ext-id">{{ ext.identificationNumber }}</span>
          <a *ngIf="ext.unitId" [routerLink]="['/units', ext.unitId]">
            Unit {{ ext.unitNumber }}
          </a>
          <span *ngIf="!ext.unitId" class="text-muted">—</span>
        </div>
        <div class="ext-card__actions">
          <button class="btn btn-xs btn-secondary" (click)="openEditForm(ext)">Edit</button>
          <button class="btn btn-xs btn-danger" (click)="confirmDelete(ext.id)">Delete</button>
        </div>
      </div>

      <!-- Card body: notes + revision summary -->
      <div class="ext-card__body">
        <p class="ext-notes" *ngIf="ext.notes">{{ ext.notes }}</p>
        <div class="ext-meta">
          <span class="revision-count">{{ ext.revisions.length }} revision(s)</span>
          <span class="latest-revision" *ngIf="latestRevisionDate(ext)">
            · Last: {{ latestRevisionDate(ext) | appDate }}
          </span>
          <span class="latest-revision text-muted" *ngIf="!latestRevisionDate(ext)">
            · <em>Never inspected</em>
          </span>
        </div>
      </div>

      <!-- Delete extinguisher confirmation (inline) -->
      <div class="delete-confirm" *ngIf="deleteConfirmId === ext.id">
        <span>
          Delete extinguisher <strong>{{ ext.identificationNumber }}</strong>?
          <ng-container *ngIf="ext.revisions.length > 0">
            This will also delete {{ ext.revisions.length }} revision record(s).
          </ng-container>
          This action cannot be undone.
        </span>
        <button class="btn btn-xs btn-danger" (click)="doDelete()">Confirm</button>
        <button class="btn btn-xs btn-secondary" (click)="cancelDelete()">Cancel</button>
      </div>

      <!-- Revision panel toggle -->
      <div class="ext-card__footer">
        <button class="btn-link" (click)="toggleRevisionPanel(ext.id)">
          {{ revisionPanelOpen[ext.id] ? 'Hide revisions' : 'View revisions (' + ext.revisions.length + ')' }}
        </button>
      </div>

      <!-- Revision panel (collapsible) -->
      <div class="revision-panel" *ngIf="revisionPanelOpen[ext.id]">

        <!-- Revision table -->
        <table class="revision-table" *ngIf="ext.revisions.length > 0">
          <thead>
            <tr><th>Date</th><th>Notes</th><th></th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let rev of ext.revisions">
              <td>{{ rev.revisionDate | appDate }}</td>
              <td>{{ rev.notes ?? '—' }}</td>
              <td>
                <button class="btn-link" (click)="confirmDeleteRevision(ext.id, rev.id)">🗑</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="no-data" *ngIf="ext.revisions.length === 0">No revision recorded yet.</p>

        <!-- Delete revision confirmation (inline) -->
        <div class="delete-confirm" *ngIf="deleteRevisionExtId === ext.id && deleteRevisionConfirmId !== null">
          <span>Delete this revision record? This action cannot be undone.</span>
          <button class="btn btn-xs btn-danger" (click)="doDeleteRevision()">Confirm</button>
          <button class="btn btn-xs btn-secondary" (click)="cancelDeleteRevision()">Cancel</button>
        </div>

        <!-- Add revision inline form -->
        <div class="revision-form" *ngIf="revisionFormOpenId !== ext.id">
          <button class="btn btn-xs btn-primary" (click)="openRevisionForm(ext.id)">+ Add revision</button>
        </div>
        <div class="revision-form-panel" *ngIf="revisionFormOpenId === ext.id">
          <form [formGroup]="revisionForm">
            <!-- revisionDate: date input, max="{{ today }}", required -->
            <!-- notes: textarea, optional -->
            <!-- Save / Cancel buttons -->
          </form>
          <div class="error-banner" *ngIf="revisionFormError">{{ revisionFormError }}</div>
        </div>

      </div>
    </div>
  </div>

  <!-- Empty state -->
  <div class="no-data" *ngIf="!loading && extinguishers.length === 0 && !showForm">
    No fire extinguishers registered.
  </div>
</section>
```

**SCSS** (`fire-extinguisher-section.component.scss`):

Reuse global classes: `btn`, `btn-xs`, `btn-primary`, `btn-secondary`, `btn-danger`, `btn-link`, `error-banner`, `no-data`, `loading-placeholder`, `section-header`, `text-muted`.  
Add only the following component-specific rules:

```scss
// fire-extinguisher-section.component.scss
// Shared styles (btn, btn-xs, btn-primary, btn-secondary, btn-danger,
// btn-link, error-banner, no-data, loading-placeholder, section-header,
// text-muted) come from global styles.scss.

.ext-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.ext-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  overflow: hidden;
}

.ext-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  border-bottom: 1px solid #e0e0e0;
  background: #f5f5f5;
}

.ext-card__identity {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.ext-id {
  font-weight: 600;
  font-size: 0.95rem;
}

.ext-card__actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.ext-card__body {
  padding: 0.75rem 1rem;
}

.ext-notes {
  font-size: 0.875rem;
  color: #555;
  margin: 0 0 0.4rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ext-meta {
  font-size: 0.82rem;
  color: #888;
}

.revision-count {
  font-weight: 500;
}

.ext-card__footer {
  padding: 0.4rem 1rem 0.55rem;
  border-top: 1px solid #f0f0f0;
}

.revision-panel {
  padding: 0.75rem 1rem;
  border-top: 1px solid #e8e8e8;
  background: #fff;
}

.revision-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  margin-bottom: 0.75rem;

  th {
    text-align: left;
    padding: 0.35rem 0.5rem;
    border-bottom: 2px solid #e0e0e0;
    font-weight: 600;
    color: #555;
  }

  td {
    padding: 0.4rem 0.5rem;
    border-bottom: 1px solid #f0f0f0;
    vertical-align: middle;
  }

  tr:last-child td { border-bottom: none; }
}

.delete-confirm {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: #fff8f8;
  border-top: 1px solid #f5c6cb;
  font-size: 0.875rem;
}

.ext-form-panel,
.revision-form-panel {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  background: #fff;
  margin-top: 0.75rem;

  h4 { margin: 0 0 1rem; font-size: 0.95rem; font-weight: 600; }
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: 0.75rem;

  label { font-size: 0.85rem; font-weight: 500; }

  input, select, textarea {
    padding: 0.4rem 0.6rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 0.9rem;
    font-family: inherit;
  }
}

.required { color: #dc3545; }
.field-error { font-size: 0.78rem; color: #dc3545; }

.form-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  margin-top: 1rem;
}
```

---

## INTEGRATION INTO `BuildingDetailsComponent`

### `building-details.component.ts` — modifications

1. Add import: `import { FireExtinguisherSectionComponent } from '../fire-extinguisher-section/fire-extinguisher-section.component';`
2. Add `FireExtinguisherSectionComponent` to the `imports` array.
3. Add import: `import { HousingUnit } from '../../../../models/housing-unit.model';`
4. Add import: `import { HousingUnitService } from '../../../../core/services/housing-unit.service';`
5. Add property: `buildingUnits: { id: number; unitNumber: string }[] = [];`
6. Inject `HousingUnitService` in the constructor.
7. In `ngOnInit()`, after the building is loaded, subscribe to `housingUnitService.getUnitsByBuilding(id)` and map the result to `{ id, unitNumber }` pairs:

```typescript
this.housingUnitService.getUnitsByBuilding(id)
  .pipe(takeUntil(this.destroy$))
  .subscribe({
    next: (units) => {
      this.buildingUnits = units.map(u => ({ id: u.id, unitNumber: u.unitNumber }));
    },
    error: () => { /* non-blocking — extinguisher form will just show no units */ }
  });
```

### `building-details.component.html` — modification

Add the following `<div class="info-card">` **after** the existing boiler section block and **before** the housing units section:

```html
<!-- Fire Extinguishers section -->
<div class="info-card">
  <app-fire-extinguisher-section
    [buildingId]="building.id"
    [buildingUnits]="buildingUnits"
  ></app-fire-extinguisher-section>
</div>
```

---

## SUMMARY OF FILES TO CREATE / MODIFY

### New files

| Path | Description |
|---|---|
| `backend/.../db/migration/V002__fire_extinguishers.sql` | Flyway migration |
| `backend/.../model/entity/FireExtinguisher.java` | JPA entity |
| `backend/.../model/entity/FireExtinguisherRevision.java` | JPA entity |
| `backend/.../model/dto/FireExtinguisherDTO.java` | Response record |
| `backend/.../model/dto/FireExtinguisherRevisionDTO.java` | Response record |
| `backend/.../model/dto/SaveFireExtinguisherRequest.java` | Create/update record |
| `backend/.../model/dto/AddRevisionRequest.java` | Add revision record |
| `backend/.../repository/FireExtinguisherRepository.java` | Spring Data |
| `backend/.../repository/FireExtinguisherRevisionRepository.java` | Spring Data |
| `backend/.../exception/FireExtinguisherNotFoundException.java` | 404 |
| `backend/.../exception/FireExtinguisherRevisionNotFoundException.java` | 404 |
| `backend/.../exception/FireExtinguisherDuplicateNumberException.java` | 409 |
| `backend/.../service/FireExtinguisherService.java` | Business logic |
| `backend/.../controller/FireExtinguisherController.java` | REST controller |
| `frontend/.../models/fire-extinguisher.model.ts` | TypeScript interfaces |
| `frontend/.../core/services/fire-extinguisher.service.ts` | HTTP service |
| `frontend/.../features/building/components/fire-extinguisher-section/fire-extinguisher-section.component.ts` | Section component |
| `frontend/.../features/building/components/fire-extinguisher-section/fire-extinguisher-section.component.html` | Template |
| `frontend/.../features/building/components/fire-extinguisher-section/fire-extinguisher-section.component.scss` | Styles |

### Modified files

| Path | Change |
|---|---|
| `backend/.../exception/GlobalExceptionHandler.java` | Add 3 exception handlers |
| `frontend/.../features/building/components/building-details/building-details.component.ts` | Import section + inject service + load units |
| `frontend/.../features/building/components/building-details/building-details.component.html` | Add `<app-fire-extinguisher-section>` block |

---

## CONSTRAINTS

- No `@author`, no `@version`, no generation timestamp in comments.
- Do **not** regenerate files not listed under "Modified files".
- Do **not** create a V003 — all schema is in V002.
- `FireExtinguisherSectionComponent` is only used inside `BuildingDetailsComponent` — no route needed.
- SCSS: keep component file minimal; rely on global utility classes wherever possible.
- Backend package: `com.immocare` — follow existing package structure.
