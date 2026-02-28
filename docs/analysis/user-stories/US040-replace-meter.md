# User Story US040: Replace a Meter

| Attribute | Value |
|-----------|-------|
| **Story ID** | US040 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** replace an active meter with a new one **so that** I can track meter changes without losing history.

## Acceptance Criteria

**AC1:** Click "Replace" → form shows current meter data read-only + fields for new meter (number, conditional fields, start date, optional reason).
**AC2:** Atomic operation: current meter closed (endDate = newStartDate), new meter created active.
**AC3:** Both meters appear in history.
**AC4:** Reason dropdown: BROKEN, END_OF_LIFE, UPGRADE, CALIBRATION_ISSUE, OTHER.
**AC5:** newStartDate < current meter's startDate → error "Start date must be on or after the current meter's start date".
**AC6:** Future start date → error "Start date cannot be in the future".
**AC7:** Same conditional field rules as add (EAN/installationNumber/customerNumber).
**AC8:** Cancel → current meter unchanged.

**Endpoints:** `PUT /api/v1/housing-units/{unitId}/meters/{meterId}/replace` and `PUT /api/v1/buildings/{buildingId}/meters/{meterId}/replace`

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
