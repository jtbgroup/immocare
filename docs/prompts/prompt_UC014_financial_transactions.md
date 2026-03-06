# ImmoCare — UC014 Manage Financial Transactions — Implementation Prompt

I want to implement Use Case UC014 (Manage Financial Transactions) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 4.x + Angular 19+ + PostgreSQL 16 — API-First, ADMIN only
- **Branch**: `develop`
- **Already implemented**: Buildings, Housing Units, Rooms, PEB Scores, Rents, Users, Meters, Persons, Leases, Boilers, Platform Config (`platform_config` table already exists), Fire Extinguishers
- **Flyway**: last migration is V012 (boilers + platform config). Fire extinguishers are in V002. Use **V013** for this feature.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

---

## USER STORIES

| Story | Title | Priority |
|-------|-------|----------|
| US078 | View Transaction List | MUST HAVE |
| US079 | Create Transaction Manually | MUST HAVE |
| US080 | Edit Transaction | MUST HAVE |
| US081 | Delete Transaction | MUST HAVE |
| US082 | Classify Transaction (Category / Subcategory) | MUST HAVE |
| US083 | Link Transaction to Asset(s) | SHOULD HAVE |
| US084 | Import Transactions from CSV | MUST HAVE |
| US085 | Review and Confirm Imported Transactions | MUST HAVE |
| US086 | Manage Tag Catalog (Categories & Subcategories) | MUST HAVE |
| US087 | Manage Bank Account Catalog | MUST HAVE |
| US088 | View Financial Summary and Statistics | SHOULD HAVE |
| US089 | Export Transactions to CSV | SHOULD HAVE |

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
    transaction_date     DATE           NOT NULL,
    value_date           DATE,
    accounting_month     DATE           NOT NULL,  -- stored as first day of month
    amount               DECIMAL(12,2)  NOT NULL CHECK (amount > 0),
    direction            VARCHAR(10)    NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    description          TEXT,
    counterparty_name    VARCHAR(200),
    counterparty_account VARCHAR(50),
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                                        CHECK (status IN ('DRAFT', 'CONFIRMED', 'RECONCILED')),
    source               VARCHAR(20)    NOT NULL CHECK (source IN ('MANUAL', 'CSV_IMPORT')),
    bank_account_id      BIGINT         REFERENCES bank_account(id) ON DELETE SET NULL,
    subcategory_id       BIGINT         REFERENCES tag_subcategory(id) ON DELETE SET NULL,
    lease_id             BIGINT         REFERENCES lease(id) ON DELETE SET NULL,
    suggested_lease_id   BIGINT         REFERENCES lease(id) ON DELETE SET NULL,
    housing_unit_id      BIGINT         REFERENCES housing_unit(id) ON DELETE SET NULL,
    building_id          BIGINT         REFERENCES building(id) ON DELETE SET NULL,
    import_batch_id      BIGINT         REFERENCES import_batch(id) ON DELETE SET NULL,
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

-- ─── transaction_asset_link ───────────────────────────────────────────────────
CREATE TABLE transaction_asset_link (
    id             BIGSERIAL   PRIMARY KEY,
    transaction_id BIGINT      NOT NULL REFERENCES financial_transaction(id) ON DELETE CASCADE,
    asset_type     VARCHAR(30) NOT NULL CHECK (asset_type IN ('BOILER', 'FIRE_EXTINGUISHER', 'METER')),
    asset_id       BIGINT      NOT NULL,
    notes          TEXT,
    CONSTRAINT uq_asset_link UNIQUE (transaction_id, asset_type, asset_id)
);

CREATE INDEX idx_tal_transaction ON transaction_asset_link (transaction_id);
CREATE INDEX idx_tal_asset       ON transaction_asset_link (asset_type, asset_id);

-- ─── tag_learning_rule ────────────────────────────────────────────────────────
CREATE TABLE tag_learning_rule (
    id              BIGSERIAL    PRIMARY KEY,
    match_field     VARCHAR(30)  NOT NULL
                                 CHECK (match_field IN ('COUNTERPARTY_ACCOUNT', 'COUNTERPARTY_NAME', 'DESCRIPTION')),
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
    counterparty_account VARCHAR(50),  -- NULL = generic rule for subcategory
    month_offset         INTEGER      NOT NULL DEFAULT 0,
    confidence           INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at      TIMESTAMP,
    CONSTRAINT uq_accounting_month_rule UNIQUE (subcategory_id, counterparty_account)
);

CREATE INDEX idx_amr_subcategory ON accounting_month_rule (subcategory_id);

-- ─── platform_config additions for CSV import ─────────────────────────────────
INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('csv.import.delimiter',                    ';',            'STRING',  'CSV column delimiter'),
    ('csv.import.date_format',                  'dd/MM/yyyy',   'STRING',  'Date format in CSV'),
    ('csv.import.skip_header_rows',             '1',            'INTEGER', 'Number of header rows to skip'),
    ('csv.import.col.date',                     '0',            'INTEGER', 'Column index for transaction date'),
    ('csv.import.col.amount',                   '1',            'INTEGER', 'Column index for amount (negative = EXPENSE)'),
    ('csv.import.col.description',              '2',            'INTEGER', 'Column index for description / communication'),
    ('csv.import.col.counterparty_name',        '3',            'INTEGER', 'Column index for counterparty name'),
    ('csv.import.col.counterparty_account',     '4',            'INTEGER', 'Column index for counterparty IBAN'),
    ('csv.import.col.external_reference',       '5',            'INTEGER', 'Column index for bank transaction reference'),
    ('csv.import.col.bank_account',             '6',            'INTEGER', 'Column index for own bank account IBAN'),
    ('csv.import.col.value_date',               '-1',           'INTEGER', 'Column index for value date (-1 = absent)'),
    ('csv.import.suggestion.confidence.threshold', '3',         'INTEGER', 'Min confidence to show tag suggestion')
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
public enum TransactionDirection { INCOME, EXPENSE }
public enum TransactionStatus    { DRAFT, CONFIRMED, RECONCILED }
public enum TransactionSource    { MANUAL, CSV_IMPORT }
public enum AssetType            { BOILER, FIRE_EXTINGUISHER, METER }
public enum SubcategoryDirection { INCOME, EXPENSE, BOTH }
public enum BankAccountType      { CURRENT, SAVINGS }
public enum TagMatchField        { COUNTERPARTY_ACCOUNT, COUNTERPARTY_NAME, DESCRIPTION }
```

### Entities

**1. `BankAccount`** — table `bank_account`
Fields: `id`, `label`, `accountNumber`, `type` (BankAccountType), `isActive` (boolean, default true), `createdAt`, `updatedAt`. `@PrePersist` / `@PreUpdate`.

**2. `TagCategory`** — table `tag_category`
Fields: `id`, `name`, `description`, `createdAt`, `updatedAt`. `@PrePersist` / `@PreUpdate`.
`@OneToMany(mappedBy="category", fetch=LAZY)` → `List<TagSubcategory> subcategories`.

**3. `TagSubcategory`** — table `tag_subcategory`
Fields: `id`, `category` (`@ManyToOne TagCategory`, NOT NULL), `name`, `direction` (SubcategoryDirection), `description`, `createdAt`, `updatedAt`. `@PrePersist` / `@PreUpdate`.

**4. `ImportBatch`** — table `import_batch`
Fields: `id`, `importedAt`, `filename`, `totalRows`, `importedCount`, `duplicateCount`, `errorCount`, `createdBy` (`@ManyToOne AppUser`, nullable). `@PrePersist` sets `importedAt`.

**5. `FinancialTransaction`** — table `financial_transaction`
Fields: `id`, `reference`, `externalReference`, `transactionDate` (LocalDate), `valueDate` (LocalDate, nullable), `accountingMonth` (LocalDate — always 1st of month), `amount` (BigDecimal), `direction` (TransactionDirection), `description`, `counterpartyName`, `counterpartyAccount`, `status` (TransactionStatus, default DRAFT), `source` (TransactionSource).
Relations (all nullable ManyToOne unless noted):
- `bankAccount` → `BankAccount`
- `subcategory` → `TagSubcategory`
- `lease` → `Lease`
- `suggestedLease` → `Lease` (maps to column `suggested_lease_id`)
- `housingUnit` → `HousingUnit`
- `building` → `Building`
- `importBatch` → `ImportBatch`
- `assetLinks`: `@OneToMany(mappedBy="transaction", cascade=ALL, orphanRemoval=true, fetch=LAZY)` → `List<TransactionAssetLink>`
`@PrePersist` / `@PreUpdate` on `createdAt` / `updatedAt`.

**6. `TransactionAssetLink`** — table `transaction_asset_link`
Fields: `id`, `transaction` (`@ManyToOne FinancialTransaction`, NOT NULL), `assetType` (AssetType), `assetId` (Long), `notes`.

**7. `TagLearningRule`** — table `tag_learning_rule`
Fields: `id`, `matchField` (TagMatchField), `matchValue`, `subcategory` (`@ManyToOne TagSubcategory`), `confidence` (int, default 1), `lastMatchedAt` (LocalDateTime, nullable).

**8. `AccountingMonthRule`** — table `accounting_month_rule`
Fields: `id`, `subcategory` (`@ManyToOne TagSubcategory`), `counterpartyAccount` (nullable), `monthOffset` (int, default 0), `confidence` (int, default 1), `lastMatchedAt` (LocalDateTime, nullable).

---

### DTOs (all Java records)

**`BankAccountDTO`**:
```java
record BankAccountDTO(Long id, String label, String accountNumber,
    BankAccountType type, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

**`SaveBankAccountRequest`**:
```java
record SaveBankAccountRequest(
    @NotBlank @Size(max=100) String label,
    @NotBlank @Size(max=50)  String accountNumber,
    @NotNull BankAccountType type,
    boolean isActive) {}
```

**`TagCategoryDTO`**:
```java
record TagCategoryDTO(Long id, String name, String description,
    int subcategoryCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

**`TagSubcategoryDTO`**:
```java
record TagSubcategoryDTO(Long id, Long categoryId, String categoryName,
    String name, SubcategoryDirection direction, String description,
    long usageCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

**`SaveTagCategoryRequest`**:
```java
record SaveTagCategoryRequest(@NotBlank @Size(max=100) String name, String description) {}
```

**`SaveTagSubcategoryRequest`**:
```java
record SaveTagSubcategoryRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max=100) String name,
    @NotNull SubcategoryDirection direction,
    String description) {}
```

**`TransactionAssetLinkDTO`**:
```java
record TransactionAssetLinkDTO(Long id, AssetType assetType, Long assetId,
    String assetLabel, String notes) {}
// assetLabel resolved by service: brand+model for BOILER, identificationNumber for FIRE_EXTINGUISHER,
// meterNumber+type for METER
```

**`SaveAssetLinkRequest`**:
```java
record SaveAssetLinkRequest(@NotNull AssetType assetType, @NotNull Long assetId, String notes) {}
```

**`FinancialTransactionDTO`** (full detail):
```java
record FinancialTransactionDTO(
    Long id, String reference, String externalReference,
    LocalDate transactionDate, LocalDate valueDate, LocalDate accountingMonth,
    BigDecimal amount, TransactionDirection direction,
    String description, String counterpartyName, String counterpartyAccount,
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
    boolean editable,   // false when status = RECONCILED
    LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

**`FinancialTransactionSummaryDTO`** (list item):
```java
record FinancialTransactionSummaryDTO(
    Long id, String reference, LocalDate transactionDate, LocalDate accountingMonth,
    TransactionDirection direction, BigDecimal amount,
    String counterpartyName, TransactionStatus status, TransactionSource source,
    String bankAccountLabel, String categoryName, String subcategoryName,
    String buildingName, String unitNumber, String leaseReference) {}
```

**`PagedTransactionResponse`**:
```java
record PagedTransactionResponse(
    List<FinancialTransactionSummaryDTO> content,
    int page, int size, long totalElements, int totalPages,
    BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netBalance) {}
// totalIncome/Expenses/netBalance computed over ALL pages matching filter (not just current page)
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
    String counterpartyName,
    @Size(max=50) String counterpartyAccount,
    Long bankAccountId,
    Long subcategoryId,
    Long leaseId,
    Long housingUnitId,
    Long buildingId,
    List<SaveAssetLinkRequest> assetLinks) {}
```

**`UpdateTransactionRequest`** — identical to `CreateTransactionRequest` (reference, externalReference, source excluded).

**`ConfirmTransactionRequest`**:
```java
record ConfirmTransactionRequest(
    Long subcategoryId,          // null = keep current
    LocalDate accountingMonth,   // null = keep current
    Long leaseId,                // null = keep current (only for INCOME)
    Long buildingId,
    Long housingUnitId) {}
```

**`ConfirmBatchRequest`**:
```java
record ConfirmBatchRequest(@NotNull Long batchId) {}
```

**`ImportBatchResultDTO`**:
```java
record ImportBatchResultDTO(
    Long batchId, int totalRows, int importedCount,
    int duplicateCount, int errorCount,
    List<ImportRowErrorDTO> errors) {}

record ImportRowErrorDTO(int rowNumber, String rawLine, String errorMessage) {}
```

**`TransactionStatisticsDTO`**:
```java
record TransactionStatisticsDTO(
    BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netBalance,
    List<CategoryBreakdownDTO> byCategory,
    List<BuildingBreakdownDTO> byBuilding,
    List<UnitBreakdownDTO> byUnit,
    List<BankAccountBreakdownDTO> byBankAccount,
    List<MonthlyTrendDTO> monthlyTrend) {}

record CategoryBreakdownDTO(
    Long categoryId, String categoryName,
    List<SubcategoryBreakdownDTO> subcategories,
    BigDecimal categoryTotal) {}

record SubcategoryBreakdownDTO(
    Long subcategoryId, String subcategoryName, SubcategoryDirection direction,
    BigDecimal amount, long transactionCount, double percentage) {}

record BuildingBreakdownDTO(
    Long buildingId, String buildingName,
    BigDecimal income, BigDecimal expenses, BigDecimal balance) {}

record UnitBreakdownDTO(
    Long unitId, String unitNumber, String buildingName,
    BigDecimal income, BigDecimal expenses, BigDecimal balance) {}

record BankAccountBreakdownDTO(
    Long bankAccountId, String label, BankAccountType type,
    BigDecimal income, BigDecimal expenses, BigDecimal balance) {}

record MonthlyTrendDTO(int year, int month, BigDecimal income, BigDecimal expenses) {}
```

**`AccountingMonthSuggestionDTO`**:
```java
record AccountingMonthSuggestionDTO(LocalDate accountingMonth, int confidence) {}
```

**`SubcategorySuggestionDTO`**:
```java
record SubcategorySuggestionDTO(Long subcategoryId, String subcategoryName,
    Long categoryId, String categoryName, int confidence) {}
```

---

### Repositories

**`BankAccountRepository`** extends `JpaRepository<BankAccount, Long>`:
```java
List<BankAccount> findAllByOrderByLabelAsc();
List<BankAccount> findByIsActiveTrueOrderByLabelAsc();
boolean existsByLabelIgnoreCase(String label);
boolean existsByLabelIgnoreCaseAndIdNot(String label, Long id);
boolean existsByAccountNumber(String accountNumber);
boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);
Optional<BankAccount> findByAccountNumber(String accountNumber);
```

**`TagCategoryRepository`** extends `JpaRepository<TagCategory, Long>`:
```java
List<TagCategory> findAllByOrderByNameAsc();
boolean existsByNameIgnoreCase(String name);
boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
```

**`TagSubcategoryRepository`** extends `JpaRepository<TagSubcategory, Long>`:
```java
List<TagSubcategory> findByCategoryIdOrderByNameAsc(Long categoryId);
List<TagSubcategory> findAllByOrderByCategoryNameAscNameAsc();
boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

@Query("SELECT COUNT(t) FROM FinancialTransaction t WHERE t.subcategory.id = :id")
long countUsage(@Param("id") Long id);
```

**`ImportBatchRepository`** extends `JpaRepository<ImportBatch, Long>`.

**`FinancialTransactionRepository`** extends `JpaRepository<FinancialTransaction, Long>`, `JpaSpecificationExecutor<FinancialTransaction>`:
```java
// Deduplication
boolean existsByExternalReferenceAndTransactionDateAndAmount(
    String externalReference, LocalDate transactionDate, BigDecimal amount);

// Totals for summary bar (over full filter, not just current page)
@Query("""
    SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t
    WHERE t.direction = :dir
    AND t.status IN ('CONFIRMED', 'RECONCILED')
    AND (:spec) -- use Specification for other filters
    """)
// → implement via two separate aggregate queries in service using Specification + CriteriaQuery

// Reference sequence
@Query(value = """
    SELECT COALESCE(MAX(CAST(SUBSTRING(reference FROM 10) AS INTEGER)), 0) + 1
    FROM financial_transaction
    WHERE reference LIKE :prefix
    """, nativeQuery = true)
int nextSequenceForYear(@Param("prefix") String prefix);

// Batch review
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
// For suggestion lookup — ordered by confidence DESC
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

**`AccountingMonthRuleRepository`** extends `JpaRepository<AccountingMonthRule, Long>`:
```java
// Specific rule (subcategory + counterparty) has priority over generic (counterparty IS NULL)
@Query("""
    SELECT r FROM AccountingMonthRule r
    WHERE r.subcategory.id = :subcategoryId
    AND (LOWER(r.counterpartyAccount) = LOWER(:counterparty) OR r.counterpartyAccount IS NULL)
    ORDER BY CASE WHEN r.counterpartyAccount IS NULL THEN 1 ELSE 0 END ASC
    """)
List<AccountingMonthRule> findBestMatch(
    @Param("subcategoryId") Long subcategoryId,
    @Param("counterparty") String counterparty);

Optional<AccountingMonthRule> findBySubcategoryIdAndCounterpartyAccountIgnoreCase(
    Long subcategoryId, String counterpartyAccount);

Optional<AccountingMonthRule> findBySubcategoryIdAndCounterpartyAccountIsNull(Long subcategoryId);
```

**`TransactionSpecification`** — static factory methods for `Specification<FinancialTransaction>`:
```java
public class TransactionSpecification {
    public static Specification<FinancialTransaction> withDirection(TransactionDirection d) { ... }
    public static Specification<FinancialTransaction> withDateFrom(LocalDate from) { ... }
    public static Specification<FinancialTransaction> withDateTo(LocalDate to) { ... }
    public static Specification<FinancialTransaction> withAccountingFrom(LocalDate from) { ... }
    public static Specification<FinancialTransaction> withAccountingTo(LocalDate to) { ... }
    public static Specification<FinancialTransaction> withCategoryId(Long id) { ... }
    public static Specification<FinancialTransaction> withSubcategoryId(Long id) { ... }
    public static Specification<FinancialTransaction> withBankAccountId(Long id) { ... }
    public static Specification<FinancialTransaction> withBuildingId(Long id) { ... }
    public static Specification<FinancialTransaction> withUnitId(Long id) { ... }
    public static Specification<FinancialTransaction> withStatus(TransactionStatus s) { ... }
    public static Specification<FinancialTransaction> withImportBatchId(Long id) { ... }
    public static Specification<FinancialTransaction> withAssetLink(AssetType type, Long assetId) {
        // JOIN to transaction_asset_link
    }
    public static Specification<FinancialTransaction> withSearch(String search) {
        // ILIKE on reference, description, counterpartyName, counterpartyAccount
    }
    public static Specification<FinancialTransaction> confirmedOrReconciled() {
        // status IN (CONFIRMED, RECONCILED) — for statistics
    }
}
```

---

### Services

**`BankAccountService`** (`@Service`, `@Transactional(readOnly=true)`):
- `getAll(boolean activeOnly)` → `List<BankAccountDTO>`
- `@Transactional create(SaveBankAccountRequest)` → validate unique label + account number; create
- `@Transactional update(Long id, SaveBankAccountRequest)` → validate unique excluding self; update
- No delete — deactivation only via `update`

**`TagCategoryService`** (`@Service`, `@Transactional(readOnly=true)`):
- `getAll()` → `List<TagCategoryDTO>` (with subcategoryCount)
- `@Transactional create(SaveTagCategoryRequest)` → unique name check
- `@Transactional update(Long id, SaveTagCategoryRequest)` → unique name excluding self
- `@Transactional delete(Long id)` → blocked if subcategories exist → throw `CategoryHasSubcategoriesException`

**`TagSubcategoryService`** (`@Service`, `@Transactional(readOnly=true)`):
- `getAll(Long categoryId)` → `List<TagSubcategoryDTO>` (categoryId optional filter; includes usageCount)
- `@Transactional create(SaveTagSubcategoryRequest)` → unique name within category
- `@Transactional update(Long id, SaveTagSubcategoryRequest)` → unique name excluding self; check direction change safety (BR-US086-AC9)
- `@Transactional delete(Long id)` → blocked if usageCount > 0 → throw `SubcategoryInUseException`

**`LearningService`** (`@Service`, `@Transactional(readOnly=true)`):

```java
// Suggest subcategory based on counterparty/description patterns
List<SubcategorySuggestionDTO> suggestSubcategory(
    String counterpartyAccount, String counterpartyName,
    String description, TransactionDirection direction, int minConfidence);
// → queries tag_learning_rule for each non-null field in priority order:
//   COUNTERPARTY_ACCOUNT first (most specific), then COUNTERPARTY_NAME, then DESCRIPTION
// → filters results to subcategories compatible with direction
// → deduplicates by subcategoryId (keep highest confidence), returns sorted DESC

// Suggest accounting month offset
AccountingMonthSuggestionDTO suggestAccountingMonth(Long subcategoryId, String counterpartyAccount);
// → queries accounting_month_rule with priority logic (specific > generic)
// → returns proposed LocalDate (= first day of month after applying offset to today)
//   and the matched confidence; confidence=0 if no rule found (default offset=0)

@Transactional
void reinforceTagRule(Long subcategoryId, String counterpartyAccount);
// → find or create TagLearningRule for (COUNTERPARTY_ACCOUNT, counterpartyAccount, subcategoryId)
// → confidence += 1, lastMatchedAt = now

@Transactional
void reinforceAccountingMonthRule(Long subcategoryId, String counterpartyAccount, int offset);
// → find or create AccountingMonthRule for (subcategoryId, counterpartyAccount)
//   if counterpartyAccount null/blank, use (subcategoryId, NULL) — generic rule
// → confidence += 1, lastMatchedAt = now
```

**`CsvImportService`** (`@Service`):

```java
// Step 1: Parse and preview (no DB writes)
List<ParsedCsvRow> parsePreview(MultipartFile file, CsvMappingConfig config);
// → reads platform_config for mapping; parses all rows; collects per-row errors
// → ParsedCsvRow: rowNumber, rawLine, date, amount, direction, description,
//                 counterpartyName, counterpartyAccount, externalReference,
//                 bankAccountIban, valueDate, parseError (nullable)

// Step 2: Import (DB writes)
@Transactional
ImportBatchResultDTO importBatch(List<ParsedCsvRow> rows, AppUser currentUser);
// Algorithm:
// 1. Create ImportBatch record
// 2. For each valid (no parseError) row:
//    a. Dedup check: if externalReference non-blank AND existsByExternalReferenceAndTransactionDateAndAmount → skip (duplicateCount++)
//    b. Resolve bankAccountId from IBAN lookup (BankAccountRepository.findByAccountNumber)
//    c. Call LearningService.suggestSubcategory() → store best match as subcategory_id if confidence ≥ threshold
//    d. Call LearningService.suggestAccountingMonth(subcategoryId, counterpartyAccount) → set accounting_month
//       (default: first day of transaction_date month if no rule or subcategory)
//    e. If counterpartyAccount matches lease tenant → set suggested_lease_id (query Person by bank_account, find active lease)
//    f. Create FinancialTransaction: status=DRAFT, source=CSV_IMPORT, importBatchId=batch.id
//    g. importedCount++
// 3. Update batch counts; return ImportBatchResultDTO

CsvMappingConfig loadMappingConfig();
// → reads all csv.import.* keys from PlatformConfigService, returns typed config record
```

**`FinancialTransactionService`** (`@Service`, `@Transactional(readOnly=true)`):

```java
PagedTransactionResponse getAll(TransactionFilter filter, Pageable pageable);
// → builds Specification from filter; executes paged query
// → computes totalIncome + totalExpenses with two separate aggregate queries over full filter
// → BR-UC014-11: all stats exclude DRAFT and CANCELLED for totals? No — totals in list include all statuses
//   (only statistics dashboard excludes DRAFT)

FinancialTransactionDTO getById(Long id);

@Transactional
FinancialTransactionDTO create(CreateTransactionRequest req, AppUser currentUser);
// → validate BR-UC014-02: direction=EXPENSE && leaseId!=null → throw TransactionValidationException
// → validate BR-UC014-03: unitId!=null → load unit, set buildingId from unit.building.id
// → validate subcategory direction compatibility (BR-UC014-06)
// → validate asset links (BR-UC014-09) for each BOILER link
// → generate reference: "TXN-" + year + "-" + String.format("%05d", nextSequence)
// → accountingMonth: truncate to first of month
// → status = CONFIRMED, source = MANUAL
// → after save: reinforce learning rules if counterpartyAccount non-blank and subcategoryId set

@Transactional
FinancialTransactionDTO update(Long id, UpdateTransactionRequest req);
// → check status != RECONCILED → throw TransactionNotEditableException
// → same validations as create
// → reinforce learning rules after save

@Transactional
void delete(Long id);
// → check status != RECONCILED → throw TransactionNotEditableException

@Transactional
FinancialTransactionDTO confirm(Long id, ConfirmTransactionRequest req, AppUser currentUser);
// → load DRAFT transaction; apply optional overrides from req
// → set status = CONFIRMED
// → reinforce learning rules for subcategory and accounting_month if applicable

@Transactional
int confirmBatch(Long batchId);
// → findByImportBatchId where status = DRAFT
// → set all to CONFIRMED
// → reinforce learning rules for each (use subcategory and accounting_month already on the transaction)
// → return count

TransactionStatisticsDTO getStatistics(StatisticsFilter filter);
// → all aggregations over status IN (CONFIRMED, RECONCILED) only
// → byCategory: group by category then subcategory, compute amount sum + count + percentage
// → byBuilding: group by building_id (plus "Unassigned" for null)
// → byUnit: only when filter.unitId or filter.buildingId set; group by unit_id
// → byBankAccount: group by bank_account_id (plus "Unassigned" for null)
// → monthlyTrend: group by year+month of accounting_month, two rows per month (INCOME/EXPENSE)

void exportCsv(TransactionFilter filter, HttpServletResponse response) throws IOException;
// → write UTF-8 BOM; write header row; stream rows via writer without loading all in memory
// → sign: positive for INCOME, negative for EXPENSE
// → accountingMonth formatted as YYYY-MM

private String resolveAssetLabel(AssetType type, Long assetId);
// → BOILER: BoilerRepository.findById → brand + " " + model
// → FIRE_EXTINGUISHER: FireExtinguisherRepository.findById → identificationNumber
// → METER: MeterRepository.findById → meterNumber + " (" + type + ")"
```

**Business rules enforced in `FinancialTransactionService`**:

| Rule | Implementation |
|---|---|
| BR-UC014-01 | `@Positive` on DTO + DB CHECK constraint |
| BR-UC014-02 | direction=EXPENSE + leaseId!=null → `TransactionValidationException("Lease link is only allowed for income transactions")` |
| BR-UC014-03 | unitId set → load HousingUnit → set buildingId = unit.building.id (overwrites any buildingId in request) |
| BR-UC014-04 | status=RECONCILED → `TransactionNotEditableException` on update/delete/confirm |
| BR-UC014-05 | Dedup in CsvImportService |
| BR-UC014-06 | subcategoryId set → load subcategory → if subcategory.direction=INCOME and transaction.direction=EXPENSE (or vice versa) → `SubcategoryDirectionMismatchException` |
| BR-UC014-08 | Accounting month rule priority in LearningService.suggestAccountingMonth() |
| BR-UC014-09 | For each BOILER asset link: load boiler → verify boiler.housingUnit.building.id == transaction.building.id |
| BR-UC014-11 | accountingMonth = req.accountingMonth().withDayOfMonth(1) |

---

### Exceptions → add all to `GlobalExceptionHandler`

| Exception | HTTP | Message |
|---|---|---|
| `TransactionNotFoundException` | 404 | `"Financial transaction not found: " + id` |
| `TransactionNotEditableException` | 422 | `"Reconciled transactions cannot be modified"` |
| `TransactionValidationException` | 400 | (message from constructor) |
| `SubcategoryDirectionMismatchException` | 400 | `"Subcategory '[name]' is not compatible with direction [direction]"` |
| `SubcategoryInUseException` | 409 | `"This subcategory is used on [N] transaction(s) and cannot be deleted."` |
| `SubcategoryNotFoundException` | 404 | `"Subcategory not found: " + id` |
| `CategoryNotFoundException` | 404 | `"Category not found: " + id` |
| `CategoryHasSubcategoriesException` | 409 | `"This category contains [N] subcategory/subcategories. Delete them first."` |
| `BankAccountNotFoundException` | 404 | `"Bank account not found: " + id` |
| `BankAccountDuplicateLabelException` | 409 | `"A bank account with this label already exists."` |
| `BankAccountDuplicateNumberException` | 409 | `"This IBAN is already registered."` |
| `ImportBatchNotFoundException` | 404 | `"Import batch not found: " + id` |
| `AssetLinkValidationException` | 400 | (message from constructor) |

---

### Controllers

**`TransactionController`** — no `@RequestMapping` prefix:

| Method | Path | Body / Params | Response | Story |
|--------|------|---------------|----------|-------|
| GET | `/api/v1/transactions` | `direction`, `from`, `to`, `accountingFrom`, `accountingTo`, `categoryId`, `subcategoryId`, `bankAccountId`, `buildingId`, `unitId`, `status`, `search`, `importBatchId`, `assetType`, `assetId`, `page`, `size`, `sort` | `PagedTransactionResponse` 200 | US078 |
| GET | `/api/v1/transactions/{id}` | — | `FinancialTransactionDTO` 200 | US078 |
| POST | `/api/v1/transactions` | `CreateTransactionRequest` | `FinancialTransactionDTO` 201 | US079 |
| PUT | `/api/v1/transactions/{id}` | `UpdateTransactionRequest` | `FinancialTransactionDTO` 200 | US080 |
| DELETE | `/api/v1/transactions/{id}` | — | 204 | US081 |
| PATCH | `/api/v1/transactions/{id}/confirm` | `ConfirmTransactionRequest` | `FinancialTransactionDTO` 200 | US085 |
| POST | `/api/v1/transactions/confirm-batch` | `ConfirmBatchRequest` | `{ "confirmedCount": N }` 200 | US085 |
| GET | `/api/v1/transactions/statistics` | `accountingFrom`, `accountingTo`, `buildingId`, `unitId`, `bankAccountId`, `direction` | `TransactionStatisticsDTO` 200 | US088 |
| GET | `/api/v1/transactions/export` | same as list, no pagination | `text/csv` stream | US089 |

**`TransactionImportController`** — no `@RequestMapping` prefix:

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| POST | `/api/v1/transactions/import` | `MultipartFile file` (multipart/form-data) | `ImportBatchResultDTO` 200 | US084 |
| GET | `/api/v1/transactions/import/{batchId}` | `page`, `size` query params | `PagedTransactionResponse` 200 | US085 |

**`TagCategoryController`** — no `@RequestMapping` prefix:

| Method | Path | Body | Response | Story |
|--------|------|------|----------|-------|
| GET | `/api/v1/tag-categories` | — | `List<TagCategoryDTO>` 200 | US086 |
| POST | `/api/v1/tag-categories` | `SaveTagCategoryRequest` | `TagCategoryDTO` 201 | US086 |
| PUT | `/api/v1/tag-categories/{id}` | `SaveTagCategoryRequest` | `TagCategoryDTO` 200 | US086 |
| DELETE | `/api/v1/tag-categories/{id}` | — | 204 | US086 |

**`TagSubcategoryController`** — no `@RequestMapping` prefix:

| Method | Path | Body / Params | Response | Story |
|--------|------|---------------|----------|-------|
| GET | `/api/v1/tag-subcategories` | `categoryId` (optional), `direction` (optional) | `List<TagSubcategoryDTO>` 200 | US086 |
| POST | `/api/v1/tag-subcategories` | `SaveTagSubcategoryRequest` | `TagSubcategoryDTO` 201 | US086 |
| PUT | `/api/v1/tag-subcategories/{id}` | `SaveTagSubcategoryRequest` | `TagSubcategoryDTO` 200 | US086 |
| DELETE | `/api/v1/tag-subcategories/{id}` | — | 204 | US086 |

**`BankAccountController`** — no `@RequestMapping` prefix:

| Method | Path | Body / Params | Response | Story |
|--------|------|---------------|----------|-------|
| GET | `/api/v1/bank-accounts` | `activeOnly` (boolean, default false) | `List<BankAccountDTO>` 200 | US087 |
| POST | `/api/v1/bank-accounts` | `SaveBankAccountRequest` | `BankAccountDTO` 201 | US087 |
| PUT | `/api/v1/bank-accounts/{id}` | `SaveBankAccountRequest` | `BankAccountDTO` 200 | US087 |

**Additional endpoint on existing controllers** (modify, do not regenerate):

Add to `BoilerController`:
```
GET /api/v1/boilers/{boilerId}/transaction-count → long
```
Add to `FireExtinguisherController`:
```
GET /api/v1/fire-extinguishers/{id}/transaction-count → long
```
Both implemented via `TransactionAssetLinkRepository.countByAssetTypeAndAssetId(...)`.

---

## FRONTEND

### Models — `transaction.model.ts`

```typescript
export type TransactionDirection  = 'INCOME' | 'EXPENSE';
export type TransactionStatus     = 'DRAFT' | 'CONFIRMED' | 'RECONCILED';
export type TransactionSource     = 'MANUAL' | 'CSV_IMPORT';
export type AssetType             = 'BOILER' | 'FIRE_EXTINGUISHER' | 'METER';
export type SubcategoryDirection  = 'INCOME' | 'EXPENSE' | 'BOTH';
export type BankAccountType       = 'CURRENT' | 'SAVINGS';

export interface BankAccount {
  id: number; label: string; accountNumber: string;
  type: BankAccountType; isActive: boolean;
}

export interface TagCategory {
  id: number; name: string; description?: string; subcategoryCount: number;
}

export interface TagSubcategory {
  id: number; categoryId: number; categoryName: string;
  name: string; direction: SubcategoryDirection; description?: string; usageCount: number;
}

export interface TransactionAssetLink {
  id?: number; assetType: AssetType; assetId: number;
  assetLabel?: string; notes?: string;
}

export interface FinancialTransactionSummary {
  id: number; reference: string;
  transactionDate: string; accountingMonth: string;  // 'YYYY-MM-DD'
  direction: TransactionDirection; amount: number;
  counterpartyName?: string; status: TransactionStatus; source: TransactionSource;
  bankAccountLabel?: string; categoryName?: string; subcategoryName?: string;
  buildingName?: string; unitNumber?: string; leaseReference?: string;
}

export interface FinancialTransaction extends FinancialTransactionSummary {
  externalReference?: string; valueDate?: string;
  description?: string; counterpartyAccount?: string;
  bankAccountId?: number;
  subcategoryId?: number; categoryId?: number;
  leaseId?: number; suggestedLeaseId?: number; suggestedLeaseReference?: string;
  housingUnitId?: number; buildingId?: number; importBatchId?: number;
  assetLinks: TransactionAssetLink[];
  editable: boolean;
  createdAt: string; updatedAt: string;
}

export interface PagedTransactionResponse {
  content: FinancialTransactionSummary[];
  page: number; size: number; totalElements: number; totalPages: number;
  totalIncome: number; totalExpenses: number; netBalance: number;
}

export interface TransactionStatistics {
  totalIncome: number; totalExpenses: number; netBalance: number;
  byCategory: CategoryBreakdown[];
  byBuilding: BuildingBreakdown[];
  byUnit: UnitBreakdown[];
  byBankAccount: BankAccountBreakdown[];
  monthlyTrend: MonthlyTrend[];
}

export interface CategoryBreakdown {
  categoryId: number; categoryName: string;
  subcategories: SubcategoryBreakdown[]; categoryTotal: number;
}
export interface SubcategoryBreakdown {
  subcategoryId: number; subcategoryName: string; direction: SubcategoryDirection;
  amount: number; transactionCount: number; percentage: number;
}
export interface BuildingBreakdown {
  buildingId?: number; buildingName: string;
  income: number; expenses: number; balance: number;
}
export interface UnitBreakdown {
  unitId?: number; unitNumber: string; buildingName: string;
  income: number; expenses: number; balance: number;
}
export interface BankAccountBreakdown {
  bankAccountId?: number; label: string; type?: BankAccountType;
  income: number; expenses: number; balance: number;
}
export interface MonthlyTrend { year: number; month: number; income: number; expenses: number; }

export interface ImportBatchResult {
  batchId: number; totalRows: number; importedCount: number;
  duplicateCount: number; errorCount: number;
  errors: { rowNumber: number; rawLine: string; errorMessage: string }[];
}

export interface SubcategorySuggestion {
  subcategoryId: number; subcategoryName: string;
  categoryId: number; categoryName: string; confidence: number;
}

// Display helpers
export const DIRECTION_LABELS: Record<TransactionDirection, string> = {
  INCOME: 'Income', EXPENSE: 'Expense'
};
export const STATUS_LABELS: Record<TransactionStatus, string> = {
  DRAFT: 'Draft', CONFIRMED: 'Confirmed', RECONCILED: 'Reconciled'
};
export const BANK_ACCOUNT_TYPE_LABELS: Record<BankAccountType, string> = {
  CURRENT: 'Current', SAVINGS: 'Savings'
};
```

### Services

**`transaction.service.ts`**:
```typescript
getTransactions(params: TransactionFilter): Observable<PagedTransactionResponse>
getById(id: number): Observable<FinancialTransaction>
create(req: CreateTransactionRequest): Observable<FinancialTransaction>
update(id: number, req: UpdateTransactionRequest): Observable<FinancialTransaction>
delete(id: number): Observable<void>
confirm(id: number, req: ConfirmTransactionRequest): Observable<FinancialTransaction>
confirmBatch(batchId: number): Observable<{ confirmedCount: number }>
getStatistics(filter: StatisticsFilter): Observable<TransactionStatistics>
exportCsv(filter: TransactionFilter): Observable<Blob>    // responseType: 'blob'
importCsv(file: File): Observable<ImportBatchResult>      // FormData
getBatch(batchId: number, page: number, size: number): Observable<PagedTransactionResponse>
```

**`tag-category.service.ts`**:
```typescript
getAll(): Observable<TagCategory[]>
create(req: SaveTagCategoryRequest): Observable<TagCategory>
update(id: number, req: SaveTagCategoryRequest): Observable<TagCategory>
delete(id: number): Observable<void>
```

**`tag-subcategory.service.ts`**:
```typescript
getAll(categoryId?: number, direction?: SubcategoryDirection): Observable<TagSubcategory[]>
create(req: SaveTagSubcategoryRequest): Observable<TagSubcategory>
update(id: number, req: SaveTagSubcategoryRequest): Observable<TagSubcategory>
delete(id: number): Observable<void>
```

**`bank-account.service.ts`**:
```typescript
getAll(activeOnly?: boolean): Observable<BankAccount[]>
create(req: SaveBankAccountRequest): Observable<BankAccount>
update(id: number, req: SaveBankAccountRequest): Observable<BankAccount>
```

### Components (all standalone)

#### `TransactionsPageComponent` — route `/transactions`
Shell with 4 tabs driven by `?tab=` query param: **List** · **Import** · **Dashboard** · **Settings**.

#### `TransactionListComponent` — tab "List"
- Filter panel (collapsible): direction toggle, execution date range, accounting month range, category, subcategory (filtered), bank account, building, status multi-select, free-text search.
- Active filters as removable chips + "Clear all" button.
- Summary bar: Total Income (green) | Total Expenses (red) | Net Balance.
- Paginated table: reference, date, accounting month (MM/YYYY), direction badge, amount, counterparty, category + subcategory, bank account, status badge, actions (edit icon, delete icon).
- Row click → navigate `/transactions/{id}`.
- "New Transaction" button → `/transactions/new`.
- "Export CSV" button → calls service, triggers download.

#### `TransactionFormComponent` — routes `/transactions/new` and `/transactions/{id}/edit`
Reactive form:
1. **General**: direction radio*, execution date*, accounting month picker*, amount*, description
2. **Counterparty**: name, account number (triggers accounting_month suggestion when subcategory also set), bank account dropdown
3. **Classification**: category dropdown → subcategory dropdown (filtered to category + direction). On subcategory or counterparty_account change → call backend suggestion endpoint for accounting_month; pre-fill if not manually overridden.
4. **Links**: building → unit (cascading); lease (INCOME only)
5. **Assets** (EXPENSE only): `AssetLinkEditorComponent`

On save: POST (new) or PUT (edit). Show reference read-only on edit.
Cancel: guard if dirty.

#### `TransactionDetailComponent` — route `/transactions/{id}`
Full read-only view. Status badge. "Edit" button (hidden if not editable). "Delete" button (hidden if not editable). All fields displayed in sections matching the form. Asset links displayed as a list with type badge + label.

#### `TransactionImportComponent` — tab "Import"
1. Drag-and-drop upload zone.
2. After file selection: preview table (parsed rows, errors in red, direction toggle per row, bank account per row).
3. "Process Import" button → calls `importCsv()` → shows result banner.
4. "Review imported transactions →" link navigates to `/transactions/import/{batchId}`.

#### `TransactionReviewComponent` — route `/transactions/import/:batchId`
- Page header: filename, import date, progress "X / Y confirmed".
- Table per DRAFT transaction: date, accounting_month (editable month picker), direction, amount, counterparty, category+subcategory (dropdowns, suggestion shown with ✨ icon + confidence label), bank account, building+unit+lease (optional), status.
- "Confirm" button per row → PATCH; reinforces learning rules.
- "Reject" button per row → sets status CANCELLED; greys out row.
- "Confirm All" button with count.
- Collapsed "Skipped duplicates (N)" panel at bottom.

#### `TransactionDashboardComponent` — tab "Dashboard"
- Filter bar: accounting month range, building, unit (depends on building), bank account, direction.
- Summary cards row.
- "By Category / Subcategory": grouped table (bold category rows + subtotal, indented subcategory rows) + horizontal bar chart.
- "By Building": table.
- "By Unit": table (visible only when building selected).
- "By Bank Account": table.
- "Monthly Trend": line chart (two lines: income / expenses) over selected accounting months.
- "Export CSV" button.

For charts, use the charting library already used in the project (ng2-charts or equivalent). If none exists, use a simple SVG-based approach.

#### `TransactionSettingsComponent` — tab "Settings"
Two sub-sections side by side (or stacked on mobile):

**Categories & Subcategories sub-section**:
- Categories table: name, description, subcategory count, Edit/Delete actions.
- "Add Category" → inline form row.
- Subcategories table below (or filterable by category): name, category badge, direction badge, usage count, Edit/Delete.
- "Add Subcategory" → inline form row.
- Delete blocked with inline error if constraints not met.

**Bank Accounts sub-section**:
- Table: label, IBAN, type badge, active toggle, Edit action. No delete.
- "Add Bank Account" → inline form row.

#### `AssetLinkEditorComponent` (standalone, reusable)
Inputs: `[buildingId]: number | null`, `[unitId]: number | null`, `[links]: TransactionAssetLink[]`.
Output: `(linksChanged): TransactionAssetLink[]`.
- Repeatable rows: asset type selector → asset picker (API call filtered to context) → notes.
- "Add" button. "×" remove per row (no confirmation).
- Asset picker disabled with hint if no building/unit set.

---

## MODIFICATIONS TO EXISTING COMPONENTS

### `building-details.component`
Add a collapsible "Financial" section at the bottom (before the housing units list):
- Shows: total income, total expenses for this building (quick summary via `GET /api/v1/transactions/statistics?buildingId=X`)
- Link: "View all transactions →" navigates to `/transactions?tab=list&buildingId=X`

### `housing-unit-details.component`
Same pattern: collapsible "Financial" section with unit-level summary and link to filtered list.

### `boiler-section.component`
Add a small badge: "Related expenses: N" computed from `GET /api/v1/boilers/{id}/transaction-count`. Clicking navigates to `/transactions?tab=list&assetType=BOILER&assetId={id}`.

### `fire-extinguisher-section.component`
Same pattern using `GET /api/v1/fire-extinguishers/{id}/transaction-count`.

### Sidebar navigation
Add **Transactions** as a top-level nav item (after Leases, before Administration).
Route: `/transactions`.

---

## SUMMARY OF FILES TO CREATE / MODIFY

### New backend files

| Path | Description |
|---|---|
| `backend/.../db/migration/V013__financial_transactions.sql` | Flyway migration |
| `backend/.../model/entity/BankAccount.java` | |
| `backend/.../model/entity/TagCategory.java` | |
| `backend/.../model/entity/TagSubcategory.java` | |
| `backend/.../model/entity/ImportBatch.java` | |
| `backend/.../model/entity/FinancialTransaction.java` | |
| `backend/.../model/entity/TransactionAssetLink.java` | |
| `backend/.../model/entity/TagLearningRule.java` | |
| `backend/.../model/entity/AccountingMonthRule.java` | |
| `backend/.../model/dto/BankAccountDTO.java` | |
| `backend/.../model/dto/SaveBankAccountRequest.java` | |
| `backend/.../model/dto/TagCategoryDTO.java` | |
| `backend/.../model/dto/TagSubcategoryDTO.java` | |
| `backend/.../model/dto/SaveTagCategoryRequest.java` | |
| `backend/.../model/dto/SaveTagSubcategoryRequest.java` | |
| `backend/.../model/dto/TransactionAssetLinkDTO.java` | |
| `backend/.../model/dto/SaveAssetLinkRequest.java` | |
| `backend/.../model/dto/FinancialTransactionDTO.java` | |
| `backend/.../model/dto/FinancialTransactionSummaryDTO.java` | |
| `backend/.../model/dto/PagedTransactionResponse.java` | |
| `backend/.../model/dto/CreateTransactionRequest.java` | |
| `backend/.../model/dto/UpdateTransactionRequest.java` | |
| `backend/.../model/dto/ConfirmTransactionRequest.java` | |
| `backend/.../model/dto/ConfirmBatchRequest.java` | |
| `backend/.../model/dto/ImportBatchResultDTO.java` | |
| `backend/.../model/dto/ImportRowErrorDTO.java` | |
| `backend/.../model/dto/TransactionStatisticsDTO.java` | (+ nested records) |
| `backend/.../model/dto/SubcategorySuggestionDTO.java` | |
| `backend/.../model/dto/AccountingMonthSuggestionDTO.java` | |
| `backend/.../model/dto/CsvMappingConfig.java` | record loaded from platform_config |
| `backend/.../model/dto/ParsedCsvRow.java` | internal use in CsvImportService |
| `backend/.../repository/BankAccountRepository.java` | |
| `backend/.../repository/TagCategoryRepository.java` | |
| `backend/.../repository/TagSubcategoryRepository.java` | |
| `backend/.../repository/ImportBatchRepository.java` | |
| `backend/.../repository/FinancialTransactionRepository.java` | |
| `backend/.../repository/TransactionAssetLinkRepository.java` | |
| `backend/.../repository/TagLearningRuleRepository.java` | |
| `backend/.../repository/AccountingMonthRuleRepository.java` | |
| `backend/.../repository/spec/TransactionSpecification.java` | |
| `backend/.../exception/TransactionNotFoundException.java` | |
| `backend/.../exception/TransactionNotEditableException.java` | |
| `backend/.../exception/TransactionValidationException.java` | |
| `backend/.../exception/SubcategoryDirectionMismatchException.java` | |
| `backend/.../exception/SubcategoryInUseException.java` | |
| `backend/.../exception/SubcategoryNotFoundException.java` | |
| `backend/.../exception/CategoryNotFoundException.java` | |
| `backend/.../exception/CategoryHasSubcategoriesException.java` | |
| `backend/.../exception/BankAccountNotFoundException.java` | |
| `backend/.../exception/BankAccountDuplicateLabelException.java` | |
| `backend/.../exception/BankAccountDuplicateNumberException.java` | |
| `backend/.../exception/ImportBatchNotFoundException.java` | |
| `backend/.../exception/AssetLinkValidationException.java` | |
| `backend/.../service/BankAccountService.java` | |
| `backend/.../service/TagCategoryService.java` | |
| `backend/.../service/TagSubcategoryService.java` | |
| `backend/.../service/LearningService.java` | |
| `backend/.../service/CsvImportService.java` | |
| `backend/.../service/FinancialTransactionService.java` | |
| `backend/.../controller/TransactionController.java` | |
| `backend/.../controller/TransactionImportController.java` | |
| `backend/.../controller/TagCategoryController.java` | |
| `backend/.../controller/TagSubcategoryController.java` | |
| `backend/.../controller/BankAccountController.java` | |

### New frontend files

| Path | Description |
|---|---|
| `frontend/.../models/transaction.model.ts` | |
| `frontend/.../core/services/transaction.service.ts` | |
| `frontend/.../core/services/tag-category.service.ts` | |
| `frontend/.../core/services/tag-subcategory.service.ts` | |
| `frontend/.../core/services/bank-account.service.ts` | |
| `frontend/.../features/transactions/transactions-page.component.ts/.html/.scss` | |
| `frontend/.../features/transactions/components/transaction-list/...` | |
| `frontend/.../features/transactions/components/transaction-form/...` | |
| `frontend/.../features/transactions/components/transaction-detail/...` | |
| `frontend/.../features/transactions/components/transaction-import/...` | |
| `frontend/.../features/transactions/components/transaction-review/...` | |
| `frontend/.../features/transactions/components/transaction-dashboard/...` | |
| `frontend/.../features/transactions/components/transaction-settings/...` | |
| `frontend/.../features/transactions/components/asset-link-editor/...` | |

### Modified files

| Path | Change |
|---|---|
| `backend/.../exception/GlobalExceptionHandler.java` | Add 13 exception handlers |
| `backend/.../controller/BoilerController.java` | Add `GET /api/v1/boilers/{id}/transaction-count` |
| `backend/.../controller/FireExtinguisherController.java` | Add `GET /api/v1/fire-extinguishers/{id}/transaction-count` |
| `frontend/src/app/app.routes.ts` (or equivalent) | Add `/transactions/**` routes |
| `frontend/.../core/layout/sidebar/sidebar.component.html` | Add Transactions nav item |
| `frontend/.../features/building/components/building-details/...` | Add Financial section |
| `frontend/.../features/housing-unit/components/housing-unit-details/...` | Add Financial section |
| `frontend/.../features/building/components/boiler-section/...` | Add transaction count badge |
| `frontend/.../features/building/components/fire-extinguisher-section/...` | Add transaction count badge |

---

## CONSTRAINTS

- Do **not** create V014 — all schema is in V013.
- Do **not** modify V001, V002, or V012.
- SCSS: minimal per-component styles; rely on global utility classes.
- `accounting_month` always stored and queried as first day of month.
- Statistics always exclude DRAFT and CANCELLED transactions.
- CSV export streams via `HttpServletResponse` output stream — do not load all rows in memory.
- `suggested_lease_id` is separate from `lease_id`: it holds the auto-suggested lease pending admin confirmation. Confirming a transaction with a suggested lease in the review UI copies `suggested_lease_id` → `lease_id` and clears `suggested_lease_id`.
- No `@author`, no `@version`, no generation timestamp.
- Backend package: `com.immocare` — follow existing structure.

---

## ACCEPTANCE CRITERIA SUMMARY

- [ ] Transaction list with all filters, summary bar, and pagination functional (US078)
- [ ] Manual create: all validations enforced, reference auto-generated, learning rules reinforced (US079)
- [ ] Edit blocked on RECONCILED transactions (US080)
- [ ] Delete blocked on RECONCILED transactions (US081)
- [ ] Subcategory direction compatibility enforced front and back (US082)
- [ ] Asset links scoped to building context, BOILER ownership validated (US083)
- [ ] CSV import: parse → preview → dedup → create DRAFT → suggestions applied (US084)
- [ ] Review: confirm per row or bulk, learning rules reinforced, duplicates in audit panel (US085)
- [ ] Tag catalog: 2-level hierarchy seeded, constraints on delete enforced (US086)
- [ ] Bank account catalog: label + IBAN unique, deactivation only (US087)
- [ ] Statistics dashboard driven by accounting_month, all breakdowns correct (US088)
- [ ] CSV export streams with correct sign, UTF-8 BOM, accounting_month as YYYY-MM (US089)
- [ ] All US078–US089 acceptance criteria verified

**Last Updated**: 2026-03-05
**Branch**: develop
**Status**: 📋 Ready for Implementation
