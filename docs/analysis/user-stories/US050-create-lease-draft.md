# User Story US050: Create Lease (Draft)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US050 |
| **Epic** | Lease Management |
| **Related UC** | UC010 |
| **Priority** | MUST HAVE |
| **Story Points** | 8 |

**As an** ADMIN **I want to** create a new lease **so that** I can formalize a rental agreement.

## Acceptance Criteria

**AC1:** Unit already has ACTIVE or DRAFT lease → error "An active or draft lease already exists for this unit. Finish or cancel it first."
**AC2:** Form sections: Dates & Duration / Financial / Indexation / Registration / Guarantee & Insurance / Tenants. Unit pre-selected, read-only.
**AC3:** End date auto-calculated from start date + duration months.
**AC4:** Lease type selection pre-fills default notice period (e.g. MAIN_RESIDENCE_9Y → 3 months).
**AC5:** At least one PRIMARY tenant required to save → error "At least one primary tenant is required".
**AC6:** Save as Draft → lease created DRAFT, "Lease saved as draft".
**AC7:** PROVISION charges type → shows "Charges Description" field.
**AC8:** All optional sections (registration, deposit, insurance) truly optional.

**Endpoint:** `POST /api/v1/housing-units/{unitId}/leases` — HTTP 201, default status DRAFT.

**Last Updated:** 2026-02-24 | **Status:** Ready for Development
