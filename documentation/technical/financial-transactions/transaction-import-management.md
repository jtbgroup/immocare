---
title: Transaction Import Management
type: technical reference
status: current
last_verified: 2026-11-08
verified_against: "TransactionImportController, ImportParserController, TransactionImportService, TransactionParserRegistry, LearningService, TagLearningRuleRepository, PersonBankAccountRepository, TransactionImportComponent, ImportRowDetailPanelComponent, TransactionReviewComponent, TransactionAssetLinkService, BR-TX-013 to BR-TX-021"
---

# Transaction Import Management

This reference describes the current bank-transaction import implementation and
its known gaps. The confirmed import and allocation requirements are defined in
[Financial Transaction Rules](../../business/financial-transactions/rules/financial-transactions.md);
future work is tracked in the [backlog](../../../backlog/backlog.md).

## Parser and bank-account selection

The application lists active parser definitions from the platform-wide parser
catalogue. The selected parser code is resolved to a registered parser
implementation when previewing or importing a file.

The import form lists bank accounts for the active estate. A bank account can
currently be left unselected; when one is selected, imported transactions are
associated with it ([BR-TX-020](../../business/financial-transactions/rules/financial-transactions.md)).
Whether account selection must be required remains an
[open question](../../../backlog/open_questions.md).

## Preview, suggestions, and row selection

Preview parses the uploaded file and returns rows without persisting transactions.
Valid, nonduplicate rows are selected by default; users can change the selection
and edit row enrichment before importing. The frontend submits only selected row
fingerprints. The import operation checks for duplicates again before saving.

The preview can suggest a subcategory from learned matches on the counterparty
account or description. An estate configuration supplies the minimum confidence
threshold, but learning-rule lookups are currently global rather than
estate-scoped. The confirmed estate boundary and the required correction are
tracked under [BR-TX-019](../../business/financial-transactions/rules/financial-transactions.md)
and in the backlog.

For rent receipts, the current implementation looks up the counterparty IBAN
case-insensitively, then checks whether the returned person and lease belong to
the active estate. The lookup itself is not estate-filtered; if the same IBAN
exists in multiple estates, a single matching account may be ambiguous. When a
person is accepted, the service selects that person's latest-starting lease. It
does not filter candidates by transaction date or lease status and does not show
the confirmed prominent warning for ambiguous associations. The lease-selection
policy remains an
[open question](../../../backlog/open_questions.md).
The import service can set the matched housing unit, but it does not currently
populate the associated building in that path; automatic association of both is
required by [BR-TX-021](../../business/financial-transactions/rules/financial-transactions.md).

The preview editor can override direction and edit classification, lease,
building, housing unit, and equipment links. The current import enrichment
supports equipment allocations, but not allocation-specific categories or
allocations directly to housing units. The confirmed split requirements are
defined in [BR-TX-017 and BR-TX-018](../../business/financial-transactions/rules/financial-transactions.md).

## Duplicate handling and persistence

Duplicate detection uses the transaction fingerprint within the active estate.
The fingerprint consists of transaction date, amount, counterparty account, and
normalized description; it does not include the selected bank account. A matching
fingerprint is skipped during import, so the same fingerprint cannot be imported
again into that estate. The detailed fingerprint behavior is recorded in
[BR-TX-009](../../business/financial-transactions/rules/financial-transactions.md).

The fingerprint's identity fields and the possibility of matching otherwise
similar transactions from separate bank accounts remain under review in the
[backlog](../../../backlog/backlog.md). The API currently treats an omitted or
empty selected-fingerprint set as selecting all parsed rows; the confirmed
selection requirement is therefore not enforced for every API request
([BR-TX-013](../../business/financial-transactions/rules/financial-transactions.md)).

Imported transactions are stored in an import batch. A row with enrichment data
is created as `CONFIRMED`; a row without it is created as `DRAFT`. The confirmed
no-reimport requirement is [BR-TX-014](../../business/financial-transactions/rules/financial-transactions.md).

## Learning scope

Importing a transaction with a subcategory can update a classification-learning
rule. The current learning-rule schema and lookup do not include an estate scope,
so a learned classification may be reused across estates. This differs from the
confirmed estate-scoped requirement ([BR-TX-019](../../business/financial-transactions/rules/financial-transactions.md))
and must be corrected before the learning feature is considered estate-isolated.

## Allocation limits

The current data model stores partial allocations to tracked equipment. The
service rejects equipment-link amounts whose sum exceeds the transaction amount,
but the import and transaction views do not provide the confirmed warning when
the allocation total is lower. Direct allocation to housing units and a separate
category and subcategory for each allocation are not implemented. See
[BR-TX-017 and BR-TX-018](../../business/financial-transactions/rules/financial-transactions.md)
and the [allocation backlog item](../../../backlog/backlog.md).

## Implementation sources

- [ImportParserController.java](../../../backend/src/main/java/com/immocare/controller/ImportParserController.java)
- [TransactionImportController.java](../../../backend/src/main/java/com/immocare/controller/TransactionImportController.java)
- [ImportParser.java](../../../backend/src/main/java/com/immocare/model/entity/ImportParser.java)
- [TransactionParserRegistry.java](../../../backend/src/main/java/com/immocare/repository/TransactionParserRegistry.java)
- [TransactionImportService.java](../../../backend/src/main/java/com/immocare/service/TransactionImportService.java)
- [LearningService.java](../../../backend/src/main/java/com/immocare/service/LearningService.java)
- [TagLearningRuleRepository.java](../../../backend/src/main/java/com/immocare/repository/TagLearningRuleRepository.java)
- [TransactionAssetLinkService.java](../../../backend/src/main/java/com/immocare/service/TransactionAssetLinkService.java)
- [PersonBankAccountRepository.java](../../../backend/src/main/java/com/immocare/repository/PersonBankAccountRepository.java)
- [TransactionImportComponent](../../../frontend/src/app/features/transaction/transaction-import/transaction-import.component.ts)
- [ImportRowDetailPanelComponent](../../../frontend/src/app/features/transaction/components/import-row-detail-panel/import-row-detail-panel.component.ts)
- [TransactionReviewComponent](../../../frontend/src/app/features/transaction/transaction-review/transaction-review.component.ts)
