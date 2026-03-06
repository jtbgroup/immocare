# User Story US079: Create Transaction Manually

| Attribute | Value |
|-----------|-------|
| **Story ID** | US079 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** manually create a financial transaction **so that** I can record cash movements that are not imported from a bank export.

## Acceptance Criteria

**AC1:** "New Transaction" button on the transaction list navigates to `/transactions/new`.

**AC2:** Form is divided into sections:
- **General**: direction (INCOME / EXPENSE, required radio), execution date (required), accounting month (month picker, required), amount (required, positive decimal), description (optional textarea)
- **Counterparty**: name (optional), account number / IBAN (optional), bank account (dropdown from active catalog entries, optional)
- **Classification**: category (required dropdown), subcategory (required dropdown filtered to category + direction compatibility)
- **Links**: building (optional dropdown), housing unit (optional, cascading from building, cleared when building changes); lease (optional, shown only when direction = INCOME, dropdown of ACTIVE leases filtered to selected unit or building)
- **Assets** (visible only when direction = EXPENSE): repeatable rows — asset type selector (BOILER / FIRE_EXTINGUISHER / METER) + asset picker (filtered to building/unit context) + notes (optional)

**AC3:** `accounting_month` is auto-proposed on page load and whenever `subcategory` or `counterparty_account` changes, using accounting month rules (BR-UC014-08). Proposed value shown pre-filled; admin can override freely.

**AC4:** Amount must be a positive number. Zero or negative → validation error "Amount must be greater than zero."

**AC5:** When direction = INCOME, the Assets section is hidden. When direction = EXPENSE, the Lease field is hidden.

**AC6:** Subcategory dropdown filters to entries compatible with the selected direction (BOTH or matching direction). Changing direction resets subcategory if incompatible.

**AC7:** Housing unit dropdown populated only after a building is selected. Selecting a unit automatically sets `building_id` server-side (BR-UC014-03).

**AC8:** On save:
- Transaction created with status CONFIRMED, source MANUAL.
- Reference auto-generated as `TXN-{YYYY}-{NNNNN}`.
- If `counterparty_account` non-empty and subcategory selected: tag learning rule created or reinforced (confidence +1).
- If `accounting_month` differs from month of `transaction_date` and subcategory selected: accounting month rule created or reinforced.
- Success message: "Transaction [reference] created."
- Redirect to transaction detail.

**AC9:** Lease link with direction = EXPENSE → validation error "Lease link is only allowed for income transactions." (BR-UC014-02).

**AC10:** Cancel button → confirmation dialog if form is dirty ("Discard changes?"). Confirmed cancel navigates back to list.

**Endpoint:** `POST /api/v1/transactions` — HTTP 201, returns `FinancialTransactionDTO`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
