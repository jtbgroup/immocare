# ImmoCare — UC004_ESTATE_PLACEHOLDER Manage Estates — Phase 4 Implementation Prompt

I want to implement UC004_ESTATE_PLACEHOLDER - Manage Estates (Phase 4 of 6) for ImmoCare.

## CONTEXT

- **Stack**: Spring Boot 3.x + Angular 19+ + PostgreSQL — API-First
- **Branch**: `develop`
- **Prerequisite**: Phase 3 (V019) must be fully deployed and tested before starting this phase.
- **Flyway**: last migration is V019. Use **V020** for this phase.
- **Backend package**: `com.immocare` — follow existing package structure
- **No `@author`, no `@version`, no generation timestamp in any file**

## PHASE CONTEXT

This is **Phase 4 of 6** of the multi-tenant estate migration.

| Phase | Flyway | Scope |
|---|---|---|
| Phase 1 | V017 | ✅ Done — Estate CRUD, membership, `app_user` migration |
| Phase 2 | V018 | ✅ Done — `estate_id` on `building`; Buildings & Housing Units scoped |
| Phase 3 | V019 | ✅ Done — `estate_id` on `person`; Persons & Leases scoped |
| **Phase 4 (this prompt)** | V020 | `estate_id` on `financial_transaction`, `bank_account`, `tag_category`; Financial scoped |
| Phase 5 | V021 | `estate_id` on config tables; per-estate config seeded at estate creation |
| Phase 6 | — | Dashboard enriched; VIEWER enforcement; cross-estate integration tests |

## WHAT CHANGES IN THIS PHASE

- `estate_id UUID NOT NULL` added to `financial_transaction`, `bank_account`, `tag_category`
- `tag_subcategory` scoped via `tag_category.estate_id` — no new column
- `import_batch` scoped via `financial_transaction.estate_id` — no new column
- `tag_learning_rule` scoped via `tag_subcategory → tag_category.estate_id` — no new column
- `accounting_month_rule` scoped via same chain — no new column
- `FinancialTransactionController` routes migrated to `/api/v1/estates/{estateId}/transactions/**`
- `BankAccountController` routes migrated to `/api/v1/estates/{estateId}/bank-accounts/**`
- `TagController` routes migrated to `/api/v1/estates/{estateId}/tags/**`
- `ImportController` routes migrated to `/api/v1/estates/{estateId}/import/**`
- Frontend financial services updated to use `estateId`

## WHAT DOES NOT CHANGE

- `boiler_service_validity_rule`, `platform_config` — Phase 5
- `import_parser` — global, never estate-scoped
- Phase 1, 2, 3 controllers and services — untouched

---

## DATABASE MIGRATION — `V020__estate_scope_financial.sql`

```sql
-- Use case: UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 4)

-- Add estate_id to financial_transaction
ALTER TABLE financial_transaction
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_ft_estate ON financial_transaction(estate_id);

-- Add estate_id to bank_account
ALTER TABLE bank_account
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_bank_account_estate ON bank_account(estate_id);

-- Add estate_id to tag_category
ALTER TABLE tag_category
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_tag_category_estate ON tag_category(estate_id);
```

> `tag_subcategory`, `tag_learning_rule`, `accounting_month_rule`, `import_batch`
> derive their estate scope through their parent entity — no column added.

---

## BACKEND

### Modified Entities

**`FinancialTransaction`** — add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL).

**`BankAccount`** — add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL).

**`TagCategory`** — add field: `estate` (`@ManyToOne Estate`, `@JoinColumn(name = "estate_id")`, NOT NULL).

### Modified Repository: `FinancialTransactionRepository`

Add `estateId` filter to all queries. Key additions:

```java
Page<FinancialTransaction> findAll(Specification<FinancialTransaction> spec, Pageable pageable);
// Specification must always include estate_id = :estateId as a mandatory predicate

boolean existsByEstateIdAndImportFingerprintAndTransactionDateAndAmount(
    UUID estateId, String fingerprint, LocalDate date, BigDecimal amount);

@Query(value = """
    SELECT COALESCE(MAX(CAST(SUBSTRING(reference FROM 10) AS INTEGER)), 0) + 1
    FROM financial_transaction
    WHERE reference LIKE :prefix AND estate_id = :estateId
    """, nativeQuery = true)
int nextSequenceForYear(@Param("prefix") String prefix, @Param("estateId") UUID estateId);

Page<FinancialTransaction> findByEstateIdAndImportBatchId(UUID estateId, Long batchId, Pageable pageable);

boolean existsByEstateIdAndId(UUID estateId, Long id);
```

### Modified Repository: `BankAccountRepository`

```java
List<BankAccount> findByEstateIdOrderByLabelAsc(UUID estateId);
boolean existsByEstateIdAndLabelIgnoreCase(UUID estateId, String label);
boolean existsByEstateIdAndAccountNumberIgnoreCase(UUID estateId, String accountNumber);
boolean existsByEstateIdAndLabelIgnoreCaseAndIdNot(UUID estateId, String label, Long id);
boolean existsByEstateIdAndAccountNumberIgnoreCaseAndIdNot(UUID estateId, String number, Long id);
Optional<BankAccount> findByEstateIdAndAccountNumberIgnoreCase(UUID estateId, String number);
```

### Modified Repository: `TagCategoryRepository`

```java
List<TagCategory> findByEstateIdOrderByNameAsc(UUID estateId);
boolean existsByEstateIdAndNameIgnoreCase(UUID estateId, String name);
boolean existsByEstateIdAndNameIgnoreCaseAndIdNot(UUID estateId, String name, Long id);
```

### Modified Repository: `TagSubcategoryRepository`

No new column — filter via join:

```java
@Query("SELECT s FROM TagSubcategory s WHERE s.category.estate.id = :estateId ORDER BY s.name ASC")
List<TagSubcategory> findByEstateId(@Param("estateId") UUID estateId);

@Query("SELECT s FROM TagSubcategory s WHERE s.category.estate.id = :estateId AND s.category.id = :categoryId")
List<TagSubcategory> findByCategoryAndEstate(@Param("estateId") UUID estateId,
                                              @Param("categoryId") Long categoryId);
```

### Modified Repository: `TagLearningRuleRepository`

Filter via subcategory → category join:

```java
@Query("""
    SELECT r FROM TagLearningRule r
    WHERE r.subcategory.category.estate.id = :estateId
    AND r.matchField = :field
    AND LOWER(r.matchValue) = LOWER(:value)
    AND r.confidence >= :minConf
    ORDER BY r.confidence DESC
    """)
List<TagLearningRule> findSuggestions(
    @Param("estateId") UUID estateId,
    @Param("field") TagMatchField field,
    @Param("value") String value,
    @Param("minConf") int minConf);
```

### Modified Service: `FinancialTransactionService`

All methods receive `estateId` as first parameter:

```java
PagedTransactionResponse getTransactions(UUID estateId, TransactionFilterRequest filter, Pageable pageable);
FinancialTransactionDTO getById(UUID estateId, Long id);
FinancialTransactionDTO create(UUID estateId, CreateTransactionRequest req, AppUser currentUser);
FinancialTransactionDTO update(UUID estateId, Long id, UpdateTransactionRequest req);
void delete(UUID estateId, Long id);
TransactionStatisticsDTO getStatistics(UUID estateId, TransactionStatsRequest req);
```

Reference generation must include `estateId`:
```java
// TXN-YYYY-NNNNN — sequence scoped per estate per year
String prefix = "TXN-" + year + "-";
int seq = transactionRepository.nextSequenceForYear(prefix + "%", estateId);
String reference = prefix + String.format("%05d", seq);
```

### Modified Service: `BankAccountService`

All methods receive `estateId`:
```java
List<BankAccountDTO> getAllBankAccounts(UUID estateId);
BankAccountDTO createBankAccount(UUID estateId, SaveBankAccountRequest req);
BankAccountDTO updateBankAccount(UUID estateId, Long id, SaveBankAccountRequest req);
void deleteBankAccount(UUID estateId, Long id);
```

### Modified Service: `TagService`

All methods receive `estateId`:
```java
List<TagCategoryDTO> getAllCategories(UUID estateId);
TagCategoryDTO createCategory(UUID estateId, SaveTagCategoryRequest req);
TagCategoryDTO updateCategory(UUID estateId, Long id, SaveTagCategoryRequest req);
void deleteCategory(UUID estateId, Long id);

List<TagSubcategoryDTO> getSubcategories(UUID estateId, Long categoryId);
TagSubcategoryDTO createSubcategory(UUID estateId, Long categoryId, SaveTagSubcategoryRequest req);
TagSubcategoryDTO updateSubcategory(UUID estateId, Long categoryId, Long id, SaveTagSubcategoryRequest req);
void deleteSubcategory(UUID estateId, Long categoryId, Long id);
```

### Modified Service: `LearningService`

All methods receive `estateId`:
```java
List<SubcategorySuggestionDTO> suggestSubcategory(UUID estateId, String counterpartyAccount,
    String description, String assetType, TransactionDirection direction, int minConfidence);

void reinforceTagRule(UUID estateId, Long subcategoryId, TagMatchField field, String matchValue);
void reinforceAccountingMonthRule(UUID estateId, Long subcategoryId, String counterpartyAccount, int offset);
```

### Modified Service: `CsvImportService` / `TransactionImportService`

All import methods receive `estateId`:
```java
List<ImportPreviewRowDTO> previewFile(UUID estateId, MultipartFile file, String parserCode, Long bankAccountId);
ImportBatchResultDTO importFile(UUID estateId, MultipartFile file, String parserCode,
    Long bankAccountId, List<ImportRowEnrichmentDTO> enrichments);
```

Fingerprint deduplication now scoped per estate:
```java
transactionRepository.existsByEstateIdAndImportFingerprintAndTransactionDateAndAmount(estateId, ...)
```

### Modified Controllers

**`FinancialTransactionController`** — `@RequestMapping("/api/v1/estates/{estateId}/transactions")`:

| Method | Path |
|--------|------|
| GET | `/api/v1/estates/{estateId}/transactions` |
| GET | `/api/v1/estates/{estateId}/transactions/{id}` |
| POST | `/api/v1/estates/{estateId}/transactions` |
| PUT | `/api/v1/estates/{estateId}/transactions/{id}` |
| DELETE | `/api/v1/estates/{estateId}/transactions/{id}` |
| GET | `/api/v1/estates/{estateId}/transactions/statistics` |
| GET | `/api/v1/estates/{estateId}/transactions/export` |

**`BankAccountController`** — `@RequestMapping("/api/v1/estates/{estateId}/bank-accounts")`.

**`TagController`** — `@RequestMapping("/api/v1/estates/{estateId}/tags")`.

**`ImportController`** — all import endpoints under `/api/v1/estates/{estateId}/import/**`.

All controllers: `@PreAuthorize("@security.isMemberOf(#estateId)")` at class level, `isManagerOf` on mutating methods.

---

## FRONTEND

### Modified Services

**`TransactionService`**, **`BankAccountService`** (catalog), **`TagService`**, **`ImportService`** — all inject `ActiveEstateService` and prepend `estateId` to all URLs following the same pattern as Phase 2 and 3.

Key URL patterns:
```typescript
// Transactions
GET /api/v1/estates/{estateId}/transactions
POST /api/v1/estates/{estateId}/transactions
GET /api/v1/estates/{estateId}/transactions/statistics

// Bank accounts (catalog)
GET /api/v1/estates/{estateId}/bank-accounts

// Tags
GET /api/v1/estates/{estateId}/tags/categories
GET /api/v1/estates/{estateId}/tags/categories/{categoryId}/subcategories

// Import
POST /api/v1/estates/{estateId}/import/preview
POST /api/v1/estates/{estateId}/import/confirm
```

### Routing updates

```typescript
{ path: 'estates/:estateId/transactions',
  loadChildren: () => import('./features/transaction/transaction.routes') },
```

Update all `routerLink` and `router.navigate()` in transaction, bank account, and tag components to include `estateId`.

### Transaction reference display

Transaction references (`TXN-YYYY-NNNNN`) are now scoped per estate — no visual change needed.

---

## BUSINESS RULES TO ENFORCE

| Rule | Where |
|---|---|
| Transaction must belong to the estate in the URL | Backend: `verifyTransactionBelongsToEstate()` |
| Bank account must belong to the estate in the URL | Backend: check in `BankAccountService` |
| Tag category must belong to the estate in the URL | Backend: check in `TagService` |
| Subcategory must belong to same estate as transaction | Backend: validate in `FinancialTransactionService.create/update()` |
| Import fingerprint deduplication scoped per estate | Backend: `existsByEstateIdAndImportFingerprint...` |
| Learning rules scoped per estate | Backend: `estateId` filter on all `LearningService` queries |
| Asset type mappings (platform_config) still global in this phase | Phase 5 will scope them |
| VIEWER cannot create/edit/delete transactions | Backend: `@PreAuthorize("@security.isManagerOf(#estateId)")` |

---

## WHAT NOT TO GENERATE IN THIS PHASE

- Do NOT modify `PlatformConfigController`, `BoilerServiceValidityRuleController`
- Do NOT add `estate_id` to `boiler_service_validity_rule` or `platform_config`
- Do NOT modify Phase 1, 2, or 3 controllers or services beyond what is listed above
- `import_parser` remains global — no change

---

## ACCEPTANCE CRITERIA

- [ ] V020: `estate_id` added to `financial_transaction`, `bank_account`, `tag_category` with FK and indexes
- [ ] All transaction endpoints moved to `/api/v1/estates/{estateId}/transactions/**`
- [ ] All bank account catalog endpoints moved to `/api/v1/estates/{estateId}/bank-accounts/**`
- [ ] All tag endpoints moved to `/api/v1/estates/{estateId}/tags/**`
- [ ] All import endpoints moved to `/api/v1/estates/{estateId}/import/**`
- [ ] Accessing a transaction from the wrong estate → HTTP 403
- [ ] Transaction reference sequence is scoped per estate (no collision between estates)
- [ ] Fingerprint deduplication scoped per estate (same fingerprint in two estates = two valid imports)
- [ ] Tag suggestions (learning rules) scoped per estate
- [ ] Frontend financial services use `estateId` from `ActiveEstateService`
- [ ] VIEWER cannot create, edit, or delete transactions (403 from backend)

**Last Updated:** 2026-04-12 | **Branch:** `develop` | **Status:** 📋 Ready for Implementation
