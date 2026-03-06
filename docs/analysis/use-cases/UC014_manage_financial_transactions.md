# UC014 — Manage Financial Transactions

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC014 |
| **Name** | Manage Financial Transactions |
| **Actor** | ADMIN |
| **Epic** | Financial Management |
| **Status** | 📋 Ready for Implementation |
| **Branch** | develop |

An admin can track all financial inflows (income) and outflows (expenses) related to the management of real estate properties. Transactions can be entered manually or imported from a bank CSV export (generic configurable format). Each transaction is classified via a two-level tag hierarchy (Category → Subcategory), linked to a bank account from a catalog, assigned an accounting month (which may differ from the execution date), and optionally linked to existing domain objects (leases, buildings, housing units, boilers, fire extinguishers, meters). The system learns classification rules and accounting month offsets from admin actions to accelerate future imports.

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
| US084 | Import Transactions from CSV | MUST HAVE | 8 |
| US085 | Review and Confirm Imported Transactions | MUST HAVE | 5 |
| US086 | Manage Tag Catalog (Categories & Subcategories) | MUST HAVE | 3 |
| US087 | Manage Bank Account Catalog | MUST HAVE | 2 |
| US088 | View Financial Summary and Statistics | SHOULD HAVE | 5 |
| US089 | Export Transactions to CSV | SHOULD HAVE | 2 |

---

## Actors

- **ADMIN**: Only role. Full access to all operations.

---

## Domain Concepts

### Transaction Direction
- **INCOME**: money received — rent payments, interest, donor transfers, deposits received, annuity returns, etc.
- **EXPENSE**: money paid out — invoices (energy, water, equipment, services), maintenance, taxes, deposit refunds, annuity payments, etc.

### Two-Level Classification
Each transaction is classified with:
- **Category**: top-level grouping (e.g. Administration, Consommables, Maintenance, Location, Travaux, Rente, Taxes, Prime, Dépôt). Categories are shared across INCOME and EXPENSE.
- **Subcategory**: belongs to a category; this is the granularity applied to each transaction (e.g. Maintenance → Chaudière, Maintenance → Extincteurs). A subcategory carries a direction compatibility flag: INCOME / EXPENSE / BOTH.

### Transaction Date vs Accounting Month
- **transaction_date**: the actual bank execution date.
- **accounting_month**: the month to which the transaction is attributed for reporting (stored as the 1st day of the month, e.g. 2026-02-01). Defaults to the month of `transaction_date`, but can differ — e.g. rent received on Jan 28 is attributed to February. This field drives all statistical breakdowns.

### Transaction Status
- **DRAFT**: imported or pending review.
- **CONFIRMED**: reviewed and validated by admin.
- **RECONCILED**: matched against external bank statement — fully immutable.

### Transaction Source
- **MANUAL**: entered directly by admin.
- **CSV_IMPORT**: imported from a bank CSV file.

### Unique Identification
- **reference**: auto-generated (`TXN-YYYY-NNNNN`), unique and permanent.
- **external_reference**: bank-provided reference, used as deduplication key on import.

### Learning System
Two types of learning rules, both reinforced when an admin confirms a suggestion:
1. **Tag learning rules**: `(match_field, match_value)` → suggested subcategory. Match fields: `COUNTERPARTY_ACCOUNT`, `COUNTERPARTY_NAME`, `DESCRIPTION`.
2. **Accounting month rules**: `(subcategory_id, counterparty_account?)` → month offset integer (0 = same month, +1 = next month). A specific rule (subcategory + counterparty) takes priority over a generic rule (subcategory only), which takes priority over the default offset 0.

---

## Main Flows

### US078 — View Transaction List
1. Admin navigates to Transactions section.
2. List shows all transactions ordered by `transaction_date DESC`, paginated.
3. Filters: direction, execution date range, accounting month range, category, subcategory, bank account, building, status, free-text search.
4. Summary bar shows total income, total expenses, net balance for the current filter.
5. Clicking a row opens transaction detail.

### US079 — Create Transaction Manually
1. Admin clicks "New Transaction".
2. Form: direction, execution date, accounting month (auto-proposed from learning rules, editable), amount, bank account (from catalog), description, counterparty name, counterparty account.
3. Classification: category → subcategory (filtered to category and direction).
4. Links: building → housing unit (cascading, optional); lease (optional, INCOME only).
5. Asset links section (EXPENSE only): type + asset picker + optional notes.
6. Save → reference generated, status CONFIRMED, source MANUAL. Learning rules reinforced for subcategory and accounting month offset.

### US080 — Edit Transaction
1. Admin opens transaction → "Edit".
2. All fields editable except reference, external_reference, source.
3. RECONCILED transactions are fully read-only — "Edit" disabled.

### US081 — Delete Transaction
1. Admin clicks "Delete" → confirmation dialog.
2. RECONCILED transactions cannot be deleted.
3. Deletes all associated asset links.

### US082 — Classify Transaction
1. Admin selects category then subcategory from filtered picker.
2. On save: if `counterparty_account` non-empty → tag learning rule created or reinforced. If `accounting_month` differs from `transaction_date` month → accounting month rule created or reinforced.

### US083 — Link Transaction to Asset(s)
1. On EXPENSE transactions: admin adds asset links (BOILER / FIRE_EXTINGUISHER / METER), picker filtered to transaction's building context.
2. Multiple links allowed. Each has optional notes.
3. Boiler and fire extinguisher detail views show "Related expenses (N)" badge.

### US084 — Import Transactions from CSV
1. Admin uploads CSV file.
2. System parses using configurable column mapping from `platform_config` (generic, not bank-specific).
3. Preview with per-row error highlighting shown before processing.
4. Import runs deduplication (`external_reference` + `transaction_date` + `amount`). Duplicates skipped and reported.
5. Each non-duplicate row: subcategory suggested from tag learning rules; accounting_month proposed from accounting month rules.
6. Non-duplicate rows created as DRAFT, linked to an `import_batch`.

### US085 — Review and Confirm Imported Transactions
1. Admin redirected to batch review view after import.
2. Each row shows suggested category/subcategory (with confidence indicator) and proposed accounting_month.
3. Admin accepts, modifies, or rejects suggestions per row. Confirming reinforces learning rules.
4. "Confirm All" bulk action. Duplicates shown in collapsed audit panel.

### US086 — Manage Tag Catalog
1. Admin manages categories (name, description) and subcategories (name, category, direction compatibility, description).
2. Subcategory cannot be deleted if used on any transaction.
3. System seeded with default taxonomy on first run.

### US087 — Manage Bank Account Catalog
1. Admin manages own bank accounts: label, IBAN, type (CURRENT / SAVINGS), active flag.
2. Inactive accounts hidden from dropdowns but preserved on existing transactions.

### US088 — View Financial Summary and Statistics
1. Dashboard filters: accounting month range (default: current year), building, bank account, direction.
2. Summary cards: Total Income, Total Expenses, Net Balance.
3. Breakdown by subcategory (grouped by category), by building, by housing unit (when building selected), by bank account.
4. Monthly trend: income vs expenses per accounting month over selected period.
5. All aggregations use `accounting_month`, not `transaction_date`.

### US089 — Export Transactions to CSV
1. Active filters applied to export.
2. Columns: reference, external_reference, transaction_date, accounting_month, direction, amount, category, subcategory, bank_account_label, counterparty_name, counterparty_account, building, housing_unit, lease_reference, status, source.
3. UTF-8 BOM encoding for Excel compatibility.
4. Filename: `immocare_transactions_YYYY-MM-DD.csv`.

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC014-01 | `amount` is always stored positive. `direction` carries the sign semantics. |
| BR-UC014-02 | `lease_id` may only be set when `direction = INCOME`. |
| BR-UC014-03 | When `housing_unit_id` is set, `building_id` is automatically derived from the unit's building by the service. |
| BR-UC014-04 | RECONCILED transactions are fully immutable: no edit, no delete, no reclassification. |
| BR-UC014-05 | Deduplication key: `external_reference` + `transaction_date` + `amount`. Rows with null `external_reference` bypass deduplication and are always imported. |
| BR-UC014-06 | A subcategory with `direction = INCOME` cannot be applied to an EXPENSE transaction and vice versa. Subcategories with `direction = BOTH` are always allowed. |
| BR-UC014-07 | Tag learning rule confidence increments by 1 on each admin confirmation of a subcategory for a given `counterparty_account`. |
| BR-UC014-08 | Accounting month rule priority: specific `(subcategory + counterparty_account)` > generic `(subcategory only)` > default offset 0. Confidence increments by 1 on confirmation. |
| BR-UC014-09 | Asset link of type BOILER: the boiler must belong to a unit of the transaction's building. |
| BR-UC014-10 | CSV column mapping is fully configurable via `platform_config` keys `csv.import.*`. No hardcoded bank format. |
| BR-UC014-11 | `accounting_month` stored as first day of month (e.g. 2026-02-01). UI displays as month/year only. |

---

## Data Model — New Tables

| Table | Purpose |
|---|---|
| `bank_account` | Catalog of own bank accounts (label, IBAN, type) |
| `tag_category` | Top-level classification groups |
| `tag_subcategory` | Fine-grained classification, belongs to a category |
| `financial_transaction` | Core transaction record |
| `transaction_asset_link` | Links a transaction to a physical asset (polymorphic) |
| `import_batch` | Tracks a CSV import operation |
| `tag_learning_rule` | Learns subcategory suggestions from counterparty/description patterns |
| `accounting_month_rule` | Learns accounting month offsets from subcategory + counterparty patterns |

### Key Foreign Keys on `financial_transaction`

| Column | References | Direction constraint |
|---|---|---|
| `bank_account_id` | `bank_account` | both |
| `subcategory_id` | `tag_subcategory` | direction compatibility enforced |
| `lease_id` | `lease` | INCOME only |
| `building_id` | `building` | both |
| `housing_unit_id` | `housing_unit` | both (building auto-derived) |
| `import_batch_id` | `import_batch` | CSV_IMPORT only |

---

## Navigation

- Main sidebar: **Transactions** top-level item.
- Sub-tabs on Transactions page: **List** · **Import** · **Dashboard** · **Settings** (tags + bank accounts).
- Building details: collapsible "Financial" section with transaction count badge + link to filtered list.
- Housing unit details: same pattern.

---

**Last Updated**: 2026-03-05
**Branch**: develop
**Status**: 📋 Ready for Implementation
