# UC013 — Manage Fire Extinguishers

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC013 |
| **Name** | Manage Fire Extinguishers |
| **Actor** | ADMIN |
| **Epic** | Fire Safety Management |
| **Status** | 📋 Ready for Implementation |

An admin can manage fire extinguishers attached to a building. Each extinguisher carries a unique identification number within the building, can optionally be assigned to a specific housing unit, and has an append-only revision history recording each inspection with its date and notes.

The feature is embedded in the **building details page** as a collapsible section, following the same pattern as the Boiler and Meter sections.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| UC012.001 | Add Fire Extinguisher to Building | MUST HAVE | 3 |
| UC012.002 | Edit Fire Extinguisher | MUST HAVE | 2 |
| UC012.003 | Delete Fire Extinguisher | MUST HAVE | 2 |
| UC012.004 | View Fire Extinguishers List | MUST HAVE | 2 |
| UC012.005 | Add Revision Record | MUST HAVE | 3 |
| UC012.006 | View Revision History | MUST HAVE | 2 |
| UC012.007 | Delete Revision Record | SHOULD HAVE | 1 |

---

## Actors

- **ADMIN**: Only role. Full access to all operations.

---

## Preconditions

- Building exists.
- For UC012.001: the building is open (not deleted).
- For UC012.002, UC012.003: the extinguisher exists.
- For UC012.005, UC012.006, UC012.007: the extinguisher exists.

---

## Main Flows

### Add Fire Extinguisher (UC012.001)
1. Admin opens building details → Fire Extinguishers section → "Add" button.
2. Inline form: identification number (required), unit assignment (optional dropdown), notes (optional).
3. Admin saves → extinguisher created and appears in the list.

### Edit Fire Extinguisher (UC012.002)
1. Admin clicks "Edit" on an extinguisher card.
2. Inline form pre-filled with current values.
3. Admin modifies fields and saves → extinguisher updated.

### Delete Fire Extinguisher (UC012.003)
1. Admin clicks "Delete" on an extinguisher card.
2. Inline confirmation prompt ("Are you sure?").
3. Admin confirms → extinguisher and its full revision history are permanently deleted.

### View Fire Extinguishers List (UC012.004)
1. Admin opens building details.
2. Fire Extinguishers section shows all extinguishers for the building, ordered by identification number.
3. Each card shows: identification number, linked unit (or "—"), notes (truncated), number of revisions, latest revision date (or "Never inspected").

### Add Revision Record (UC012.005)
1. Admin expands a revision history panel → clicks "Add revision".
2. Inline form: revision date (required, not future), notes (optional).
3. Admin saves → revision record added at the top of the history table.

### View Revision History (UC012.006)
1. Admin clicks "View revisions (N)" toggle on an extinguisher card.
2. Table appears: Date | Notes | Actions — ordered by revision date DESC.
3. No revisions → "No revision recorded yet."

### Delete Revision Record (UC012.007)
1. Admin clicks the delete icon on a revision row.
2. Inline confirmation.
3. Admin confirms → revision record permanently deleted; counter on the card updates.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC013-01 | `identificationNumber` must be unique (case-insensitive) within the building |
| BR-UC013-02 | `unitId`, if provided, must belong to the same building as the extinguisher |
| BR-UC013-03 | `revisionDate` must not be in the future |
| BR-UC013-04 | Deleting an extinguisher cascades to all its revision records (no blocking constraint) |
| BR-UC013-05 | Revision records are ordered by `revisionDate` DESC, then `createdAt` DESC |

---

## Data Model

### Table: `fire_extinguisher`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `building_id` | BIGINT | NOT NULL, FK → `building(id)` ON DELETE CASCADE |
| `unit_id` | BIGINT | NULL, FK → `housing_unit(id)` ON DELETE SET NULL |
| `identification_number` | VARCHAR(50) | NOT NULL |
| `notes` | TEXT | NULL |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

**Constraint**: `UNIQUE (building_id, identification_number)`

### Table: `fire_extinguisher_revision`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `fire_extinguisher_id` | BIGINT | NOT NULL, FK → `fire_extinguisher(id)` ON DELETE CASCADE |
| `revision_date` | DATE | NOT NULL |
| `notes` | TEXT | NULL |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

---

## DTOs

### `FireExtinguisherDTO` (response)
```
id
buildingId
unitId            — null if not assigned
unitNumber        — from housing_unit.unit_number, null if not assigned
identificationNumber
notes
revisions         — List<FireExtinguisherRevisionDTO>, ordered by revisionDate DESC
createdAt
updatedAt
```

### `FireExtinguisherRevisionDTO` (response)
```
id
fireExtinguisherId
revisionDate
notes
createdAt
```

### `SaveFireExtinguisherRequest` (POST + PUT body)
```
identificationNumber*   VARCHAR(50)   required
unitId                  Long          optional
notes                   TEXT          optional, max 2000 chars
```

### `AddRevisionRequest` (POST body)
```
revisionDate*   LocalDate   required, not future
notes           TEXT        optional, max 2000 chars
```

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Extinguisher not found | 404 | `Fire extinguisher not found` |
| Revision not found | 404 | `Revision record not found` |
| Duplicate identification number in building | 409 | `An extinguisher with this identification number already exists in this building` |
| `unitId` does not belong to this building | 400 | `The specified unit does not belong to this building` |
| Revision date in future | 400 | `Revision date cannot be in the future` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | `/api/v1/buildings/{buildingId}/fire-extinguishers` | UC012.004 |
| GET | `/api/v1/fire-extinguishers/{id}` | UC012.004 |
| POST | `/api/v1/buildings/{buildingId}/fire-extinguishers` | UC012.001 |
| PUT | `/api/v1/fire-extinguishers/{id}` | UC012.002 |
| DELETE | `/api/v1/fire-extinguishers/{id}` | UC012.003 |
| POST | `/api/v1/fire-extinguishers/{id}/revisions` | UC012.005 |
| DELETE | `/api/v1/fire-extinguishers/{extId}/revisions/{revId}` | UC012.007 |

---

## UC Map Update

Add the following row to `docs/analysis/use-cases/README.md`:

| [UC013](./UC013_manage_fire_extinguishers.md) | Manage Fire Extinguishers | UC012.001–UC012.007 | 📋 Ready for Implementation |

---

**Last Updated:** 2026-03-04 | **Status:** 📋 Ready for Implementation
