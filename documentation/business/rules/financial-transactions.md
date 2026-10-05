# Candidate Business Rules — Financial Transactions

The rules below were extracted from current behavior, primarily transaction validation and processing. They **need business validation** before being treated as organizational policies.

| ID | Observed rule | Status |
|---|---|---|
| BR-TX-001 | A transaction has direction `INCOME` or `EXPENSE` and a strictly positive amount. Direction, not the sign of the amount, indicates whether it is an inflow or outflow. | Needs business validation |
| BR-TX-002 | A transaction may be assigned to an accounting month different from its transaction date. When saved, the day is normalized to the first day of that month. | Needs business validation |
| BR-TX-003 | A lease may only be linked to an income transaction. | Needs business validation |
| BR-TX-004 | A subcategory's direction must be compatible with the transaction direction; a subcategory defined for both directions is compatible with either. | Needs business validation |
| BR-TX-005 | A `RECONCILED` transaction cannot be modified or deleted through the observed operations. | Needs clarification |
| BR-TX-006 | Partial amounts assigned to equipment links must not total more than the transaction amount. | Needs business validation |
| BR-TX-007 | A manually entered transaction is created as `CONFIRMED`. On import, a row with enrichment is created as `CONFIRMED`; otherwise it is created as `DRAFT`. | Needs clarification |
| BR-TX-008 | A transaction can only be accessed or processed within the portfolio to which it belongs. | Needs business validation |
| BR-TX-009 | During import, a fingerprint already present in the same portfolio is treated as a duplicate and the row is skipped; the same movement in another portfolio does not block the import. | Needs business validation |

## Observed sources

`CreateTransactionRequest`, `FinancialTransactionService`, `TransactionImportService`, `FinancialTransaction`, `TransactionAssetLink`, `TransactionDirection`, `TransactionStatus`, and `SubcategoryDirection`.
