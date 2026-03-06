# User Story US087: Manage Bank Account Catalog

| Attribute | Value |
|-----------|-------|
| **Story ID** | US087 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** manage the catalog of own bank accounts **so that** I can associate transactions with the correct account and filter/analyze by account.

## Acceptance Criteria

**AC1:** "Bank Accounts" sub-section in the Settings tab of the Transactions page.

**AC2:** Table lists all bank accounts ordered by label: label, IBAN (formatted), type badge (CURRENT / SAVINGS), active status toggle, action buttons.

**AC3:** "Add Bank Account" → inline form row: label* (required, unique case-insensitive), IBAN* (required, valid IBAN format), type* (CURRENT / SAVINGS selector), active (checkbox, default checked).

**AC4:** Duplicate label (case-insensitive) → error "A bank account with this label already exists."

**AC5:** Duplicate IBAN → error "This IBAN is already registered."

**AC6:** "Edit" → inline edit of all fields.

**AC7:** Active toggle: unchecking deactivates the account. Inactive accounts are hidden from transaction form dropdowns (create/edit/import) but remain visible on existing transactions and in this settings table.

**AC8:** No delete button — bank accounts are deactivated, never deleted, to preserve transaction history integrity.

**AC9:** At least one active bank account should exist; deactivating the last active account → warning "This is your last active bank account. Are you sure?" (warning only, not blocked).

**AC10:** On CSV import (US084): if the value parsed from the bank account column exactly matches a known IBAN in the catalog, the account is auto-assigned to the transaction.

**Endpoints:**
- `GET /api/v1/bank-accounts` — returns all accounts (query param `?activeOnly=true` for dropdown usage).
- `POST /api/v1/bank-accounts` — HTTP 201.
- `PUT /api/v1/bank-accounts/{id}` — HTTP 200.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
