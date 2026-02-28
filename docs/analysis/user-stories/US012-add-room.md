# User Story US012: Add Room to Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US012 |
| **Epic** | Room Management |
| **Related UC** | UC003 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add rooms to a housing unit **so that** I can define the composition of the apartment.

## Acceptance Criteria

**AC1:** Click "Add Room" → form shown with type dropdown + surface field.
**AC2:** Select "Bedroom" + 15.5m² → save → room created, success message, room appears in list, total surface recalculated.
**AC3:** Type dropdown shows: Living Room, Bedroom, Kitchen, Bathroom, Toilet, Hallway, Storage, Office, Dining Room, Other.
**AC4:** Leave type unselected → error "Room type is required".
**AC5:** Leave surface empty → error "Surface is required".
**AC6:** Unit already has 1 bedroom → add 2nd bedroom → both saved (multiple rooms of same type allowed).

**Endpoint:** `POST /api/v1/housing-units/{unitId}/rooms` — HTTP 201.

**Last Updated:** 2024-01-15 | **Status:** Ready for Development
