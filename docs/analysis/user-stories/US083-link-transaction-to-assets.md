# User Story US083: Link Transaction to Asset(s)

| Attribute | Value |
|-----------|-------|
| **Story ID** | US083 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** link an expense transaction to one or more physical assets **so that** I can trace maintenance and service costs per asset over time.

## Acceptance Criteria

**AC1:** Asset Links section is visible only when `direction = EXPENSE`. Hidden entirely for INCOME transactions.

**AC2:** "Add Asset Link" button adds a new row: asset type selector (BOILER / FIRE_EXTINGUISHER / METER) + asset picker + optional notes field.

**AC3:** Asset picker content depends on asset type:
- **BOILER**: shows active boilers of the linked housing unit. If no unit selected, shows all active boilers of the linked building.
- **FIRE_EXTINGUISHER**: shows all extinguishers of the linked building, with unit number when assigned.
- **METER**: shows all meters of the linked building or unit.
- If neither building nor unit is set: asset picker is disabled with hint "Select a building or unit first."

**AC4:** Multiple asset links allowed on a single transaction. Same asset can appear only once per transaction → duplicate asset → error "This asset is already linked to this transaction."

**AC5:** Asset link for type BOILER where the boiler does not belong to the transaction's building → backend returns HTTP 400: "The selected boiler does not belong to the transaction's building." (BR-UC014-09).

**AC6:** Each link row has a "×" remove button (no confirmation required).

**AC7:** In the boiler details section (housing unit details page): a "Related expenses (N)" badge is shown. Clicking it navigates to the transaction list filtered to `assetType=BOILER&assetId={id}`.

**AC8:** Same badge in the fire extinguisher section on building details page.

**AC9:** Asset links are saved as part of the transaction create/update payload. No separate endpoint.

**Endpoint:** Asset links embedded in `POST /api/v1/transactions` and `PUT /api/v1/transactions/{id}` payloads.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
