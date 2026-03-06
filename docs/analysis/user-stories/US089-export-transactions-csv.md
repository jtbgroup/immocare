# User Story US089: Export Transactions to CSV

| Attribute | Value |
|-----------|-------|
| **Story ID** | US089 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 2 |

**As an** ADMIN **I want to** export the filtered transaction list to CSV **so that** I can use the data in external tools such as Excel or accounting software.

## Acceptance Criteria

**AC1:** "Export CSV" button available in two places: the transaction list header (US078) and the dashboard header (US088).

**AC2:** The export applies the same filters currently active in the list or dashboard (direction, date ranges, category, subcategory, building, status, search, etc.). No separate filter configuration needed.

**AC3:** CSV columns in order:
`reference`, `external_reference`, `transaction_date`, `accounting_month`, `direction`, `amount`, `category`, `subcategory`, `bank_account_label`, `bank_account_iban`, `counterparty_name`, `counterparty_account`, `description`, `building`, `housing_unit`, `lease_reference`, `status`, `source`

**AC4:** `accounting_month` exported as `YYYY-MM` (e.g. `2026-02`).

**AC5:** `transaction_date` exported as `YYYY-MM-DD`.

**AC6:** `amount` exported as a plain decimal with 2 decimal places, dot separator (e.g. `1234.50`). No currency symbol, no thousands separator. Sign follows direction: positive for INCOME, negative for EXPENSE.

**AC7:** File encoded as UTF-8 with BOM (`\xEF\xBB\xBF`) for correct display in Excel on Windows.

**AC8:** Filename: `immocare_transactions_YYYY-MM-DD.csv` where the date is today's date.

**AC9:** Empty result (no transactions match filters): file downloaded with header row only. No error shown.

**AC10:** Export is streamed server-side to avoid loading all rows into memory. No pagination — all matching rows exported regardless of page size setting.

**Endpoint:** `GET /api/v1/transactions/export` — same query parameters as `GET /api/v1/transactions`. Response Content-Type: `text/csv; charset=UTF-8`. Content-Disposition: `attachment; filename="immocare_transactions_YYYY-MM-DD.csv"`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
