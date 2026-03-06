# User Story US088: View Financial Summary and Statistics

| Attribute | Value |
|-----------|-------|
| **Story ID** | US088 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | SHOULD HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** view financial summaries and statistics broken down by category, building, unit, and period **so that** I can analyze the financial performance of my property portfolio.

## Acceptance Criteria

**AC1:** "Dashboard" tab on the Transactions page.

**AC2:** Filter bar at the top:
- Accounting month range: from / to month pickers (default: January of current year to current month).
- Building: ALL or specific (dropdown).
- Housing unit: depends on building selection, cleared when building changes.
- Bank account: ALL or specific.
- Direction: ALL / INCOME / EXPENSE.
All statistics and charts react immediately to filter changes.

**AC3:** Summary cards row: "Total Income" (green background), "Total Expenses" (red background), "Net Balance" (green if ≥ 0, red if < 0). All figures computed over CONFIRMED and RECONCILED transactions only (DRAFT and CANCELLED excluded).

**AC4:** "By Category / Subcategory" section:
- Table grouped by category (bold header row per category with subtotal) and subcategory rows within each group.
- Columns: name, total amount, number of transactions, percentage of total in that direction.
- Sorted by category name ASC, subcategory name ASC within category.
- Horizontal bar chart alongside the table, one bar per subcategory, colored by category.
- When direction filter = ALL: income subcategories and expense subcategories shown in separate groups.

**AC5:** "By Building" section:
- Table: building name, total income, total expenses, net balance.
- Transactions with no building assigned shown in a "Unassigned" row.
- Sorted by building name ASC.

**AC6:** "By Housing Unit" section:
- Visible only when a specific building is selected in filters.
- Table: unit number, total income, total expenses, net balance.
- Transactions linked to building but no unit shown in "Building-level" row.
- Sorted by unit number ASC.

**AC7:** "By Bank Account" section:
- Table: account label, account type badge, total income, total expenses, net balance.
- Transactions with no bank account shown in "Unassigned" row.

**AC8:** "Monthly Trend" section:
- Line chart with two lines: Income and Expenses, one data point per accounting month over the selected period.
- X-axis: months. Y-axis: amount in €.
- Tooltip on hover: exact income, expense, and balance for that month.
- When a single month is selected: chart not shown (no trend to display), replaced by message "Select a wider date range to see the monthly trend."

**AC9:** No data for selected filters → "No confirmed transactions for the selected period." shown in each section. Summary cards show €0.00.

**AC10:** "Export CSV" button in dashboard header → exports the filtered transaction list (US089) using the same filters currently applied to the dashboard.

**Endpoint:** `GET /api/v1/transactions/statistics?accountingFrom=&accountingTo=&buildingId=&unitId=&bankAccountId=&direction=` — HTTP 200, returns `TransactionStatisticsDTO`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
