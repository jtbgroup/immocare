---
title: Manual Transaction Management
type: technical reference
status: current
last_verified: 2026-11-08
verified_against: "TransactionController, FinancialTransactionService, TransactionStatus, TransactionFormComponent, TransactionListComponent, TransactionDetailComponent, TransactionAssetLinkService, V015__uc015_manage_financial_transactions.sql, BR-TX-012 stakeholder confirmation 2026-11-08"
---

# Manual Transaction Management

This reference describes the current implementation for manually recording,
consulting, editing, and deleting financial transactions. It does not specify the
bank-import workflow, import review, or how automated classification suggestions
should work.

## Access and estate scope

Transaction reads are available to estate members. Creating, editing, and deleting
transactions require the user to be a manager of the selected estate. The API
routes are estate-scoped:

```text
GET    /api/v1/estates/{estateId}/transactions
GET    /api/v1/estates/{estateId}/transactions/{id}
POST   /api/v1/estates/{estateId}/transactions
PUT    /api/v1/estates/{estateId}/transactions/{id}
DELETE /api/v1/estates/{estateId}/transactions/{id}
```

The service also verifies estate ownership when loading a transaction or
subcategory. See [Estate Management](../estates-and-access/estate-management.md)
for the broader membership and access model.

## Entering a transaction

The form captures:

- Direction (`INCOME` or `EXPENSE`), transaction date, accounting month, and a
  strictly positive amount.
- Optional value date, description, external reference, counterparty account, and
  bank account.
- An optional subcategory. Its category is derived from the selected subcategory;
  the form filters choices to those compatible with the selected direction.
- Optional building, housing unit, and lease links. Housing units are loaded after
  a building is selected. Selecting a lease can fill an empty counterparty account
  from the primary tenant's primary bank account, or their first bank account.
- Optional equipment links for expenses in the form. A linked item can be a boiler,
  fire extinguisher, or meter; the server resolves its housing-unit/building
  context from the equipment record.

The form initially proposes the current month as the accounting month. The user can
change it, and the server normalizes it to the first day of that month. It does not
re-propose the month when the subcategory or counterparty account changes.

On successful creation, the server sets the source to `MANUAL`, the status to
`CONFIRMED`, and generates a `TXN-YYYY-NNNNN` reference. The sequence is generated
for the current year across transactions, without an estate filter. A housing-unit
link also sets the transaction's building from that unit. An expense cannot be
linked to a lease, and a selected subcategory must be compatible with the
transaction direction; see
[Financial Transaction Rules](../../business/financial-transactions/rules/financial-transactions.md).

The confirmed product behavior is to let the user choose `DRAFT` or `CONFIRMED`
when creating a manual transaction, with `CONFIRMED` selected by default.
`RECONCILED` is not a creation choice. After creation, status changes are handled
through a dedicated action, not by editing the transaction ([BR-TX-012](../../business/financial-transactions/rules/financial-transactions.md)).
This behavior is not yet implemented: the current create request does not accept a
status and the service always creates manual transactions as `CONFIRMED`. The
implementation work is tracked in the [backlog](../../../backlog/backlog.md).

## Consulting and finding transactions

The list defaults to 20 results per page, sorted by transaction date descending.
The API supports filters for direction, transaction-date and accounting-month
ranges, category, subcategory, bank account, building, housing unit, status, text
search, import batch, and equipment link. The current list UI exposes direction,
date ranges, accounting-month ranges, category/subcategory, bank account, status,
and text search.

Text search matches reference, description, and counterparty account. The current
API also calculates income, expense, and net totals over all matching transactions,
in addition to the requested page. This is not the intended list behavior: for
performance, the list should return the paginated rows only; a broader aggregated
view belongs in the dashboard. This implementation gap is tracked in the
[backlog](../../../backlog/backlog.md) and the target behavior is recorded in
[BR-TX-010](../../business/financial-transactions/rules/financial-transactions.md).
Selecting a transaction opens its detail view, which shows its reference, dates,
amount, status, source, classification, account and property links, description,
and any linked equipment.

## Editing and deleting

The edit form is pre-populated from the transaction. The current form displays
the generated reference but has no reference or source input; the update request
also has no `reference` or `source` field. The `externalReference` field is
editable. This describes the current implementation, not an agreed product
constraint; intended editability remains an [open question](../../../backlog/open_questions.md).
Updating refreshes `updatedAt`; the accounting month is again normalized to its
first day.

The current UI hides edit and delete actions for a transaction marked `RECONCILED`, and the
service rejects updates and deletions for that status. Other statuses are not
blocked by the service's edit/delete check. The implementation does not explain
the business rationale for this restriction; its meaning and intended policy remain
open under [BR-TX-005](../../business/financial-transactions/rules/financial-transactions.md).
The Java `TransactionStatus` enum defines `DRAFT`, `CONFIRMED`, and `RECONCILED`.
The V015 database constraint also permits `CANCELLED`, which is absent from that
enum; this discrepancy is listed in the [open questions](../../../backlog/open_questions.md).
Deleting a transaction removes its equipment links through the entity
relationship's cascading orphan removal.

## Allocation across costs

A single expense transaction may cover multiple costs, and splitting its amount is
not limited to equipment links ([BR-TX-011](../../business/financial-transactions/rules/financial-transactions.md)).
The current data model only represents partial allocations to tracked equipment
through `transaction_asset_link`; it has no generic allocation record for other
costs. Supporting the broader allocation is tracked in the [backlog](../../../backlog/backlog.md).

## Scope notes and observed differences

- The current form permits saving without a subcategory, despite the historical
  create-story criteria calling category and subcategory required.
- The lease selector is available when a housing unit is selected; the form does
  not restrict the returned leases to `ACTIVE` status. The server rejects a lease
  on an expense.
- The form hides equipment links for income, but the create/update service does
  not independently reject equipment links on income transactions.
- Equipment links are the only allocation structure currently persisted; they
  associate amounts with boilers, fire extinguishers, or meters, not arbitrary
  costs such as invoice line items.
- Saving a transaction with a subcategory calls the learning service using the
  counterparty account and linked equipment types. This side effect is noted here
  because it occurs during manual saves; the automated suggestion and learning
  behavior is outside this reference's scope.
- An equipment-to-subcategory mapping can prefill the subcategory in the form.
  The mapping and suggestion behavior are not specified here.
- Historical list criteria call for 25 rows per page; the current API and UI default
  to 20. The legacy stories are historical analysis, not the authority for current
  application behavior.

## Implementation sources

- [TransactionController.java](../../../backend/src/main/java/com/immocare/controller/TransactionController.java)
- [FinancialTransactionService.java](../../../backend/src/main/java/com/immocare/service/FinancialTransactionService.java)
- [FinancialTransactionRepository.java](../../../backend/src/main/java/com/immocare/repository/FinancialTransactionRepository.java)
- [TransactionAssetLinkService.java](../../../backend/src/main/java/com/immocare/service/TransactionAssetLinkService.java)
- [TransactionFormComponent](../../../frontend/src/app/features/transaction/transaction-form/transaction-form.component.ts)
- [TransactionListComponent](../../../frontend/src/app/features/transaction/transaction-list/transaction-list.component.ts)
- [TransactionDetailComponent](../../../frontend/src/app/features/transaction/transaction-detail/transaction-detail.component.ts)
- [V015 transaction schema](../../../backend/src/main/resources/db/migration/V015__uc015_manage_financial_transactions.sql)
