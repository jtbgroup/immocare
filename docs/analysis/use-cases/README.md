# ImmoCare — Use Case Documentation Index

## Project

**ImmoCare** — Property management application.  
**Stack:** Spring Boot 3 · Java 17 · Angular 17 · PostgreSQL · Flyway  
**Branch:** `develop`  
**DB Flyway:** single file `V001__initial_schema.sql` (full baseline)

---

## Use Case Map

| UC | Name | US range | Status |
|---|---|---|---|
| [UC001](./UC001_manage_buildings.md) | Manage Buildings | US001–US005 | ✅ Implemented |
| [UC002](./UC002_manage_housing_units.md) | Manage Housing Units | US006–US011 | ✅ Implemented |
| [UC003](./UC003_manage_rooms.md) | Manage Rooms | US012–US016 | ✅ Implemented |
| [UC004](./UC004_manage_peb_scores.md) | Manage PEB Scores | US017–US020 | ✅ Implemented |
| [UC005](./UC005_manage_rents.md) | Manage Rents (Housing Unit) | US021–US025 | ✅ Implemented |
| [UC007](./UC007_manage_users.md) | Manage Users | US031–US035 | ✅ Implemented |
| [UC008](./UC008_manage_meters.md) | Manage Meters | US036–US042 | ✅ Implemented |
| [UC009](./UC009_manage_persons.md) | Manage Persons | US043–US048 | ✅ Implemented |
| [UC010](./UC010_manage_leases.md) | Manage Leases | US049–US059 | 📋 Ready for Implementation |
| [AUTH](./AUTH_security_infrastructure.md) | Security Infrastructure | (cross-cutting) | ✅ Implemented |

> **Note:** UC006 does not exist in this project. UC009 = Manage Persons (not Authentication).

---

## Entity Hierarchy

```
app_user
person
  └── (owner of) building
         └── housing_unit
                ├── room
                ├── peb_score_history
                ├── rent_history          ← standalone unit rent (UC005)
                ├── meter (HOUSING_UNIT)
                └── lease
                       ├── lease_tenant  → person
                       └── lease_rent_adjustment
building
  └── meter (BUILDING)
```

---

## Key Design Decisions

### rent_history vs lease indexation

Two distinct rent concepts:
- `rent_history` (UC005) — the **market rent** of a housing unit, independent of any lease. Timeline managed directly on the unit (US021–US025).
- `lease_indexation_history` (UC010 US055–US056) — formal rent indexations on an active lease. Stored per lease with index values, applied rent, and notification dates.

### Lease indexation (UC010)

Indexation is recorded via US055 (Record Indexation) → `POST /api/v1/leases/{id}/indexations`. It creates a record in `lease_indexation_history` **and** updates `lease.monthly_rent` to the applied rent. The alert logic (US059) detects when an indexation anniversary is approaching and no indexation has been recorded for that year.

### Person.isTenant

Currently a stub (`false`). Will be implemented when the lease → person query is added to `PersonService.buildFullDTO()` and `enrichSummaryFlags()`.

### Housing Unit delete rule

`HousingUnitHasDataException` is thrown (with `roomCount`) only when rooms exist. PEB, rent history, and meters are **not** blocking deletion (cascade DELETE handles them).

---

## Database (V001 — full baseline)

All tables are created in a single Flyway migration `V001__initial_schema.sql`.  
Do **not** create new numbered migrations for features already present in V001.  
For new features: create `V002__<description>.sql`.

Tables in V001:
- `app_user`
- `person`
- `building`
- `housing_unit`
- `room`
- `peb_score_history`
- `rent_history`
- `meter`
- `lease`
- `lease_tenant`
- `lease_rent_adjustment`
