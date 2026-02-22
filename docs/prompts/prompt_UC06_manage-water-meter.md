# Prompt — Implementation of UC006 and US026–US030: Water Meter Management

## Context

Branch: `develop`

Implement the water meter management feature for housing units, covering the full use case UC006 and its associated user stories US026 to US030.

---

## Scope

### UC006 — Manage Water Meters

Implement all flows described in UC006:

- View current water meter and history on the housing unit details page
- Assign a first water meter to a unit (no existing meter)
- Replace an active meter with a new one (atomic operation: close old, open new)
- Remove a meter without replacement (set removal date, unit becomes meterless)
- View meter detail (number, location, dates, duration, status)

Business rules to enforce:
- One active meter maximum per unit at any time
- Installation date cannot be in the future
- Replacement installation date must be >= current meter's installation date
- Removal date must be >= installation date
- Meter number format: alphanumeric with optional hyphens/underscores (max 50 chars)
- Location is optional (max 100 chars)
- Warn (non-blocking) if new meter number equals current meter number

---

### US026 — Assign Water Meter

- Show "No water meter assigned" + "Assign Meter" button when no meter exists
- Form: Meter Number (required), Location (optional), Installation Date (required)
- Validations: required fields, date not in future, valid format
- On success: meter saved as active (removal_date = NULL), unit details updated

### US027 — Replace Water Meter

- "Replace Meter" button visible when active meter exists
- Form: current meter shown read-only, new meter fields, reason dropdown (Broken / End of life / Upgrade / Calibration issue / Other)
- On save: old meter removal_date set to new meter's installation date, new meter becomes active
- Both records preserved in history

### US028 — View Water Meter History

- "View History" link on unit details
- History table columns: Meter Number, Location, Installation Date, Removal Date (or "Active"), Duration, Status
- Sorted by installation date DESC
- Active meter: green "Active" badge; replaced meters: gray "Replaced" badge
- Duration calculated (e.g., "5 months")

### US029 — Remove Water Meter

- "Remove Meter" button visible when active meter exists
- Confirmation dialog: meter number shown, warning that unit will have no active meter
- Date picker for removal date (default: today)
- Validation: removal date >= installation date
- On confirm: meter removal_date updated, unit shows "No water meter assigned"

### US030 — Track Meter Installation Dates

- Display "Installed: [date]" and "Active for: X months" on unit details
- In history: show calculated duration per meter period
- Optional: display warning badge if meter is older than 10 years

---

## Technical Requirements

- Backend endpoint: `GET /api/v1/housing-units/{id}/meters` — meter history
- Backend endpoint: `POST /api/v1/housing-units/{id}/meters` — assign first meter
- Backend endpoint: `PUT /api/v1/housing-units/{id}/meters/replace` — replace meter
- Backend endpoint: `DELETE /api/v1/housing-units/{id}/meters/active` — remove meter
- Performance: all meter operations < 500ms
- Security: only ADMIN role can create/update/delete; authenticated users can read

---

## Test Scenarios to Cover

TS-UC006-01 through TS-UC006-10 as defined in UC006, including:
- Assign first meter
- Replace meter (automatic removal date)
- Future installation date error
- View history with durations
- Remove meter
- Same meter number warning
- Invalid meter number format error
- Backdate installation error
- View active meter on unit details

---

## Out of Scope (Phase 2)

- Electricity, gas, and heating meters (BACKLOG-012, 013, 014)
- Meter reading management (BACKLOG-015)
- Automatic consumption calculation
- Anomaly detection