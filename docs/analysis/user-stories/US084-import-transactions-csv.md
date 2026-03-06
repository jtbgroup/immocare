# User Story US084: Import Transactions from CSV

| Attribute | Value |
|-----------|-------|
| **Story ID** | US084 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 8 |

**As an** ADMIN **I want to** import a bank CSV export **so that** I can bulk-load transactions without manual data entry and benefit from automatic classification suggestions.

## Acceptance Criteria

**AC1:** "Import" tab on the Transactions page shows a file upload zone (drag-and-drop or browse button). Only `.csv` files accepted; max size 5 MB. Wrong file type → error "Please upload a CSV file." Oversized file → error "File exceeds the 5 MB limit."

**AC2:** After file selection, system immediately parses the file using the column mapping from `platform_config` (keys `csv.import.*`): delimiter, date format, skip header rows, and column indices for date, amount, description, counterparty name, counterparty account, external reference, bank account, value date. The mapping is generic — not tied to any specific bank.

**AC3:** A preview table appears showing all parsed rows with columns: row number, date, amount, inferred direction (negative amount = EXPENSE, positive = INCOME), description, counterparty name, counterparty account. Rows with parse errors (invalid date, non-numeric amount, missing required column) shown with a red warning icon and error message inline.

**AC4:** Admin can override the inferred direction per row in the preview (direction toggle per row). Admin can also adjust the bank account assignment per row (dropdown from catalog).

**AC5:** "Process Import" button triggers the actual import. Button disabled if all rows have errors.

**AC6:** Import runs deduplication: a row is flagged as DUPLICATE if a transaction with the same `external_reference` AND `transaction_date` AND `amount` already exists in the database. Duplicate rows are not created. Rows with null or empty `external_reference` bypass deduplication (BR-UC014-05).

**AC7:** For each non-duplicate, non-error row:
- System queries `tag_learning_rule` for matches on `COUNTERPARTY_ACCOUNT` (exact, case-insensitive), then `COUNTERPARTY_NAME` (contains, case-insensitive), then `DESCRIPTION` (contains, case-insensitive). Rules with confidence ≥ threshold (from `platform_config` key `csv.import.suggestion.confidence.threshold`, default 3) are used. Best match (highest confidence) → suggested subcategory (and its parent category).
- System queries `accounting_month_rule` for matching `(subcategory_id, counterparty_account)` (specific rule first), then `(subcategory_id, NULL)` (generic rule). Applies offset to `transaction_date` to compute proposed `accounting_month`. Default offset = 0 if no rule found.
- If `counterparty_account` in CSV matches the IBAN of a known active bank account in the catalog: `bank_account_id` auto-set.

**AC8:** `import_batch` record created with: filename, imported_at, total_rows, imported_count, duplicate_count, error_count, created_by.

**AC9:** All non-duplicate valid rows created with status DRAFT, source CSV_IMPORT, `import_batch_id` set.

**AC10:** Import result banner shown: "Imported: X | Duplicates skipped: Y | Errors: Z". Below the banner, a "Review imported transactions" button navigates to the batch review view (US085).

**AC11:** If all rows are duplicates or errors → no `import_batch` created. Message: "No new transactions to import."

**Endpoint:** `POST /api/v1/transactions/import` (multipart/form-data, field `file`) — HTTP 200, returns `ImportBatchResultDTO`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
