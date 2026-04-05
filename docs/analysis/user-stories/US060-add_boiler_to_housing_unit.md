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