# UC014 — Manage Financial Transactions

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC014 |
| **Name** | Manage Financial Transactions |
| **Actor** | ADMIN |
| **Epic** | Financial Management |
| **Flyway** | V014 |
| **Status** | ✅ Implemented |
| **Branch** | develop |

An admin can track all financial inflows (income) and outflows (expenses) related to the management of real estate properties. Transactions can be entered manually or imported from a bank file (CSV or PDF) using a registered parser (see UC015). Each transaction is classified via a two-level tag hierarchy (Category → Subcategory), linked to a bank account from the catalog, assigned an accounting month, and optionally linked to existing domain objects (leases, buildings, housing units, boilers, fire extinguishers, meters). The system learns classification rules and accounting month offsets from admin confirmations to accelerate future imports.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US078 | View Transaction List | MUST HAVE | 3 |
| US079 | Create Transaction Manually | MUST HAVE | 5 |
| US080 | Edit Transaction | MUST HAVE | 3 |
| US081 | Delete Transaction | MUST HAVE | 2 |
| US082 | Classify Transaction (Category / Subcategory) | MUST HAVE | 3 |
| US083 | Link Transaction to Asset(s) | SHOULD HAVE | 3 |
| US084 | Import Transactions (CSV / PDF) | MUST HAVE | 8 |
| US085 | Review and Confirm Imported Transactions | MUST HAVE | 5 |
| US086 | Manage Tag Catalog (Categories & Subcategories) | MUST HAVE | 3 |
| US087 | Manage Bank Account Catalog | MUST HAVE | 2 |
| US088 | View Financial Summary and Statistics | SHOULD HAVE | 5 |
| US089 | Export Transactions to CSV | SHOULD HAVE | 2 |

---

## Domain Concepts

### Transaction Direction
- **INCOME**: money received — rent payments, deposits received, annuity returns, etc.
- **EXPENSE**: money paid out — invoices, maintenance, taxes, deposit refunds, annuity payments, etc.

### Two-Level Classification
- **Category**: top-level grouping (e.g. Administration, Maintenance, Location, Travaux, Taxes, Dépôt).
- **Subcategory**: belongs to one category; carries a direction compatibility flag (INCOME / EXPENSE / BOTH).

### Transaction Date vs Accounting Month
- **transaction_date**: actual bank execution date.
- **accounting_month**: month to which the transaction is attributed for reporting (stored as the 1st of the month, e.g. `2026-02-01`). May differ from `transaction_date` — e.g. rent received on Jan 28 attributed to February.

### Import Flow (3 steps)
1. **Form** — admin selects a parser (from UC015 registry) and uploads a file.
2. **Preview** — file is parsed server-side; rows returned with duplicate flags, subcategory suggestions, lease suggestions. No persistence. Admin can enrich each row via a side panel (subcategory, building, unit, lease, bank account, direction override).
3. **Import** — admin triggers import with enrichments. Enriched rows saved as CONFIRMED; unenriched rows saved as DRAFT. Admin redirected to batch review page (US085).

### Fingerprint Deduplication
Each parsed row produces a SHA-256 fingerprint from `(transactionDate + amount + counterpartyAccount + description)`. A row is flagged as DUPLICATE if the fingerprint already exists in `financial_transaction.import_fingerprint`. Duplicate rows are shown in the preview but excluded from import.

### Lease Suggestion
During preview and import, the service attempts to match `counterparty_account` against known person IBANs (UC016). If a match is found, the most suitable lease is suggested: exact active date range preferred; closest end date used for historical matches. The suggestion includes `leaseId`, `unitId`, `buildingId`, and `personFullName` for UI pre-fill.

---

## User Story Details

### US078 — View Transaction List
1. Paginated list with filters: direction, status, accounting month range, building, bank account, category, subcategory, search (counterparty name / description).
2. Columns: reference, date, accounting month, direction badge, amount, counterparty name, category/subcategory, bank account, building/unit, status badge.
3. Summary totals (income / expenses / net) computed over the full filtered set, not just the current page.
4. Sort on any column; default: transaction_date DESC.

### US079 — Create Transaction Manually
1. Form sections: General (direction, execution date, accounting month, amount, description), Counterparty (name, IBAN, bank account), Classification (category, subcategory filtered by direction), Links (building, unit cascading, lease — INCOME only), Assets (EXPENSE only).
2. `accounting_month` auto-proposed from accounting_month_rule on subcategory/counterparty change; admin can override.
3. On save: status = CONFIRMED, source = MANUAL, reference auto-generated `TXN-{YYYY}-{NNNNN}`. Learning rules reinforced.

### US080 — Edit Transaction
1. All fields editable except `reference`, `external_reference`, `source`, `import_fingerprint`.
2. Blocked if status = RECONCILED.
3. Learning rules reinforced on save.

### US081 — Delete Transaction
1. Confirmation dialog required.
2. Blocked if status = RECONCILED.

### US082 — Classify Transaction
1. Inline category + subcategory dropdowns on transaction list row.
2. Subcategory filtered by direction compatibility.
3. Learning rule reinforced on save.

### US083 — Link Transaction to Asset(s)
1. Repeatable asset link rows: asset type (BOILER / FIRE_EXTINGUISHER / METER) + asset picker (filtered to building/unit context) + notes.
2. BOILER links: boiler must belong to a unit of the transaction's building (BR-UC014-09).

### US084 — Import Transactions (CSV / PDF)
1. Import tab shows: parser selector (from active parsers — UC015), file upload zone (drag-and-drop or browse). File extension validated against parser format.
2. After file + parser selection, "Preview" button calls `POST /api/v1/transactions/preview`. Preview table shows: row number, date, amount, direction badge, description, counterparty name, counterparty account, duplicate flag, subcategory suggestion (with confidence), lease suggestion.
3. Each row has a side panel (detail panel) for enrichment: direction override, bank account, subcategory, building, unit, lease. Accepting a lease suggestion pre-fills building and unit dropdowns. Lease dropdown loads all statuses (DRAFT / ACTIVE / FINISHED / CANCELLED) to support historical imports.
4. "Import selected rows" button calls `POST /api/v1/transactions/import` with the file, parserCode, bankAccountId, and per-row enrichments for selected fingerprints.
5. Enriched rows → CONFIRMED. Unenriched rows → DRAFT. Duplicate rows skipped.
6. Result banner: "Imported: X | Duplicates skipped: Y". "Review" button navigates to batch review (US085).

### US085 — Review and Confirm Imported Transactions
1. Batch review page at `/transactions/import/{batchId}`. Shows all rows with their status.
2. Each DRAFT row: inline accounting month picker, subcategory dropdowns, building/unit/lease dropdowns, confirm/reject buttons. Lease suggestion column shows ✓ (confirmed) or ? (suggestion) badge.
3. "Confirm" per row → CONFIRMED, learning rules reinforced.
4. "Reject" per row → CANCELLED, row struck through.
5. "Confirm All" → confirmation dialog → all DRAFT rows confirmed in one batch call.
6. Page accessible at any time; already CONFIRMED/CANCELLED rows shown read-only.

### US086 — Manage Tag Catalog
1. Settings tab → "Categories & Subcategories" sub-section.
2. Add/edit/delete categories. Delete blocked if subcategories exist.
3. Add/edit/delete subcategories. Each has: name, category, direction (INCOME/EXPENSE/BOTH), description.
4. Delete subcategory blocked if used on any transaction.

### US087 — Manage Bank Account Catalog
1. Settings tab → "Bank Accounts" sub-section.
2. Fields: label, IBAN, type (CURRENT/SAVINGS), active flag, owner (optional AppUser link).
3. Inactive accounts hidden from dropdowns but preserved on existing transactions.
4. No delete — deactivate only.

### US088 — View Financial Summary and Statistics
1. Dashboard filters: accounting month range (default: current year), building, bank account, direction.
2. Summary cards: Total Income, Total Expenses, Net Balance.
3. Breakdown by subcategory (grouped by category), by building, by housing unit, by bank account.
4. Monthly trend: income vs expenses per accounting month.
5. All aggregations use `accounting_month`, not `transaction_date`.

### US089 — Export Transactions to CSV
1. Applies active list filters.
2. Columns: reference, external_reference, transaction_date, accounting_month, direction, amount, category, subcategory, bank_account_label, counterparty_name, counterparty_account, building, housing_unit, lease_reference, status, source.
3. UTF-8 BOM, filename: `immocare_transactions_YYYY-MM-DD.csv`.

---

## Data Model — Tables (V014)

| Table | Purpose |
|---|---|
| `bank_account` | Catalog of own bank accounts (label, IBAN, type, active, owner_user_id) |
| `tag_category` | Top-level classification groups |
| `tag_subcategory` | Fine-grained classification, belongs to a category |
| `financial_transaction` | Core transaction record |
| `transaction_asset_link` | Links a transaction to a physical asset (polymorphic) |
| `import_batch` | Tracks an import operation (parser, bank account, counts) |
| `tag_learning_rule` | Learns subcategory suggestions from counterparty/description patterns |
| `accounting_month_rule` | Learns accounting month offsets from subcategory + counterparty patterns |

### Key columns on `financial_transaction`

| Column | Type | Notes |
|---|---|---|
| `reference` | VARCHAR(20) | Auto-generated `TXN-YYYY-NNNNN`, unique |
| `import_fingerprint` | VARCHAR(64) | SHA-256, nullable, indexed; deduplication key for imports |
| `status` | VARCHAR(20) | DRAFT / CONFIRMED / RECONCILED / CANCELLED |
| `source` | VARCHAR(20) | MANUAL / IMPORT |
| `suggested_lease_id` | BIGINT | FK → lease; set during import, may differ from confirmed lease_id |
| `parser_id` | BIGINT | FK → import_parser; set during import |
| `bank_account_id` | BIGINT | FK → bank_account |
| `subcategory_id` | BIGINT | FK → tag_subcategory |
| `lease_id` | BIGINT | FK → lease (INCOME only) |
| `building_id` | BIGINT | FK → building |
| `housing_unit_id` | BIGINT | FK → housing_unit |
| `import_batch_id` | BIGINT | FK → import_batch |

---

## DTOs

### `ImportPreviewRowDTO`
```
rowNumber, fingerprint, transactionDate, amount, direction, description,
counterpartyAccount, parseError, duplicateInDb,
suggestedSubcategoryId, suggestedSubcategoryName,
suggestedCategoryId, suggestedCategoryName, suggestionConfidence,
suggestedLease: { leaseId, unitId, unitNumber, buildingId, buildingName, personId, personFullName }
```

### `ImportRowEnrichmentDTO`
```
fingerprint, directionOverride, subcategoryId, bankAccountId,
buildingId, housingUnitId, leaseId
```

### `FinancialTransactionSummaryDTO` (list / batch review)
```
id, reference, transactionDate, accountingMonth, direction, amount,
status, source,
bankAccountLabel, categoryName, subcategoryName,
buildingName, unitNumber, leaseReference,
suggestedLeaseId, buildingId, housingUnitId
```

### `ImportBatchResultDTO`
```
batchId, totalRows, importedCount, duplicateCount, errorCount,
errors: [{ rowNumber, rawLine, errorMessage }]
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC014-01 | `amount` is always stored positive; `direction` carries the sign semantics |
| BR-UC014-02 | `lease_id` may only be set when `direction = INCOME` |
| BR-UC014-03 | When `housing_unit_id` is set, `building_id` is automatically derived from the unit's building |
| BR-UC014-04 | RECONCILED transactions are fully immutable: no edit, no delete, no reclassification |
| BR-UC014-05 | Deduplication is fingerprint-based (SHA-256). Rows with a fingerprint already present in `import_fingerprint` are skipped |
| BR-UC014-06 | A subcategory with `direction = INCOME` cannot be applied to an EXPENSE transaction and vice versa; BOTH is always allowed |
| BR-UC014-07 | Tag learning rule confidence increments by 1 on each admin confirmation of a subcategory for a given counterparty_account |
| BR-UC014-08 | Accounting month rule priority: specific (subcategory + counterparty_account) > generic (subcategory only) > default offset 0 |
| BR-UC014-09 | Asset link of type BOILER: the boiler must belong to a unit of the transaction's building |
| BR-UC014-10 | Import requires a registered active parser (UC015); file extension must match parser format |
| BR-UC014-11 | `accounting_month` stored as first day of month (e.g. `2026-02-01`); displayed as month/year only |
| BR-UC014-12 | Enriched rows (matched fingerprint in enrichment payload) saved as CONFIRMED; all other imported rows saved as DRAFT |
| BR-UC014-13 | Lease suggestion during import loads leases of all statuses (DRAFT/ACTIVE/FINISHED/CANCELLED) to support historical imports |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Transaction not found | 404 | `Transaction not found` |
| Edit/delete on RECONCILED | 409 | `Transaction is reconciled and cannot be modified` |
| Direction + lease violation | 400 | `Lease link is only allowed for income transactions` |
| Subcategory direction mismatch | 400 | `Subcategory direction is incompatible with transaction direction` |
| Parser not found or inactive | 400 | `Parser not found or inactive` |
| Parse error | 400 | `File could not be parsed: {detail}` |

---

## Navigation

- Sidebar: **Transactions** top-level item.
- Sub-tabs: **List** · **Import** · **Dashboard** · **Settings** (tags + bank accounts).
- Building/unit details: collapsible "Financial" section with transaction count + link to filtered list.

---

**Last Updated:** 2026-03-10
**Branch:** develop
**Status:** ✅ Implemented
