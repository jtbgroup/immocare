# User Story US071: Add Fire Extinguisher to Building

| Attribute | Value |
|-----------|-------|
| **Story ID** | US071 |
| **Epic** | Fire Safety Management |
| **Related UC** | UC013 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a fire extinguisher to a building **so that** I can track the fire safety equipment on site.

## Acceptance Criteria

**AC1:** Fire Extinguishers section on building details shows "No fire extinguishers registered." + "Add" button when the list is empty.
**AC2:** Click "Add" → inline form appears: Identification Number* (required), Unit (optional dropdown listing units of the building), Notes (optional textarea).
**AC3:** Valid data → save → extinguisher created, appears in the list with the entered details.
**AC4:** Identification number empty → error "Identification number is required".
**AC5:** Identification number already used on this building (case-insensitive) → error "An extinguisher with this identification number already exists in this building".
**AC6:** Unit dropdown selected → saved extinguisher shows unit number linked to unit details. No unit selected → shows "—".
**AC7:** Cancel → form closes, no extinguisher created.

**Endpoint:** `POST /api/v1/buildings/{buildingId}/fire-extinguishers` — HTTP 201.

**Last Updated:** 2026-03-04 | **Status:** Ready for Development
