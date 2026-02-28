# User Story US038: Add a Meter to a Housing Unit

| Attribute | Value |
|-----------|-------|
| **Story ID** | US038 |
| **Epic** | Meter Management |
| **Related UC** | UC008 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** add a meter to a housing unit **so that** I can track which meters serve the apartment.

## Acceptance Criteria

**AC1:** Add WATER meter: number + installation number + start date → saved, shown in WATER block.
**AC2:** Add GAS meter: number + EAN code + start date → saved.
**AC3:** Add ELECTRICITY meter: number + EAN code + start date → saved.
**AC4:** GAS without EAN → error "EAN code is required for gas meters".
**AC5:** ELECTRICITY without EAN → error "EAN code is required for electricity meters".
**AC6:** WATER without installation number → error "Installation number is required for water meters".
**AC7:** Future start date → error "Start date cannot be in the future".
**AC8:** No `customerNumber` field shown for housing unit context.
**AC9:** Multiple active meters of same type allowed.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/meters` — HTTP 201.

**Last Updated:** 2025-02-23 | **Status:** Ready for Development
