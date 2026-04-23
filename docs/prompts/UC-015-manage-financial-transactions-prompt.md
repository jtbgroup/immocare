# ImmoCare — UC015 Manage Financial Transactions — Implementation Prompt

I want to implement Use Case UC015 - Manage Financial Transactions for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Boilers, Platform Config (platform_config table already exists), Fire Extinguishers
- **Flyway**: last migration is V012 (boilers + platform config). Fire extinguishers are in V002. Use **V013** for this feature.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

---

## USER STORIES

| Story | Title | Priority |
|-------|-------|----------|
| UC015.001 | View Transaction List | MUST HAVE |
| UC015.002 | Create Transaction Manually | MUST HAVE |
| UC015.003 | Edit Transaction | MUST HAVE |
| UC015.004 | Delete Transaction | MUST HAVE |
| UC015.005 | Classify Transaction (Category / Subcategory) | MUST HAVE |
| UC015.006 | Link Transaction to Asset(s) | SHOULD HAVE |
| UC015.007 | Import Transactions (CSV / PDF) | MUST HAVE |
| UC015.008 | Review and Confirm Imported Transactions | MUST HAVE |
| UC015.009 | Manage Tag Catalog (Categories & Subcategories) | MUST HAVE |
| UC015.010 | Manage Bank Account Catalog | MUST HAVE |
| UC015.011 | View Financial Summary and Statistics | SHOULD HAVE |
| UC015.012 | Export Transactions to CSV | SHOULD HAVE |

---

## DATABASE MIGRATION — `V013__financial_transactions.sql`

```sql
-- ─── bank_account ─────────────────────────────────────────────────────────────
CREATE TABLE bank_account (
    id             BIGSERIAL    PRIMARY KEY,
    label          VARCHAR(100) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    type           VARCHAR(10)  NOT NULL CHECK (type IN ('CURRENT', 'SAVINGS')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    owner_user_id  BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bank_account_label  UNIQUE (label),
    CONSTRAINT uq_bank_account_number UNIQUE (account_number)
);

-- ─── tag_category ─────────────────────────────────────────────────────────────
CREATE TABLE tag_category (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tag_category_name UNIQUE (name)
);

-- ─── tag_subcategory ──────────────────────────────────────────────────────────
CREATE TABLE tag_subcategory (
    id          BIGSERIAL    PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES tag_category(id) ON DELETE RESTRICT,
    name        VARCHAR(100) NOT NULL,
    direction   VARCHAR(10)  NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE', 'BOTH')),
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tag_subcategory_name_category UNIQUE (category_id, name)
);

CREATE INDEX idx_tag_subcategory_category ON tag_subcategory (category_id);

-- ─── import_batch ─────────────────────────────────────────────────────────────
CREATE TABLE import_batch (
    id              BIGSERIAL    PRIMARY KEY,
    imported_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filename        VARCHAR(255),
    parser_id       BIGINT       REFERENCES import_parser(id) ON DELETE SET NULL,
    bank_account_id BIGINT       REFERENCES bank_account(id) ON DELETE SET NULL,
    total_rows      INTEGER      NOT NULL DEFAULT 0,
    imported_count  INTEGER      NOT NULL DEFAULT 0,
    duplicate_count INTEGER      NOT NULL DEFAULT 0,
    error_count     INTEGER      NOT NULL DEFAULT 0,
    created_by      BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

-- ─── financial_transaction ────────────────────────────────────────────────────
CREATE TABLE financial_transaction (
    id                   BIGSERIAL      PRIMARY KEY,
    reference            VARCHAR(20)    NOT NULL,
    external_reference   VARCHAR(200),
    import_fingerprint   VARCHAR(64),
    transaction_date     DATE           NOT NULL,
    value_date           DATE,
    accounting_month     DATE           NOT NULL,
    amount               DECIMAL(12,2)  NOT NULL CHECK (amount > 0),
    direction            VARCHAR(10)    NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    description          TEXT,
    counterparty_account VARCHAR(50),
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                                        CHECK (status IN ('DRAFT', 'CONFIRMED', 'RECONCILED', 'CANCELLED')),
    source               VARCHAR(20)    NOT NULL CHECK (source IN ('MANUAL', 'IMPORT')),
    bank_account_id      BIGINT         REFERENCES bank_account(id) ON DELETE SET NULL,
    subcategory_id       BIGINT         REFERENCES tag_subcategory(id) ON DELETE SET NULL,
    lease_id             BIGINT         REFERENCES lease(id) ON DELETE SET NULL,
    suggested_lease_id   BIGINT         REFERENCES lease(id) ON DELETE SET NULL,
    housing_unit_id      BIGINT         REFERENCES housing_unit(id) ON DELETE SET NULL,
    building_id          BIGINT         REFERENCES building(id) ON DELETE SET NULL,
    import_batch_id      BIGINT         REFERENCES import_batch(id) ON DELETE SET NULL,
    parser_id            BIGINT         REFERENCES import_parser(id) ON DELETE SET NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_financial_transaction_reference UNIQUE (reference)
);

CREATE INDEX idx_ft_transaction_date   ON financial_transaction (transaction_date DESC);
CREATE INDEX idx_ft_accounting_month   ON financial_transaction (accounting_month DESC);
CREATE INDEX idx_ft_direction          ON financial_transaction (direction);
CREATE INDEX idx_ft_status             ON financial_transaction (status);
CREATE INDEX idx_ft_building           ON financial_transaction (building_id);
CREATE INDEX idx_ft_unit               ON financial_transaction (housing_unit_id);
CREATE INDEX idx_ft_lease              ON financial_transaction (lease_id);
CREATE INDEX idx_ft_import_batch       ON financial_transaction (import_batch_id);
CREATE INDEX idx_ft_external_ref       ON financial_transaction (external_reference);
CREATE INDEX idx_ft_subcategory        ON financial_transaction (subcategory_id);
CREATE INDEX idx_ft_bank_account       ON financial_transaction (bank_account_id);
CREATE INDEX idx_ft_fingerprint        ON financial_transaction (import_fingerprint) WHERE import_fingerprint IS NOT NULL;

-- ─── transaction_asset_link ───────────────────────────────────────────────────
-- Links a transaction to a physical asset (BOILER / FIRE_EXTINGUISHER / METER).
-- housing_unit_id and building_id are resolved server-side from the device relationship
-- and are never provided by the client.
-- amount is optional: null = full transaction amount attributed to this asset;
--   when multiple links exist, partial amounts can be entered and their sum
--   must not exceed the transaction total.
CREATE TABLE transaction_asset_link (
    id              BIGSERIAL   PRIMARY KEY,
    transaction_id  BIGINT      NOT NULL REFERENCES financial_transaction(id) ON DELETE CASCADE,
    asset_type      VARCHAR(30) NOT NULL CHECK (asset_type IN ('BOILER', 'FIRE_EXTINGUISHER', 'METER')),
    asset_id        BIGINT      NOT NULL,
    housing_unit_id BIGINT      REFERENCES housing_unit(id) ON DELETE SET NULL,
    building_id     BIGINT      REFERENCES building(id) ON DELETE SET NULL,
    amount          DECIMAL(12,2) CHECK (amount > 0),
    notes           TEXT,
    CONSTRAINT uq_asset_link UNIQUE (transaction_id, asset_type, asset_id)
);

CREATE INDEX idx_tal_transaction ON transaction_asset_link (transaction_id);
CREATE INDEX idx_tal_asset       ON transaction_asset_link (asset_type, asset_id);

-- ─── tag_learning_rule ────────────────────────────────────────────────────────
-- COUNTERPARTY_NAME is NOT a valid match_field.
-- Valid values: COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE
-- For ASSET_TYPE: match_value is the asset type name (BOILER, FIRE_EXTINGUISHER, METER)
CREATE TABLE tag_learning_rule (
    id              BIGSERIAL    PRIMARY KEY,
    match_field     VARCHAR(30)  NOT NULL
                                 CHECK (match_field IN ('COUNTERPARTY_ACCOUNT', 'DESCRIPTION', 'ASSET_TYPE')),
    match_value     VARCHAR(200) NOT NULL,
    subcategory_id  BIGINT       NOT NULL REFERENCES tag_subcategory(id) ON DELETE CASCADE,
    confidence      INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at TIMESTAMP,
    CONSTRAINT uq_learning_rule UNIQUE (match_field, match_value, subcategory_id)
);

CREATE INDEX idx_learning_rule_field_value ON tag_learning_rule (match_field, match_value);

-- ─── accounting_month_rule ────────────────────────────────────────────────────
CREATE TABLE accounting_month_rule (
    id                   BIGSERIAL    PRIMARY KEY,
    subcategory_id       BIGINT       NOT NULL REFERENCES tag_subcategory(id) ON DELETE CASCADE,
    counterparty_account VARCHAR(50),
    month_offset         INTEGER      NOT NULL DEFAULT 0,
    confidence           INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at      TIMESTAMP,
    CONSTRAINT uq_accounting_month_rule UNIQUE (subcategory_id, counterparty_account)
);

CREATE INDEX idx_amr_subcategory ON accounting_month_rule (subcategory_id);

-- ─── platform_config additions for CSV import ─────────────────────────────────
INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('csv.import.delimiter',                       ';',          'STRING',  'CSV column delimiter'),
    ('csv.import.date_format',                     'dd/MM/yyyy', 'STRING',  'Date format in CSV'),
    ('csv.import.skip_header_rows',                '1',          'INTEGER', 'Number of header rows to skip'),
    ('csv.import.col.date',                        '0',          'INTEGER', 'Column index for transaction date'),
    ('csv.import.col.amount',                      '1',          'INTEGER', 'Column index for amount'),
    ('csv.import.col.description',                 '2',          'INTEGER', 'Column index for description'),
    ('csv.import.col.counterparty_account',        '3',          'INTEGER', 'Column index for counterparty IBAN'),
    ('csv.import.col.external_reference',          '4',          'INTEGER', 'Column index for bank transaction reference'),
    ('csv.import.col.bank_account',                '5',          'INTEGER', 'Column index for own bank account IBAN'),
    ('csv.import.col.value_date',                  '-1',         'INTEGER', 'Column index for value date (-1 = absent)'),
    ('csv.import.suggestion.confidence.threshold', '3',          'INTEGER', 'Min confidence to show tag suggestion')
ON CONFLICT (config_key) DO NOTHING;

-- ─── seed: tag categories ─────────────────────────────────────────────────────
INSERT INTO tag_category (name) VALUES
    ('Administration'), ('Consommables'), ('Dépôt'), ('Location'),
    ('Maintenance'), ('Prime'), ('Rente'), ('Taxes'), ('Travaux');

-- ─── seed: tag subcategories ──────────────────────────────────────────────────
INSERT INTO tag_subcategory (category_id, name, direction) VALUES
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'Assurance habitation', 'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'PEB',                  'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'Petits frais',         'BOTH'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Adoucisseur',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Eau',                  'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Electricité',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Gaz',                  'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Dépôt'),          'Garantie locative',    'BOTH'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Annonce',              'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Etat des lieux',       'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Garantie locative',    'BOTH'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Loyer',                'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Petits travaux',       'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Adoucisseur',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Chaudière',            'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Electricité',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Extincteurs',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Insert',               'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Matériel',             'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Nettoyage communs',    'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Petits travaux',       'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Plomberie',            'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Prime'),          'Banque',               'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Rente'),          'Retour',               'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Rente'),          'Usufruit',             'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Taxes'),          'Précompte immobilier', 'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Travaux'),        'Structure',            'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Travaux'),        'Toiture',              'EXPENSE');
```

---

## BACKEND

### Enums

```java
public enum TransactionDirection  { INCOME, EXPENSE }
public enum TransactionStatus     { DRAFT, CONFIRMED, RECONCILED, CANCELLED }
public enum TransactionSource     { MANUAL, IMPORT }
public enum AssetType             { BOILER, FIRE_EXTINGUISHER, METER }
public enum SubcategoryDirection  { INCOME, EXPENSE, BOTH }
public enum BankAccountType       { CURRENT, SAVINGS }
// TagMatchField: COUNTERPARTY_NAME removed; ASSET_TYPE added
public enum TagMatchField         { COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE }
```

### Entities

**`BankAccount`** — table `bank_account`. Fields: `id`, `label`, `accountNumber`, `type` (BankAccountType), `isActive` (boolean), `ownerUser` (@ManyToOne AppUser, nullable), `createdAt`, `updatedAt`. @PrePersist/@PreUpdate.

**`TagCategory`** — table `tag_category`. Fields: `id`, `name`, `description`, `createdAt`, `updatedAt`. @PrePersist/@PreUpdate. `@OneToMany(mappedBy="category", fetch=LAZY)` → subcategories.

**`TagSubcategory`** — table `tag_subcategory`. Fields: `id`, `category` (@ManyToOne, NOT NULL), `name`, `direction` (SubcategoryDirection), `description`, `createdAt`, `updatedAt`. @PrePersist/@PreUpdate.

**`ImportBatch`** — table `import_batch`. Fields: `id`, `importedAt`, `filename`, `parser` (@ManyToOne ImportParser, nullable), `bankAccount` (@ManyToOne BankAccount, nullable), `totalRows`, `importedCount`, `duplicateCount`, `errorCount`, `createdBy` (@ManyToOne AppUser, nullable). @PrePersist sets importedAt.

**`FinancialTransaction`** — table `financial_transaction`. Fields: `id`, `reference`, `externalReference`, `importFingerprint`, `transactionDate` (LocalDate), `valueDate` (LocalDate, nullable), `accountingMonth` (LocalDate — always 1st of month), `amount` (BigDecimal), `direction` (TransactionDirection), `description`, `counterpartyAccount`, `status` (TransactionStatus), `source` (TransactionSource). Relations (all nullable ManyToOne unless noted): `bankAccount`, `subcategory`, `lease`, `suggestedLease` (column: suggested_lease_id), `housingUnit`, `building`, `importBatch`, `parser`. `@OneToMany(mappedBy="transaction", cascade=ALL, orphanRemoval=true, fetch=LAZY)` → `assetLinks`. @PrePersist/@PreUpdate.

**`TransactionAssetLink`** — table `transaction_asset_link`. Fields: `id`, `transaction` (@ManyToOne FinancialTransaction, NOT NULL), `assetType` (AssetType), `assetId` (Long), `housingUnit` (@ManyToOne HousingUnit, nullable), `building` (@ManyToOne Building, nullable), `amount` (BigDecimal, nullable), `notes`. No @PrePersist/@PreUpdate.

**`TagLearningRule`** — table `tag_learning_rule`. Fields: `id`, `matchField` (TagMatchField), `matchValue`, `subcategory` (@ManyToOne), `confidence` (int), `lastMatchedAt` (LocalDateTime, nullable).

**`AccountingMonthRule`** — table `accounting_month_rule`. Fields: `id`, `subcategory` (@ManyToOne), `counterpartyAccount` (nullable), `monthOffset` (int), `confidence` (int), `lastMatchedAt` (LocalDateTime, nullable).

### DTOs (all Java records)

**`TransactionAssetLinkDTO`**:
```java
record TransactionAssetLinkDTO(
    Long id,
    AssetType assetType,
    Long assetId,
    String assetLabel,       // resolved: brand+model for BOILER, identificationNumber for FIRE_EXTINGUISHER, meterNumber+type for METER
    Long housingUnitId,      // resolved server-side from device relationship
    String unitNumber,       // resolved server-side
    Long buildingId,         // resolved server-side
    String buildingName,     // resolved server-side
    BigDecimal amount,       // nullable
    String notes
) {}
```

**`SaveAssetLinkRequest`**:
```java
record SaveAssetLinkRequest(
    @NotNull AssetType assetType,
    @NotNull Long assetId,
    @DecimalMin(value = "0.01") BigDecimal amount,  // nullable
    String notes
) {}
// housingUnitId and buildingId are NOT in this request — resolved server-side only
```

**`FinancialTransactionDTO`** (full detail):
```java
record FinancialTransactionDTO(
    Long id, String reference, String externalReference,
    LocalDate transactionDate, LocalDate valueDate, LocalDate accountingMonth,
    BigDecimal amount, TransactionDirection direction,
    String description, String counterpartyAccount,
    TransactionStatus status, TransactionSource source,
    Long bankAccountId, String bankAccountLabel,
    Long subcategoryId, String subcategoryName,
    Long categoryId, String categoryName,
    Long leaseId, String leaseReference,
    Long suggestedLeaseId, String suggestedLeaseReference,
    Long housingUnitId, String unitNumber,
    Long buildingId, String buildingName,
    Long importBatchId,
    List<TransactionAssetLinkDTO> assetLinks,
    boolean editable,
    LocalDateTime createdAt, LocalDateTime updatedAt
) {}
```

**`FinancialTransactionSummaryDTO`** (list item — no counterpartyName):
```java
record FinancialTransactionSummaryDTO(
    Long id, String reference, LocalDate transactionDate, LocalDate accountingMonth,
    TransactionDirection direction, BigDecimal amount,
    String counterpartyAccount, TransactionStatus status, TransactionSource source,
    String bankAccountLabel, String categoryName, String subcategoryName,
    String buildingName, String unitNumber, String leaseReference,
    Long suggestedLeaseId, Long buildingId, Long housingUnitId
) {}
```

**`PagedTransactionResponse`**:
```java
record PagedTransactionResponse(
    List<FinancialTransactionSummaryDTO> content,
    int page, int size, long totalElements, int totalPages,
    BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netBalance
) {}
```

**`CreateTransactionRequest`**:
```java
record CreateTransactionRequest(
    @NotNull TransactionDirection direction,
    @NotNull LocalDate transactionDate,
    LocalDate valueDate,
    @NotNull LocalDate accountingMonth,
    @NotNull @Positive BigDecimal amount,
    String description,
    @Size(max=50) String counterpartyAccount,
    Long bankAccountId,
    Long subcategoryId,
    Long leaseId,
    Long housingUnitId,
    Long buildingId,
    List<SaveAssetLinkRequest> assetLinks
) {}
```

**`UpdateTransactionRequest`** — identical to `CreateTransactionRequest` (reference, externalReference, source excluded).

**`ImportPreviewRowDTO`** (no counterpartyName):
```java
record ImportPreviewRowDTO(
    int rowNumber, String fingerprint,
    LocalDate transactionDate, BigDecimal amount,
    TransactionDirection direction,
    String description, String counterpartyAccount,
    String parseError, boolean duplicateInDb,
    Long suggestedSubcategoryId, String suggestedSubcategoryName,
    Long suggestedCategoryId, String suggestedCategoryName,
    int suggestionConfidence,
    SuggestedLeaseDTO suggestedLease
) {}
```

**`ImportRowEnrichmentDTO`**:
```java
record ImportRowEnrichmentDTO(
    String fingerprint,
    TransactionDirection directionOverride,
    Long subcategoryId,
    Long bankAccountId,
    Long buildingId,
    Long housingUnitId,
    Long leaseId
) {}
```

**`ImportBatchResultDTO`**:
```java
record ImportBatchResultDTO(
    Long batchId, int totalRows, int importedCount,
    int duplicateCount, int errorCount,
    List<ImportRowErrorDTO> errors
) {}
```

**`TransactionStatisticsDTO`** — see UC015 use case document for full structure (unchanged).

**`SubcategorySuggestionDTO`**:
```java
record SubcategorySuggestionDTO(
    Long subcategoryId, String subcategoryName,
    Long categoryId, String categoryName, int confidence
) {}
```

**`AccountingMonthSuggestionDTO`**:
```java
record AccountingMonthSuggestionDTO(LocalDate accountingMonth, int confidence) {}
```

### Repositories

**`FinancialTransactionRepository`** extends `JpaRepository`, `JpaSpecificationExecutor`:
```java
boolean existsByImportFingerprintAndTransactionDateAndAmount(
    String fingerprint, LocalDate date, BigDecimal amount);

@Query(value = """
    SELECT COALESCE(MAX(CAST(SUBSTRING(reference FROM 10) AS INTEGER)), 0) + 1
    FROM financial_transaction WHERE reference LIKE :prefix
    """, nativeQuery = true)
int nextSequenceForYear(@Param("prefix") String prefix);

Page<FinancialTransaction> findByImportBatchId(Long batchId, Pageable pageable);
```

**`TransactionAssetLinkRepository`** extends `JpaRepository<TransactionAssetLink, Long>`:
```java
List<TransactionAssetLink> findByTransactionId(Long transactionId);
boolean existsByTransactionIdAndAssetTypeAndAssetId(Long txId, AssetType type, Long assetId);
long countByAssetTypeAndAssetId(AssetType type, Long assetId);
```

**`TagLearningRuleRepository`** extends `JpaRepository<TagLearningRule, Long>`:
```java
// COUNTERPARTY_ACCOUNT and DESCRIPTION: exact/contains match
// ASSET_TYPE: exact match on match_value (e.g. "BOILER")
@Query("""
    SELECT r FROM TagLearningRule r
    WHERE r.matchField = :field
    AND LOWER(r.matchValue) = LOWER(:value)
    AND r.confidence >= :minConf
    ORDER BY r.confidence DESC
    """)
List<TagLearningRule> findSuggestions(
    @Param("field") TagMatchField field,
    @Param("value") String value,
    @Param("minConf") int minConf);

Optional<TagLearningRule> findByMatchFieldAndMatchValueIgnoreCaseAndSubcategoryId(
    TagMatchField field, String value, Long subcategoryId);
```

**Other repositories** — `BankAccountRepository`, `TagCategoryRepository`, `TagSubcategoryRepository`, `ImportBatchRepository`, `AccountingMonthRuleRepository`, `TransactionSpecification` — unchanged from UC015 use case document.

### Services

**`LearningService`** (`@Service`, `@Transactional(readOnly=true)`):

```java
List<SubcategorySuggestionDTO> suggestSubcategory(
    String counterpartyAccount, String description,
    String assetType,                    // nullable — asset type name e.g. "BOILER"
    TransactionDirection direction,
    int minConfidence);
// Priority order for lookup:
// 1. ASSET_TYPE (if assetType non-null): exact match on match_value
// 2. COUNTERPARTY_ACCOUNT (if counterpartyAccount non-blank): exact match
// 3. DESCRIPTION (if description non-blank): contains match
// Filter results to subcategories compatible with direction.
// Deduplicate by subcategoryId (keep highest confidence). Return sorted DESC.
// NOTE: COUNTERPARTY_NAME is no longer a valid match_field and must not be used.

AccountingMonthSuggestionDTO suggestAccountingMonth(Long subcategoryId, String counterpartyAccount);

@Transactional
void reinforceTagRule(Long subcategoryId, TagMatchField field, String matchValue);
// find or create TagLearningRule for (field, matchValue, subcategoryId)
// confidence += 1, lastMatchedAt = now
// Used for COUNTERPARTY_ACCOUNT, DESCRIPTION, and ASSET_TYPE fields

@Transactional
void reinforceAccountingMonthRule(Long subcategoryId, String counterpartyAccount, int offset);
```

**`FinancialTransactionService`** (`@Service`, `@Transactional(readOnly=true)`):

Key methods and logic:

```java
@Transactional
FinancialTransactionDTO create(CreateTransactionRequest req, AppUser currentUser);
// BR-UC015-02: direction=EXPENSE && leaseId!=null → throw TransactionValidationException
// BR-UC015-03: unitId set → load unit → set buildingId = unit.building.id
// BR-UC015-06: validate subcategory direction compatibility
// BR-UC015-14: for each asset link → resolve housingUnit and building server-side:
//   BOILER       → boiler.housingUnit → boiler.housingUnit.building
//   FIRE_EXT     → extinguisher.unit (nullable) + extinguisher.building
//   METER        → meter.ownerType/ownerId → unit or building
// BR-UC015-09: if building set on transaction, BOILER must belong to that building
// BR-UC015-15: sum of non-null asset link amounts must not exceed transaction.amount
// BR-UC015-17: if assetLinks non-empty → for each distinct assetType in links:
//   read platform_config key asset.type.subcategory.mapping.{ASSET_TYPE}
//   if non-empty and subcategoryId not already set → pre-fill subcategoryId
//   reinforce tag learning rule for ASSET_TYPE match_field
// After save: if counterpartyAccount non-blank and subcategoryId set:
//   reinforceTagRule(subcategoryId, COUNTERPARTY_ACCOUNT, counterpartyAccount)
// Reference: "TXN-" + year + "-" + String.format("%05d", nextSequence)
// accountingMonth: req.accountingMonth().withDayOfMonth(1)
// status = CONFIRMED, source = MANUAL

private void resolveAssetLinkContext(TransactionAssetLink link);
// sets link.housingUnit and link.building based on asset type and asset id

private String resolveAssetLabel(AssetType type, Long assetId);
// BOILER: brand + " " + model
// FIRE_EXTINGUISHER: identificationNumber
// METER: meterNumber + " (" + type + ")"
```

**`CsvImportService`** — remove all references to `counterpartyName` from `ParsedCsvRow` and `ImportPreviewRowDTO`. The `col.counterparty_name` platform config key is removed. Only `counterpartyAccount` is parsed from the CSV.

### Exceptions — add to GlobalExceptionHandler

```java
TransactionNotFoundException          → 404
TransactionNotEditableException       → 422
TransactionValidationException        → 400
SubcategoryDirectionMismatchException → 400
SubcategoryInUseException             → 409
SubcategoryNotFoundException          → 404
CategoryNotFoundException             → 404
CategoryHasSubcategoriesException     → 409
BankAccountNotFoundException          → 404
BankAccountDuplicateLabelException    → 409
BankAccountDuplicateNumberException   → 409
ImportBatchNotFoundException          → 404
AssetLinkValidationException          → 400  // amount sum exceeds total, BOILER not in building, duplicate asset
```

### Controllers

**`TransactionController`** — endpoints unchanged from UC015 use case document.

**Additional endpoints on existing controllers** (modify, do not regenerate):
- `BoilerController`: `GET /api/v1/boilers/{boilerId}/transaction-count → long`
- `FireExtinguisherController`: `GET /api/v1/fire-extinguishers/{id}/transaction-count → long`

Both via `TransactionAssetLinkRepository.countByAssetTypeAndAssetId(...)`.

---

## FRONTEND

### Models — `transaction.model.ts`

```typescript
export type TransactionDirection  = 'INCOME' | 'EXPENSE';
export type TransactionStatus     = 'DRAFT' | 'CONFIRMED' | 'RECONCILED' | 'CANCELLED';
export type TransactionSource     = 'MANUAL' | 'IMPORT';
export type AssetType             = 'BOILER' | 'FIRE_EXTINGUISHER' | 'METER';
export type SubcategoryDirection  = 'INCOME' | 'EXPENSE' | 'BOTH';
export type BankAccountType       = 'CURRENT' | 'SAVINGS';

export interface TransactionAssetLink {
  id?: number;
  assetType: AssetType;
  assetId: number;
  assetLabel?: string;
  housingUnitId?: number;   // read-only, resolved server-side
  unitNumber?: string;       // read-only, resolved server-side
  buildingId?: number;       // read-only, resolved server-side
  buildingName?: string;     // read-only, resolved server-side
  amount?: number;           // nullable — partial amount
  notes?: string;
}

export interface SaveAssetLinkRequest {
  assetType: AssetType;
  assetId: number;
  amount?: number;   // do NOT include housingUnitId or buildingId
  notes?: string;
}

export interface FinancialTransactionSummary {
  id: number; reference: string;
  transactionDate: string; accountingMonth: string;
  direction: TransactionDirection; amount: number;
  counterpartyAccount?: string;   // no counterpartyName
  status: TransactionStatus; source: TransactionSource;
  bankAccountLabel?: string; categoryName?: string; subcategoryName?: string;
  buildingName?: string; unitNumber?: string; leaseReference?: string;
  suggestedLeaseId?: number; buildingId?: number; housingUnitId?: number;
}

// ImportPreviewRow — no counterpartyName field
export interface ImportPreviewRow {
  rowNumber: number; fingerprint: string;
  transactionDate: string; amount: number;
  direction: TransactionDirection;
  description?: string; counterpartyAccount?: string;
  parseError?: string; duplicateInDb: boolean;
  suggestedSubcategoryId?: number; suggestedSubcategoryName?: string;
  suggestedCategoryId?: number; suggestedCategoryName?: string;
  suggestionConfidence: number;
  suggestedLease?: SuggestedLease;
}

// All other interfaces (FinancialTransaction, PagedTransactionResponse, etc.)
// unchanged from UC015 use case document except removal of counterpartyName everywhere.
```

### Component — `AssetLinkEditorComponent`

Standalone. Inputs: `[transactionAmount]: number`, `[buildingId]: number | null`. Output: `(linksChanged): SaveAssetLinkRequest[]`.

**Layout per row:**
```
[Type ▾]  [Device picker (search)]  [Unit — Building (read-only)]  [Amount]  [Notes]  [×]
```

**Behavior:**
- Asset type selector (BOILER / FIRE_EXTINGUISHER / METER).
- Device picker is a **text search** input (min 2 chars) that calls a dedicated endpoint filtered by `buildingId` if set on the transaction. Each result shows enough context: brand/model or identifier + unit number + building name.
- Once a device is selected, `unitNumber` and `buildingName` are displayed read-only on the row (resolved from the search result metadata — no separate API call needed).
- `amount` field optional. When multiple rows are present, show ventilation summary below:
  ```
  Ventilated: X.XX € / Transaction total: Y.YY € [✓ or ✗]
  ```
  Green ✓ if sum ≤ total, red ✗ if sum > total (client-side validation, also enforced server-side).
- When asset type is selected: call `GET /api/v1/config/asset-type-mappings` (or use cached value from parent), if a subcategory is mapped for this asset type, emit an event `(subcategoryPreFill): Long` so the parent form can pre-fill the subcategory field. The admin can override freely.
- "Add asset link" button adds a new empty row.
- "×" removes the row without confirmation.
- Section is only rendered when `direction = EXPENSE`.

**Device search endpoints** (add to existing controllers, do not regenerate):
```
GET /api/v1/boilers/search?q=&buildingId=         → List<BoilerSearchResultDTO>
GET /api/v1/fire-extinguishers/search?q=&buildingId= → List<FireExtinguisherSearchResultDTO>
GET /api/v1/meters/search?q=&buildingId=           → List<MeterSearchResultDTO>
```

Each result DTO includes: `id`, `label` (display text), `unitNumber` (nullable), `buildingName`.

### Other frontend components

All other components (`TransactionListComponent`, `TransactionFormComponent`, `TransactionDetailComponent`, `TransactionImportComponent`, `TransactionReviewComponent`, `TransactionDashboardComponent`, `TransactionSettingsComponent`) follow the UC015 use case document with these corrections:

- Remove all references to `counterpartyName` from column definitions, filter fields, CSV export columns, and import preview table.
- `TransactionFormComponent`: the Asset Links section uses `AssetLinkEditorComponent`. The section is hidden when `direction = INCOME`. It is always optional — no validation error if empty for EXPENSE transactions.
- `TransactionReviewComponent`: remove `counterpartyName` column from batch review table.
- CSV export columns (UC015.012): remove `counterparty_name` column. Updated order: `reference`, `external_reference`, `transaction_date`, `accounting_month`, `direction`, `amount`, `category`, `subcategory`, `bank_account_label`, `counterparty_account`, `description`, `building`, `housing_unit`, `lease_reference`, `status`, `source`.

---

## MODIFICATIONS TO EXISTING COMPONENTS

### `building-details.component`
Add collapsible "Financial" section: total income + expenses summary via statistics endpoint, link to filtered transaction list.

### `housing-unit-details.component`
Same pattern at unit level.

### `boiler-section.component`
Add "Related expenses (N)" badge via `GET /api/v1/boilers/{id}/transaction-count`. Click → `/transactions?tab=list&assetType=BOILER&assetId={id}`.

### `fire-extinguisher-section.component`
Add "Related expenses (N)" badge via `GET /api/v1/fire-extinguishers/{id}/transaction-count`. Click → `/transactions?tab=list&assetType=FIRE_EXTINGUISHER&assetId={id}`.

### Sidebar navigation
Add **Transactions** as top-level nav item (after Leases, before Administration). Route: `/transactions`.

---

## BUSINESS RULES SUMMARY

| ID | Rule |
|---|---|
| BR-UC015-01 | amount always positive; direction carries sign semantics |
| BR-UC015-02 | lease_id only when direction = INCOME |
| BR-UC015-03 | housingUnitId set → buildingId auto-derived |
| BR-UC015-04 | RECONCILED transactions fully immutable |
| BR-UC015-05 | Deduplication by SHA-256 fingerprint |
| BR-UC015-06 | Subcategory direction must be compatible with transaction direction |
| BR-UC015-07 | Learning rule confidence += 1 on each confirmation |
| BR-UC015-08 | Accounting month rule priority: specific > generic > default 0 |
| BR-UC015-09 | BOILER asset link: boiler must belong to transaction's building (when building set) |
| BR-UC015-11 | accountingMonth stored as first day of month |
| BR-UC015-12 | Enriched import rows → CONFIRMED; others → DRAFT |
| BR-UC015-13 | Lease suggestion loads all lease statuses |
| BR-UC015-14 | housingUnitId and buildingId on asset links resolved server-side only |
| BR-UC015-15 | Sum of asset link amounts must not exceed transaction total |
| BR-UC015-16 | Asset links are optional even for EXPENSE transactions |
| BR-UC015-17 | Asset type mapping from platform_config pre-fills subcategory; admin can override |
| BR-UC015-18 | Valid tag_learning_rule match_field values: COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE only |
| BR-UC015-19 | ASSET_TYPE learning rule: match_value = asset type name; confidence reinforced when suggestion accepted |

---

## ACCEPTANCE CRITERIA

- [ ] Transaction list: no counterpartyName column, counterpartyAccount shown instead
- [ ] Asset links: section optional, EXPENSE only, device picker with text search
- [ ] Asset links: unit and building resolved and displayed read-only after device selection
- [ ] Asset links: partial amount per row, ventilation summary shown, sum validated ≤ total
- [ ] Asset links: subcategory pre-filled from platform config mapping when asset type selected
- [ ] Learning engine: COUNTERPARTY_NAME not used anywhere; ASSET_TYPE used for asset-based suggestions
- [ ] Import preview: no counterpartyName column in preview table
- [ ] CSV export: no counterparty_name column
- [ ] Transaction count badge on boiler and fire extinguisher cards
- [ ] All UC015.001–UC015.012 acceptance criteria verified

**Last Updated:** 2026-04-04 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
