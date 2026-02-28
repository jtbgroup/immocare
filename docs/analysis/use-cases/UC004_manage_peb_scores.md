# UC004 — Manage PEB Scores

## Overview

| Attribute | Value |
|---|---|
| ID | UC004 |
| Name | Manage PEB Scores |
| Actor | Admin |
| Module | Housing Units → PEB |
| Stack | Spring Boot · Angular · PostgreSQL |
| Branch | develop |

## Description

Allows an administrator to record and track the energy performance certificate (PEB — *Performance Énergétique des Bâtiments*) history of a housing unit. PEB scores are **append-only** by design — no edits or deletes. The current score is the most recent by `scoreDate`. Certificate validity is monitored and an improvement summary is available.

---

## User Stories

### US017 — Add PEB Score

**As an** admin, **I want to** record a PEB score for a housing unit **so that** I can track the unit's energy performance.

**Acceptance Criteria:**
- AC1: The add PEB form is displayed when I click "Add PEB Score"; required fields are marked.
- AC2: The score dropdown shows all 9 options (A++ to G) with color coding: A++ dark green → G dark red.
- AC3: First score: saved and displayed as current score on unit details.
- AC4: Newer score (more recent `scoreDate`): becomes the current score; previous becomes historical.
- AC5: All optional fields stored: `certificateNumber`, `validUntil`.
- AC6: Validation — `scoreDate` is required; shows "Score date is required".
- AC7: Validation — `scoreDate` cannot be in the future; shows "Score date cannot be in the future".

**Endpoint:** `POST /api/v1/housing-units/{unitId}/peb-scores`

---

### US018 — View PEB Score History

**As an** admin, **I want to** view the complete PEB score history of a housing unit **so that** I can track energy efficiency improvements over time.

**Acceptance Criteria:**
- AC1: The PEB section on unit details shows the current score badge, score date, and a "View History" link.
- AC2: Clicking "View History" displays a table/modal with all scores sorted by date (newest first).
- AC3: History table columns: Score (with color badge), Score Date, Certificate Number, Valid Until, Status (Current / Historical / Expired).
- AC4: Optional visual timeline showing progression (e.g., D → C → B).

**Endpoints:**
- `GET /api/v1/housing-units/{unitId}/peb-scores` — full history
- `GET /api/v1/housing-units/{unitId}/peb-scores/current` — current score only (204 if none)

---

### US019 — Check PEB Certificate Validity

**As an** admin, **I want to** see if PEB certificates are expired or expiring soon **so that** I can plan for certificate renewals.

**Acceptance Criteria:**
- AC1: `validUntil` in the past → "Expired" warning in red; suggestion "Certificate needs renewal".
- AC2: `validUntil` within 3 months → "Expires soon" warning in orange with expiry date highlighted.
- AC3: `validUntil` > 3 months away → no warning shown; valid until date displayed normally.
- AC4: No `validUntil` → no expiry warning; displays "Validity period not specified".

---

### US020 — Track PEB Score Improvements

**As an** admin, **I want to** see if PEB scores have improved over time **so that** I can evaluate the impact of energy efficiency renovations.

**Acceptance Criteria:**
- AC1: Improvement indicator: D→C = "Improved" (green ↑), C→B = "Improved" (green ↑).
- AC2: Degradation indicator: B→C = "Degraded" (red ↓).
- AC3: No change: consecutive equal scores = "No change" (gray −).
- AC4: Total summary: "Improved by 4 grades over 6 years" (from first to current).

**Endpoint:** `GET /api/v1/housing-units/{unitId}/peb-scores/improvements`

---

## Data Model

### Table: `peb_score_history`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `housing_unit_id` | BIGINT | NOT NULL, FK → `housing_unit.id` ON DELETE CASCADE |
| `peb_score` | VARCHAR(10) | NOT NULL, CHECK IN (A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G) |
| `score_date` | DATE | NOT NULL |
| `certificate_number` | VARCHAR(50) | nullable |
| `valid_until` | DATE | nullable |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

---

## DTOs

### `PebScoreDTO` (response)
```
id, housingUnitId, pebScore, scoreDate, certificateNumber, validUntil,
createdAt, isCurrent, status, expiryWarning
```

### `PebImprovementDTO` (response)
```
firstScore: PebScore
currentScore: PebScore
improved: boolean
jumpedCategories: int
```

### `CreatePebScoreRequest` (POST/PUT body)
```
pebScore*         PebScore enum
scoreDate*        LocalDate  (not future)
certificateNumber VARCHAR(50)
validUntil        LocalDate  (must be after scoreDate)
```

---

## Enums

### `PebScore`
```
A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G
```
Ordered from best (A_PLUS_PLUS) to worst (G) for improvement calculation.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC004-01 | PEB scores are append-only — no update or delete endpoints |
| BR-UC004-02 | `isCurrent` = score with the most recent `scoreDate` |
| BR-UC004-03 | `scoreDate` cannot be in the future |
| BR-UC004-04 | `validUntil`, if provided, must be after `scoreDate` |
| BR-UC004-05 | Multiple scores on the same date are allowed (correction use case) |
| BR-UC004-06 | `status = EXPIRING_SOON` when `validUntil` is within 3 months of today |
| BR-UC004-07 | `status = EXPIRED` when `validUntil` is in the past |

---

## Error Responses

| Condition | HTTP | Error key |
|---|---|---|
| Unit not found | 404 | `HousingUnitNotFoundException` |
| `scoreDate` in future | 400 | `Invalid PEB score date` |
| `validUntil` ≤ `scoreDate` | 400 | `Invalid validity period` |

---

## Generation Prompt

```
Generate the complete Spring Boot + Angular implementation for UC004 — Manage PEB Scores in the ImmoCare application.

Stack:
- Backend: Spring Boot 3, Java 17, Spring Data JPA, PostgreSQL, MapStruct, Spring Security (ROLE_ADMIN)
- Frontend: Angular 17 standalone components, TypeScript, SCSS
- Database: Flyway V001 already contains `peb_score_history` table — do NOT generate a migration
- Branch: develop

User Stories: US017 (Add PEB Score), US018 (View History), US019 (Certificate Validity), US020 (Track Improvements)

Important design constraint: PEB scores are APPEND-ONLY. There are NO update (PUT) or delete (DELETE) endpoints.

Backend classes to generate:
1. Enum: `PebScore` — A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G (ordered list for improvement diff)
2. Entity: `PebScoreHistory` — table `peb_score_history`. Fields: id, housingUnit (ManyToOne), pebScore (String), scoreDate, certificateNumber, validUntil, createdAt (@PrePersist only, no updatedAt).
3. DTOs: `PebScoreDTO` (with isCurrent, status, expiryWarning — computed in service), `PebImprovementDTO`, `CreatePebScoreRequest` (validated)
4. Mapper: `PebScoreMapper` (MapStruct) — map housingUnit.id → housingUnitId, ignore computed fields
5. Repository: `PebScoreRepository` — findByHousingUnitIdOrderByScoreDateDesc, findFirstByHousingUnitIdOrderByScoreDateDesc
6. Service: `PebScoreService` — addScore(unitId, req), getHistory(unitId), getCurrentScore(unitId), getImprovementSummary(unitId). Enforce BR-UC004-01 to 07. `isCurrent` computed by comparing scoreDate with max. `status` computed from validUntil vs today (EXPIRED / EXPIRING_SOON within 3 months / VALID). Improvement summary compares ordinal positions in PebScore enum.
7. Exceptions: `InvalidPebScoreDateException`, `InvalidValidityPeriodException`
8. Controller: `PebScoreController` — @RequestMapping("/api/v1/housing-units/{unitId}/peb-scores"). Endpoints: POST (US017), GET (US018), GET /current (US018), GET /improvements (US020). No PUT or DELETE endpoints.

Frontend classes to generate:
1. Model: `peb-score.model.ts` — PebScore enum, PEB_SCORE_DISPLAY map (label + color + textColor for each score), PebScoreHistory, PebImprovementDTO, CreatePebScoreRequest
2. Service: `PebScoreService` — addScore(unitId, req), getHistory(unitId), getCurrentScore(unitId), getImprovementSummary(unitId)
3. Component: `PebSectionComponent` (standalone, [unitId] input, inside HousingUnitDetailsComponent)
   - Shows current score as a colored badge with validity status (VALID / EXPIRING_SOON / EXPIRED) — US019
   - "View History" link opens history table (US018): columns Score, Date, Certificate, Valid Until, Status, Improvement indicator
   - Improvement summary shown if ≥ 2 records: "Improved by X grades over Y years" — US020
   - Inline add form: score picker (color-coded), date (max today), certificate number (optional), valid until (optional) — US017

Business rules to enforce in frontend:
- scoreDate: today's date as max (BR-UC004-03)
- validUntil: must be after scoreDate (BR-UC004-04)
- Display EXPIRING_SOON as orange badge, EXPIRED as red badge (US019)
- Per-row improvement indicator in history: ↑ green / ↓ red / − grey (US020)
```
