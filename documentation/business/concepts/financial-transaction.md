# Concept — Financial Transaction

A financial transaction represents a movement of money tracked as part of property management. It helps distinguish income from expenses, classify them, and associate them with the relevant property or activity.

## Direction and amount

The model distinguishes two directions: **income** (`INCOME`) and **expense** (`EXPENSE`). The amount is stored as a positive value; direction indicates whether money is received or paid out. Balance calculations subtract expenses from income.

## Dates

- The **transaction date** represents the date of the movement.
- The **value date** is separate and optional.
- The **accounting month** is the month used for grouping and reporting. It is recorded at month granularity and may differ from the transaction date.

The current import behavior defaults to the month of the transaction date; for manual entry, the accounting month is provided separately. The accounting policy to apply needs confirmation.

## Classification

Classification follows a **category → subcategory** hierarchy. A subcategory is defined as applicable to income, expenses, or both. The system checks that its direction is compatible with the transaction.

## Context and links

A transaction belongs to an estate. It may be associated with a bank account, building, housing unit, or lease. The observed management service only allows a lease to be associated with income.

Separate links can associate a transaction with one or more tracked pieces of equipment: a boiler, fire extinguisher, or meter. A partial amount may be assigned to an item of equipment. The code accepts a sum of partial amounts that does not exceed the transaction total.

## Origin, status, and import

- A transaction originates either from manual entry or from an import.
- A manually entered transaction is created with status `CONFIRMED`.
- An imported transaction with enrichment is created as `CONFIRMED`; an imported row without enrichment is created as `DRAFT`.
- Transactions with status `RECONCILED` cannot be modified or deleted by the observed service.

These are application states observed in the code. In particular, the observed code does not define the business meaning of reconciliation or the process by which it is achieved.

An import is tracked as a batch, and its rows may be identified as duplicates by fingerprint within the same estate. Classification and lease suggestions are processing aids; they should not be treated as business decisions until reviewed.

## Conceptual relationships

```text
Transaction → Estate
Transaction → Direction + Date + Accounting month + Amount
Transaction → Category → Subcategory
Transaction → Bank account (optional)
Transaction → Building / housing unit / lease (optional)
Transaction → Equipment item(s) (optional)
Import → Batch → Imported transaction(s)
```

Currently observed rules are documented in the [transaction rules](../rules/financial-transactions.md).
