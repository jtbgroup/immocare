# User Story US078: View Transaction List

| Attribute | Value |
|-----------|-------|
| **Story ID** | US078 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 3 |

**As an** ADMIN **I want to** view the list of all financial transactions with filtering capabilities **so that** I have a complete and navigable overview of all cash flows.

## Acceptance Criteria

**AC1:** Transactions listed ordered by `transaction_date DESC`, 25 rows per page with pagination controls.

**AC2:** Each row displays: reference, execution date, accounting month (MM/YYYY), direction badge (green INCOME / red EXPENSE), amount formatted with currency symbol, counterparty name, category + subcategory chips, bank account label, status badge (DRAFT / CONFIRMED / RECONCILED).

**AC3:** Filters panel contains:
- Direction toggle: ALL / INCOME / EXPENSE
- Execution date range: from / to date pickers
- Accounting month range: from / to month pickers
- Category: single select dropdown
- Subcategory: single select dropdown, filtered to selected category
- Bank account: single select from catalog
- Building: single select
- Status: multi-select (DRAFT / CONFIRMED / RECONCILED)
- Free-text search: matches reference, description, counterparty name, counterparty account

**AC4:** Active filters displayed as removable chips above the list. "Clear all filters" button resets all filters.

**AC5:** Summary bar above the list shows — for the current filter (all pages): Total Income (green), Total Expenses (red), Net Balance (green if positive, red if negative).

**AC6:** Clicking any row navigates to the transaction detail view `/transactions/{id}`.

**AC7:** No transactions match current filters → "No transactions found." message with a "Create Transaction" button.

**AC8:** "New Transaction" button always visible in the page header, navigates to `/transactions/new`.

**AC9:** "Export CSV" button applies current filters and triggers download (US089).

**Endpoints:**
- `GET /api/v1/transactions` — query params: `direction`, `from`, `to`, `accountingFrom`, `accountingTo`, `categoryId`, `subcategoryId`, `bankAccountId`, `buildingId`, `status`, `search`, `page`, `size`, `sort`

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
