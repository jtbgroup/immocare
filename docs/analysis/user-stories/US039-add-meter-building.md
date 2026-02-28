# User Story US039: Add a Meter to a Building

| Attribute | Value |
|-----------|-------|
| **Story ID** | US039 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a meter to a building **so that** I can track which meters serve the common areas.

## Acceptance Criteria

**AC1:** Add WATER meter to building: number + installation number + customer number + start date → saved with customer number shown.
**AC2:** WATER without customer number → error "Customer number is required for water meters on a building".
**AC3:** WATER without installation number → error "Installation number is required for water meters".
**AC4:** GAS without EAN → error "EAN code is required for gas meters".
**AC5:** ELECTRICITY without EAN → error "EAN code is required for electricity meters".
**AC6:** Future start date → error "Start date cannot be in the future".

**Endpoint:** `POST /api/v1/buildings/{buildingId}/meters` — HTTP 201.

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
