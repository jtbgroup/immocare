# UC015 — Import Parser Strategies

## Overview

| Attribute | Value |
|---|---|
| **UC ID** | UC015 |
| **Name** | Import Parser Strategies |
| **Actor** | ADMIN |
| **Epic** | Financial Management |
| **Flyway** | V015 |
| **Status** | ✅ Implemented |
| **Branch** | develop |

The import parser registry allows the system to support multiple bank statement formats (CSV or PDF) without hardcoding any file structure. Each parser is a named strategy registered in the database and implemented as a Spring `@Component`. During import (UC014 US084), the admin selects the appropriate parser for their bank export. The parser is responsible for transforming raw bytes into a list of `ParsedTransaction` objects; deduplication, enrichment, and persistence are handled by `TransactionImportService`.

---

## User Stories

| Story | Title | Priority | Points |
|---|---|---|---|
| US090 | Select Parser on Import | MUST HAVE | 1 |
| US091 | View Parser Registry | SHOULD HAVE | 2 |

---

## US090 — Select Parser on Import

**As an** ADMIN **I want to** select the appropriate parser before uploading a bank export **so that** the file is interpreted correctly regardless of the bank or format.

**Acceptance Criteria:**
- AC1: Import form (UC014 US084) shows a "Parser" dropdown listing all active parsers, ordered by label. Each entry shows: label, format badge (CSV / PDF), bank hint.
- AC2: File upload zone accepts only the extension matching the selected parser format (`.csv` or `.pdf`). Mismatched file → error "This parser expects a {FORMAT} file."
- AC3: Selected `parserCode` is sent as a multipart field alongside the file in both preview and import requests.
- AC4: Unknown or inactive `parserCode` → HTTP 400 "Parser not found or inactive."

**Endpoint:** `GET /api/v1/import-parsers` — returns list of active parsers.

---

## US091 — View Parser Registry

**As an** ADMIN **I want to** view the list of available parsers **so that** I know which bank formats are supported.

**Acceptance Criteria:**
- AC1: Settings page (or dedicated section) lists all parsers: code, label, format, bank hint, active status, description.
- AC2: Inactive parsers shown with a greyed-out badge; not available in the import dropdown.
- AC3: No create/edit/delete in UI — parsers are managed by developers and deployed via Flyway seed data.

**Endpoint:** `GET /api/v1/import-parsers?activeOnly=false` — returns all parsers including inactive.

---

## Architecture — Strategy Pattern

```
TransactionParser (interface)
├── getCode(): String          — matches import_parser.code in DB
├── parse(InputStream): List<ParsedTransaction>
└── getDescription(): String

Implementations (Spring @Component, auto-registered via TransactionParserRegistry):
├── KeytradeCsvParser          — code: keytrade-csv-20260102
└── KeytradePdfParser          — code: keytrade-pdf-20260301
```

`TransactionParserRegistry` is a Spring `@Component` that collects all `TransactionParser` beans and exposes `getParser(code)` for lookup by code.

### ParsedTransaction fields
```
fingerprint        String    SHA-256 of (transactionDate + amount + counterpartyAccount + description)
transactionDate    LocalDate
valueDate          LocalDate  nullable
amount             BigDecimal always positive
direction          TransactionDirection  INCOME / EXPENSE / null (unknown — determined during enrichment)
description        String
counterpartyAccount String
rowNumber          int
parseError         String    null if row parsed successfully
```

---

## Fingerprint Deduplication

The fingerprint is computed by each parser on every parsed row. It is a SHA-256 hex digest of the concatenation: `transactionDate|amount|counterpartyAccount|description` (pipe-separated, null fields replaced by empty string).

During import, `TransactionImportService` calls `financialTransactionRepository.findIdByImportFingerprint(fingerprint)`. If a result is returned, the row is flagged as DUPLICATE and skipped. The fingerprint is stored on `financial_transaction.import_fingerprint` after successful insert to prevent future re-import of the same row.

---

## Implemented Parsers

### `keytrade-csv-20260102` — Keytrade CSV (format Jan 2026)

| Field | Details |
|---|---|
| Format | CSV |
| Delimiter | `;` |
| Encoding | UTF-8 |
| Columns | Date ; Description ; De (counterparty name) ; IBAN (counterparty account) ; Montant |
| Amount | Positive value with `EUR` suffix; direction determined by description prefix (`vers:` = EXPENSE, `de:` = INCOME) |
| Date format | `dd/MM/yyyy` |
| Header rows | 1 |

### `keytrade-pdf-20260301` — Keytrade PDF (format Mar 2026)

| Field | Details |
|---|---|
| Format | PDF |
| Structure | Multi-line blocks per transaction |
| Amount | Prefixed with `+` (INCOME) or `-` (EXPENSE) |
| Direction | Explicit sign; `vers:` line = EXPENSE, `de:` line = INCOME |
| Parsing library | Apache PDFBox |

---

## Data Model (V015)

### Table: `import_parser`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `code` | VARCHAR(60) | NOT NULL, UNIQUE |
| `label` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | nullable |
| `format` | VARCHAR(10) | NOT NULL, CHECK IN ('CSV', 'PDF') |
| `bank_hint` | VARCHAR(100) | nullable |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### Additions to existing tables

**`import_batch`** — columns added in V015:
- `parser_id` BIGINT FK → import_parser(id) ON DELETE SET NULL
- `bank_account_id` BIGINT FK → bank_account(id) ON DELETE SET NULL

**`financial_transaction`** — columns added in V015:
- `import_fingerprint` VARCHAR(64) — indexed (partial index WHERE NOT NULL)
- `parser_id` BIGINT FK → import_parser(id) ON DELETE SET NULL

**`bank_account`** — column added in V015:
- `owner_user_id` BIGINT FK → app_user(id) ON DELETE SET NULL — optional link to the admin user who owns this account

---

## DTOs

### `ImportParserDTO`
```
id, code, label, description, format, bankHint, active
```

---

## Business Rules

| ID | Rule |
|---|---|
| BR-UC015-01 | Parser code must be unique across the registry |
| BR-UC015-02 | File extension must match the parser format; mismatch rejected before parsing |
| BR-UC015-03 | Parsers are managed via Flyway seed data; no create/edit/delete via API |
| BR-UC015-04 | Inactive parsers are not returned in the import dropdown (`activeOnly=true` default) |
| BR-UC015-05 | Fingerprint computation is deterministic and parser-specific; same raw row always produces the same fingerprint |
| BR-UC015-06 | A row with a fingerprint already present in `financial_transaction.import_fingerprint` is marked DUPLICATE and excluded from import |

---

## Error Responses

| Condition | HTTP | Message |
|---|---|---|
| Unknown parserCode | 400 | `Parser not found or inactive` |
| File extension mismatch | 400 | `This parser expects a {FORMAT} file` |
| Unparseable file | 400 | `File could not be parsed: {detail}` |

---

## API Endpoints

| Method | Endpoint | Story |
|---|---|---|
| GET | `/api/v1/import-parsers` | US090, US091 |

---

**Last Updated:** 2026-03-10
**Branch:** develop
**Status:** ✅ Implemented
