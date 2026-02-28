# User Story US041: Remove a Meter

| Attribute | Value |
|-----------|-------|
| **Story ID** | US041 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** remove an active meter without replacing it **so that** I can handle disconnected meters.

## Acceptance Criteria

**AC1:** Click "Remove" → confirmation dialog: meter type/number + warning "This meter will be deactivated. No replacement will be created." + date picker (default: today).
**AC2:** Confirm → meter endDate set, no longer shown as active, still visible in history with "Closed" badge.
**AC3:** Other active meters on same unit/building unaffected.
**AC4:** endDate < meter startDate → error "End date must be on or after the meter's start date".
**AC5:** Future endDate → error "End date cannot be in the future".
**AC6:** Cancel → meter remains active.

**Endpoints:** `DELETE /api/v1/housing-units/{unitId}/meters/{meterId}` and `DELETE /api/v1/buildings/{buildingId}/meters/{meterId}` — body: `{ endDate }`.

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
