# User Story US080: Edit Transaction

| Attribute | Value |
|-----------|-------|
| **Story ID** | US080 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** edit an existing transaction **so that** I can correct errors or complete missing information.

## Acceptance Criteria

**AC1:** "Edit" button visible on the transaction detail view for DRAFT and CONFIRMED transactions.

**AC2:** RECONCILED transaction → "Edit" button absent; a read-only banner displays "This transaction has been reconciled and cannot be modified."

**AC3:** Edit form pre-filled with all current values. Same layout and validation rules as creation (US079).

**AC4:** Fields that cannot be modified: `reference`, `external_reference`, `source`. These are displayed as read-only labels.

**AC5:** Changing `direction` resets `subcategory` if the current subcategory is incompatible with the new direction. A warning is shown: "Subcategory '[name]' is not compatible with direction [INCOME/EXPENSE] and has been cleared."

**AC6:** Changing `building` resets `housing_unit` and `lease` fields. Changing `direction` to EXPENSE clears `lease` field.

**AC7:** On save:
- `updated_at` refreshed.
- Learning rules reinforced if `counterparty_account` and `subcategory` are set (same logic as creation).
- Success message: "Transaction updated."

**AC8:** Cancel → confirmation dialog if form is dirty. No changes persisted on cancel.

**Endpoint:** `PUT /api/v1/transactions/{id}` — HTTP 200, returns updated `FinancialTransactionDTO`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
