# User Story US060: Add Boiler to Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US060 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a boiler to a housing unit **so that** I can track the heating equipment installed.

## Acceptance Criteria

**AC1:** Boiler section on housing unit details shows "No boiler registered" + "Add Boiler" button when no active boiler exists.
**AC2:** Click "Add Boiler" → inline form: brand* (required), model (optional), fuel type (optional, dropdown: GAS, OIL, WOOD, PELLET, ELECTRIC, HEAT_PUMP, OTHER), installation date* (required, not future).
**AC3:** Valid data → save → boiler created, section shows boiler details with "Active" badge.
**AC4:** Brand empty → error "Brand is required".
**AC5:** Installation date in future → error "Installation date cannot be in the future".
**AC6:** Active boiler already exists → "Add Boiler" button hidden (replaced by "Replace Boiler").

**Endpoint:** `POST /api/v1/housing-units/{unitId}/boilers` — HTTP 201.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US061: View Active Boiler

| Attribute | Value |
|-----------|-------|
| **Story ID** | US061 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the active boiler of a housing unit **so that** I can quickly see heating equipment details and service validity.

## Acceptance Criteria

**AC1:** Active boiler card shows: brand, model (if set), fuel type (if set), installation date, latest service date, valid until, validity status badge.
**AC2:** No active boiler → "No boiler registered" message + "Add Boiler" button.
**AC3:** Active boiler with no service record → validity badge shows 🔴 "No service recorded".
**AC4:** Validity badge colors: 🔴 Expired / 🟠 Expiring soon / ✅ Valid — thresholds from platform config.
**AC5:** "View History" link visible when at least one boiler exists (active or removed).

**Endpoint:** `GET /api/v1/housing-units/{unitId}/boilers/active` — HTTP 200 or 204 if none.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US062: Replace Boiler

| Attribute | Value |
|-----------|-------|
| **Story ID** | US062 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** replace the active boiler with a new one **so that** I can record equipment changes without losing history.

## Acceptance Criteria

**AC1:** "Replace Boiler" button shown on active boiler card.
**AC2:** Form shows current boiler info (read-only) + two sections: "Remove current boiler" (removal date*) and "New boiler" (brand*, model, fuel type, installation date*).
**AC3:** Confirm → atomic operation: current boiler gets removal_date set, new boiler created active. Both in history.
**AC4:** Removal date < current boiler's installation date → error "Removal date must be on or after the boiler installation date".
**AC5:** New installation date < removal date → error "New boiler installation date must be on or after the removal date".
**AC6:** Cancel → no changes made.

**Endpoint:** `PUT /api/v1/housing-units/{unitId}/boilers/active/replace` — HTTP 200 with new boiler DTO.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US063: View Boiler History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US063 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the history of boilers for a housing unit **so that** I can track all past equipment.

## Acceptance Criteria

**AC1:** "View History" toggle in boiler section reveals full history table.
**AC2:** Columns: brand, model, fuel type, installation date, removal date, status badge (Active / Removed).
**AC3:** Ordered by installation date DESC.
**AC4:** Active boiler shown with green "Active" badge; removed boilers with grey "Removed" badge + removal date.
**AC5:** No boiler ever registered → history toggle not shown.

**Endpoint:** `GET /api/v1/housing-units/{unitId}/boilers` — returns all boilers for the unit.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US064: Add Boiler Service Record

| Attribute | Value |
|-----------|-------|
| **Story ID** | US064 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** record a boiler service **so that** I can track maintenance history and validity.

## Acceptance Criteria

**AC1:** "+ Add Service" button visible on the active boiler's service section.
**AC2:** Form: service date* (required, not future), valid until (auto-filled, editable), notes (optional).
**AC3:** On service date change → valid_until auto-recalculates using the validity rule in effect on that date. User sees the calculated date before saving.
**AC4:** Admin can manually override valid_until (any date accepted).
**AC5:** Service date in future → error "Service date cannot be in the future".
**AC6:** Save → record appears at top of service history; validity badge on boiler card updates.

**Endpoint:** `POST /api/v1/boilers/{boilerId}/services` — HTTP 201.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US065: View Boiler Service History

| Attribute | Value |
|-----------|-------|
| **Story ID** | US065 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** view the service history of a boiler **so that** I can review past maintenance records.

## Acceptance Criteria

**AC1:** Service history section shown on the active boiler card (and in full history view per boiler).
**AC2:** Columns: service date, valid until, notes, status badge.
**AC3:** Ordered by service date DESC.
**AC4:** Each row shows validity status badge (Expired / Expiring soon / Valid).
**AC5:** No service records → "No service recorded" with 🔴 badge.

**Endpoint:** `GET /api/v1/boilers/{boilerId}/services` — returns all service records for the boiler.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development

---

# User Story US066: View Boiler Service Validity Alert

| Attribute | Value |
|-----------|-------|
| **Story ID** | US066 |
| **Epic** | Boiler Management |
| **Related UC** | UC011 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** see a validity alert on the boiler **so that** I know when a service is due or overdue.

## Acceptance Criteria

**AC1:** Alert based on most recent service record's valid_until date.
**AC2:** valid_until < today → 🔴 "Expired" badge.
**AC3:** valid_until within alert threshold (from platform_config `boiler.service.alert.threshold.months`) → 🟠 "Expiring soon — due by [date]".
**AC4:** valid_until beyond threshold → ✅ "Valid until [date]".
**AC5:** No service record exists → 🔴 "No service recorded".
**AC6:** Alert visible on housing unit details page (boiler card) without needing to expand any section.

**Last Updated:** 2026-03-01 | **Status:** Ready for Development
