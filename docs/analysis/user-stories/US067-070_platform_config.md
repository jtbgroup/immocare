# User Story US067: View Platform Settings

| Attribute | Value |
|-----------|-------|
| **Story ID** | US067 |
| **Epic** | Platform Administration |
| **Related UC** | UC012 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** access a dedicated Platform Settings page **so that** I can manage application-wide configuration.

## Acceptance Criteria

**AC1:** "Platform Settings" menu item visible in the Administration section of the navigation (alongside "Users").
**AC2:** Page has two clearly separated sections: "Boiler Service Validity Rules" and "General Settings".
**AC3:** Boiler Service Validity Rules section shows the current rule highlighted and a full history table.
**AC4:** General Settings section shows all configurable parameters with current values.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US068: Add Boiler Service Validity Rule

| Attribute | Value |
|-----------|-------|
| **Story ID** | US068 |
| **Epic** | Platform Administration |
| **Related UC** | UC012 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a new boiler service validity rule **so that** I can reflect regulation changes over time.

## Acceptance Criteria

**AC1:** "Add Rule" button in the Boiler Service Validity Rules section.
**AC2:** Form: valid from date* (required), duration in months* (required, > 0), description (optional).
**AC3:** Save → new rule appears in history table; if valid_from ≤ today and is the most recent, it becomes the current rule (highlighted).
**AC4:** valid_from already used by another rule → error "A validity rule already exists for this date".
**AC5:** Duration ≤ 0 → error "Validity duration must be greater than zero".
**AC6:** Existing boiler service records are NOT retroactively modified.
**AC7:** Created by and created at shown in history table.

**Endpoint:** `POST /api/v1/config/boiler-validity-rules` — HTTP 201.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US069: View Boiler Service Validity Rules History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US069 |
| **Epic** | Platform Administration |
| **Related UC** | UC012 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view all historical validity rules **so that** I can understand how the regulation has evolved.

## Acceptance Criteria

**AC1:** Table shows all rules ordered by valid_from DESC.
**AC2:** Columns: valid from, duration (months), description, created by, created at.
**AC3:** Current rule (valid_from ≤ today, most recent) has a "Current" badge.
**AC4:** Future rules (valid_from > today) have a "Scheduled" badge.
**AC5:** Rules have no edit or delete buttons — read-only after creation.
**AC6:** Seed rule (1900-01-01, 24 months) visible as the oldest entry.

**Endpoint:** `GET /api/v1/config/boiler-validity-rules` — HTTP 200.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US070: Update General Alert Settings

| Attribute | Value |
|-----------|-------|
| **Story ID** | US070 |
| **Epic** | Platform Administration |
| **Related UC** | UC012 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** update the alert thresholds **so that** I can control how early warnings appear in the application.

## Acceptance Criteria

**AC1:** General Settings section shows: "Boiler service expiry warning threshold" with current value (e.g., "3 months") and an "Edit" button.
**AC2:** Click "Edit" → inline form with the current value pre-filled.
**AC3:** Save valid value → config updated, new value shown immediately.
**AC4:** Value ≤ 0 or not an integer → error "Value must be a positive integer".
**AC5:** Cancel → original value restored, no change saved.
**AC6:** Updated at timestamp and updated by username shown next to each setting.

**Endpoint:** `PUT /api/v1/config/settings/{key}` — HTTP 200.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development
