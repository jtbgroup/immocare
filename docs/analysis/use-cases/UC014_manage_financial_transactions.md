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

### Transaction Linking — Two Independent Mechanisms

A transaction carries two independent sets of links that coexist:

**1. Contextual link** — used for all transaction types:
- `building_id` (optional)
- `housing_unit_id` (optional, cascades from building)
- `lease_id` (optional, INCOME only)

**2. Asset links** (`transaction_asset_link`) — optional, EXPENSE only:
- One or more physical devices (BOILER / FIRE_EXTINGUISHER / METER)
- `housing_unit_id` and `building_id` on each link are **resolved server-side** from the device relationship — not sent by the frontend
- A partial `amount` can be assigned per device to split a single invoice across multiple assets
- When an asset type is selected, the transaction subcategory is **automatically pre-filled** from the platform config mapping (`asset.type.subcategory.mapping.*`) if one exists

### Import Flow (3 steps)
1. **Form** — admin selects a parser (from UC015 registry) and uploads a file.
2. **Preview** — file is parsed server-side; rows returned with duplicate flags, subcategory suggestions, lease suggestions. No persistence. Admin can enrich each row via a side panel.
3. **Import** — admin triggers import with enrichments. Enriched rows saved as CONFIRMED; unenriched rows saved as DRAFT.

### Fingerprint Deduplication
Each parsed row produces a SHA-256 fingerprint from `(transactionDate + amount + counterpartyAccount + description)`. A row is flagged as DUPLICATE if the fingerprint already exists in `financial_transaction.import_fingerprint`.

### Lease Suggestion
During preview and import, `counterparty_account` is matched against known person IBANs (UC016). If a match is found, the most suitable lease is suggested.

---

## Data Model — Tables (V014)

| Table | Purpose |
|---|---|
| `bank_account` | Catalog of own bank accounts (label, IBAN, type, active, owner_user_id) |
| `tag_category` | Top-level classification groups |
| `tag_subcategory` | Fine-grained classification, belongs to a category |
| `financial_transaction` | Core transaction record |
| `transaction_asset_link` | Links a transaction to a physical asset (polymorphic), with optional partial amount |
| `import_batch` | Tracks an import operation (parser, bank account, counts) |
| `tag_learning_rule` | Learns subcategory suggestions from counterparty account, description, or asset type patterns |
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

### Table: `transaction_asset_link`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `transaction_id` | BIGINT | NOT NULL, FK → financial_transaction(id) ON DELETE CASCADE | |
| `asset_type` | VARCHAR(30) | NOT NULL, CHECK IN (BOILER, FIRE_EXTINGUISHER, METER) | |
| `asset_id` | BIGINT | NOT NULL | Polymorphic FK — no DB-level constraint |
| `housing_unit_id` | BIGINT | NULL, FK → housing_unit(id) ON DELETE SET NULL | Resolved server-side from device relationship |
| `building_id` | BIGINT | NULL, FK → building(id) ON DELETE SET NULL | Resolved server-side from device relationship |
| `amount` | DECIMAL(12,2) | NULL, CHECK > 0 | Partial amount for this asset; null = full transaction amount attributed to this asset |
| `notes` | TEXT | NULL | |

**Constraint:** `UNIQUE (transaction_id, asset_type, asset_id)` — same asset cannot appear twice on the same transaction.

**Resolution rules for `housing_unit_id` and `building_id`:**
- BOILER → `boiler.housing_unit_id` → then `housing_unit.building_id`
- FIRE_EXTINGUISHER → `fire_extinguisher.unit_id` (nullable) + `fire_extinguisher.building_id`
- METER → `meter.owner_id` + `meter.owner_type` (HOUSING_UNIT or BUILDING)

### Table: `tag_learning_rule`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `match_field` | VARCHAR(30) | NOT NULL, CHECK IN (COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE) | `COUNTERPARTY_NAME` removed |
| `match_value` | VARCHAR(200) | NOT NULL | For ASSET_TYPE: value is BOILER / FIRE_EXTINGUISHER / METER |
| `subcategory_id` | BIGINT | NOT NULL, FK → tag_subcategory(id) ON DELETE CASCADE | |
| `confidence` | INTEGER | NOT NULL, DEFAULT 1, CHECK >= 1 | |
| `last_matched_at` | TIMESTAMP | NULL | |

**Constraint:** `UNIQUE (match_field, match_value, subcategory_id)`

---

## DTOs

### `TransactionAssetLinkDTO`
```
id
assetType
assetId
assetLabel       -- resolved by service: brand+model for BOILER,
                 -- identificationNumber for FIRE_EXTINGUISHER,
                 -- meterNumber+type for METER
housingUnitId    -- resolved server-side, read-only
unitNumber       -- resolved server-side, read-only
buildingId       -- resolved server-side, read-only
buildingName     -- resolved server-side, read-only
amount           -- nullable
notes
```

### `SaveAssetLinkRequest`
```
assetType*   AssetType   required
assetId*     Long        required
amount       BigDecimal  optional, must be > 0 if provided
notes        String      optional
```

> `housingUnitId` and `buildingId` are **not** part of the request — resolved server-side only.

### `ImportPreviewRowDTO`
```
rowNumber, fingerprint, transactionDate, amount, direction, description,
counterpartyAccount, parseError, duplicateInDb,
suggestedSubcategoryId, suggestedSubcategoryName,
suggestedCategoryId, suggestedCategoryName, suggestionConfidence,
suggestedLease: { leaseId, unitId, unitNumber, buildingId, buildingName, personId, personFullName }
```

> `counterpartyName` removed — not part of the parsed data model.

### `FinancialTransactionSummaryDTO` (list / batch review)
```
id, reference, transactionDate, accountingMonth, direction, amount,
counterpartyAccount, status, source,
bankAccountLabel, categoryName, subcategoryName,
buildingName, unitNumber, leaseReference,
suggestedLeaseId, buildingId, housingUnitId
```

> `counterpartyName` removed.

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
| BR-UC014-03 | When `housing_unit_id` is set on the transaction, `building_id` is automatically derived from the unit's building |
| BR-UC014-04 | RECONCILED transactions are fully immutable: no edit, no delete, no reclassification |
| BR-UC014-05 | Deduplication is fingerprint-based (SHA-256). Rows with a fingerprint already present in `import_fingerprint` are skipped |
| BR-UC014-06 | A subcategory with `direction = INCOME` cannot be applied to an EXPENSE transaction and vice versa; BOTH is always allowed |
| BR-UC014-07 | Tag learning rule confidence increments by 1 on each admin confirmation of a subcategory for a given match_field + match_value |
| BR-UC014-08 | Accounting month rule priority: specific (subcategory + counterparty_account) > generic (subcategory only) > default offset 0 |
| BR-UC014-09 | Asset link of type BOILER: when a building is set on the transaction, the boiler must belong to a unit of that building |
| BR-UC014-10 | Import requires a registered active parser (UC015); file extension must match parser format |
| BR-UC014-11 | `accounting_month` stored as first day of month (e.g. `2026-02-01`); displayed as month/year only |
| BR-UC014-12 | Enriched rows (matched fingerprint in enrichment payload) saved as CONFIRMED; all other imported rows saved as DRAFT |
| BR-UC014-13 | Lease suggestion during import loads leases of all statuses (DRAFT/ACTIVE/FINISHED/CANCELLED) to support historical imports |
| BR-UC014-14 | `housing_unit_id` and `building_id` on `transaction_asset_link` are resolved server-side from the device relationship and are never provided by the client |
| BR-UC014-15 | The sum of partial amounts on asset links must not exceed the transaction total amount |
| BR-UC014-16 | Asset links are optional even for EXPENSE transactions — a transaction may have no asset links |
| BR-UC014-17 | When an asset link is added, if platform config contains a mapping for the asset type (`asset.type.subcategory.mapping.*`), the transaction subcategory is pre-filled with the mapped value. The admin can override freely |
| BR-UC014-18 | `COUNTERPARTY_NAME` is not a valid `match_field` in `tag_learning_rule`. Valid values are: COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE |
| BR-UC014-19 | A `tag_learning_rule` with `match_field = ASSET_TYPE` uses the asset type name as `match_value` (BOILER, FIRE_EXTINGUISHER, METER). Its confidence is reinforced each time an admin accepts a subcategory suggestion triggered by that asset type |

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
| Asset link amount sum exceeds transaction total | 400 | `Sum of asset link amounts (X) exceeds transaction total (Y)` |
| Duplicate asset on same transaction | 409 | `This asset is already linked to this transaction` |
| BOILER not in transaction building | 400 | `The selected boiler does not belong to the transaction's building` |

---

## Navigation

- Sidebar: **Transactions** top-level item.
- Sub-tabs: **List** · **Import** · **Dashboard** · **Settings** (tags + bank accounts).
- Building/unit details: collapsible "Financial" section with transaction count + link to filtered list.

---

**Last Updated:** 2026-04-04
**Branch:** develop
**Status:** ✅ Implemented (pending asset link + learning engine updates)