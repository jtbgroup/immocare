# User Story US085: Review and Confirm Imported Transactions

| Attribute | Value |
|-----------|-------|
| **Story ID** | US085 |
| **Epic** | Financial Management |
| **Related UC** | UC014 |
| **Priority** | MUST HAVE |
| **Story Points** | 5 |

**As an** ADMIN **I want to** review imported transactions and validate or correct automatic suggestions **so that** the classification and accounting month are accurate before the data enters reporting.

## Acceptance Criteria

**AC1:** After import (US084), admin is automatically redirected to the batch review page at `/transactions/import/{batchId}`.

**AC2:** Page title: "Review import — [filename] — [imported_at date]". Progress indicator: "X / Y confirmed" updated in real time.

**AC3:** Each DRAFT transaction row displays:
- Execution date, accounting month (editable month picker inline), direction badge, amount, counterparty name, counterparty account.
- Category + subcategory fields: pre-filled with suggestion if confidence ≥ threshold, shown with a ✨ icon indicating it is a suggestion. Editable dropdowns inline.
- Confidence level shown as a small label next to the suggestion (e.g. "confidence: 7").
- Bank account label (pre-assigned or empty dropdown).
- Building / unit / lease fields (optional, inline dropdowns).
- Row status: DRAFT (grey) / CONFIRMED (green) / REJECTED (strikethrough, red).

**AC4:** "Confirm" button per row → row status changes to CONFIRMED. Learning rules reinforced:
- If subcategory suggestion was shown and accepted (not changed by admin): `tag_learning_rule.confidence += 1`.
- If `accounting_month` proposal was accepted (not changed): `accounting_month_rule.confidence += 1`.
- If admin manually changed subcategory or accounting_month: new or updated rule created with the corrected value.

**AC5:** "Reject" button per row → status set to CANCELLED (soft delete equivalent). Row greyed out and struck through. Rejected rows not included in statistics.

**AC6:** "Confirm All" button → confirmation dialog: "Confirm [N] remaining DRAFT transactions? Suggestions will be applied as-is." Confirm → all DRAFT rows in batch set to CONFIRMED in one operation. Learning rules reinforced for each.

**AC7:** Collapsed "Skipped duplicates ([N])" panel at bottom of page for audit. Shows: date, amount, direction, counterparty, and the reference of the existing matching transaction.

**AC8:** Batch review page accessible at any time via `GET /api/v1/transactions/import/{batchId}` — even after partial confirmation. Already CONFIRMED or CANCELLED rows shown with their final status (non-editable).

**AC9:** All transactions in batch are CONFIRMED or CANCELLED → "All transactions reviewed." banner with link back to transaction list.

**Endpoints:**
- `GET /api/v1/transactions/import/{batchId}` — HTTP 200, paged list of transactions in batch.
- `PATCH /api/v1/transactions/{id}/confirm` — HTTP 200, returns updated transaction.
- `POST /api/v1/transactions/confirm-batch` body `{ batchId }` — HTTP 200, returns `{ confirmedCount: int }`.

**Last Updated:** 2026-03-05 | **Status:** Ready for Development
