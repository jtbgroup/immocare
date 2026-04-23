package com.immocare.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.model.dto.AccountingMonthSuggestionDTO;
import com.immocare.model.dto.CsvMappingConfig;
import com.immocare.model.dto.EstatePlatformConfigDTOs;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.ParsedCsvRow;
import com.immocare.model.dto.SubcategorySuggestionDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.ImportBatch;
import com.immocare.model.entity.Lease;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.ImportBatchRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PersonBankAccountRepository;

/**
 * Legacy CSV import service.
 * UC004_ESTATE_PLACEHOLDER Phase 5: loadMappingConfig() now accepts an estateId and reads
 * all CSV import settings from the estate-scoped platform config.
 */
@Service
public class CsvImportService {

    private final FinancialTransactionRepository transactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final BankAccountRepository bankAccountRepository;
    private final LearningService learningService;
    private final PlatformConfigService platformConfigService;
    private final PersonBankAccountRepository personBankAccountRepository;
    private final LeaseRepository leaseRepository;

    public CsvImportService(FinancialTransactionRepository transactionRepository,
            ImportBatchRepository importBatchRepository,
            BankAccountRepository bankAccountRepository,
            LearningService learningService,
            PlatformConfigService platformConfigService,
            LeaseRepository leaseRepository,
            PersonBankAccountRepository personBankAccountRepository) {
        this.transactionRepository = transactionRepository;
        this.importBatchRepository = importBatchRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.learningService = learningService;
        this.platformConfigService = platformConfigService;
        this.leaseRepository = leaseRepository;
        this.personBankAccountRepository = personBankAccountRepository;
    }

    /**
     * Loads CSV mapping configuration from estate-scoped platform config.
     * UC004_ESTATE_PLACEHOLDER Phase 5: reads from estate config rather than global keys.
     */
    public CsvMappingConfig loadMappingConfig(UUID estateId) {
        return new CsvMappingConfig(
                platformConfigService.getStringValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_DELIMITER, ";"),
                platformConfigService.getStringValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_DATE_FORMAT, "dd/MM/yyyy"),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_SKIP_HEADER_ROWS, 1),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_DATE, 0),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_AMOUNT, 1),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_DESCRIPTION, 2),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_COUNTERPARTY_ACCOUNT, 3),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_EXTERNAL_REFERENCE, 4),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_BANK_ACCOUNT, 5),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_COL_VALUE_DATE, -1),
                platformConfigService.getIntValue(estateId,
                        EstatePlatformConfigDTOs.KEY_CSV_SUGGESTION_CONFIDENCE, 3));
    }

    public List<ParsedCsvRow> parsePreview(MultipartFile file, CsvMappingConfig config) {
        List<ParsedCsvRow> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(config.dateFormat());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int rowNumber = 0;
            int skipped = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (skipped < config.skipHeaderRows()) {
                    skipped++;
                    continue;
                }

                String rawLine = line;
                String[] cols = line.split(config.delimiter(), -1);

                try {
                    LocalDate transactionDate = parseDate(cols, config.colDate(), fmt);
                    BigDecimal rawAmount = parseDecimal(cols, config.colAmount());
                    TransactionDirection direction = rawAmount.compareTo(BigDecimal.ZERO) >= 0
                            ? TransactionDirection.INCOME
                            : TransactionDirection.EXPENSE;
                    BigDecimal amount = rawAmount.abs();
                    String description = safeGet(cols, config.colDescription());
                    String counterpartyAccount = safeGet(cols, config.colCounterpartyAccount());
                    String externalReference = safeGet(cols, config.colExternalReference());
                    String bankAccountIban = safeGet(cols, config.colBankAccount());
                    LocalDate valueDate = config.colValueDate() >= 0
                            ? parseDate(cols, config.colValueDate(), fmt)
                            : null;

                    rows.add(new ParsedCsvRow(rowNumber, rawLine, transactionDate, valueDate,
                            amount, direction, description, counterpartyAccount,
                            externalReference, bankAccountIban, null));
                } catch (Exception e) {
                    rows.add(new ParsedCsvRow(rowNumber, rawLine, null, null, null,
                            null, null, null, null, null, e.getMessage()));
                }
            }
        } catch (Exception e) {
            rows.add(new ParsedCsvRow(0, "", null, null, null, null, null, null, null, null,
                    "Failed to read file: " + e.getMessage()));
        }
        return rows;
    }

    @Transactional
    public ImportBatchResultDTO importBatch(UUID estateId, List<ParsedCsvRow> rows, AppUser currentUser) {
        CsvMappingConfig config = loadMappingConfig(estateId);
        int minConfidence = config.suggestionConfidenceThreshold();

        ImportBatch batch = new ImportBatch();
        batch.setFilename("import");
        batch.setTotalRows(rows.size());
        batch.setCreatedBy(currentUser);
        batch = importBatchRepository.save(batch);

        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<ImportBatchResultDTO.RowError> errors = new ArrayList<>();

        for (ParsedCsvRow row : rows) {
            if (row.parseError() != null) {
                errorCount++;
                errors.add(new ImportBatchResultDTO.RowError(row.rowNumber(), row.rawLine(), row.parseError()));
                continue;
            }

            // Deduplication — estate-scoped
            if (row.externalReference() != null && !row.externalReference().isBlank()) {
                if (transactionRepository.existsByExternalReferenceAndTransactionDateAndAmount(
                        row.externalReference(), row.transactionDate(), row.amount())) {
                    duplicateCount++;
                    continue;
                }
            }

            try {
                FinancialTransaction tx = new FinancialTransaction();
                tx.setTransactionDate(row.transactionDate());
                tx.setValueDate(row.valueDate());
                tx.setAmount(row.amount());
                tx.setDirection(row.direction());
                tx.setDescription(row.description());
                tx.setCounterpartyAccount(row.counterpartyAccount());
                tx.setExternalReference(row.externalReference());
                tx.setStatus(TransactionStatus.DRAFT);
                tx.setSource(TransactionSource.IMPORT);
                tx.setImportBatch(batch);

                // Resolve bank account (estate-scoped)
                if (row.bankAccountIban() != null && !row.bankAccountIban().isBlank()) {
                    bankAccountRepository.findByEstateIdAndAccountNumber(estateId, row.bankAccountIban())
                            .ifPresent(tx::setBankAccount);
                }

                // Suggest subcategory
                List<SubcategorySuggestionDTO> suggestions = learningService.suggestSubcategory(
                        row.counterpartyAccount(), row.description(), row.direction(), minConfidence);
                Long subcategoryId = suggestions.isEmpty() ? null : suggestions.get(0).subcategoryId();

                // Set accounting_month
                AccountingMonthSuggestionDTO monthSuggestion = learningService.suggestAccountingMonth(
                        subcategoryId, row.counterpartyAccount());
                tx.setAccountingMonth(monthSuggestion != null && monthSuggestion.confidence() > 0
                        ? monthSuggestion.accountingMonth()
                        : row.transactionDate().withDayOfMonth(1));

                // Generate reference
                String year = String.valueOf(row.transactionDate().getYear());
                long seq = transactionRepository.nextRefSequence();
                tx.setReference("TXN-" + year + "-" + String.format("%05d", seq));

                // Suggest lease via counterparty IBAN
                suggestLease(tx, row.counterpartyAccount(), row.transactionDate());

                transactionRepository.save(tx);
                importedCount++;
            } catch (Exception e) {
                errorCount++;
                errors.add(new ImportBatchResultDTO.RowError(row.rowNumber(), row.rawLine(), e.getMessage()));
            }
        }

        batch.setImportedCount(importedCount);
        batch.setDuplicateCount(duplicateCount);
        batch.setErrorCount(errorCount);
        importBatchRepository.save(batch);

        return new ImportBatchResultDTO(batch.getId(), rows.size(), importedCount,
                duplicateCount, errorCount, errors);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void suggestLease(FinancialTransaction tx, String counterpartyIban, LocalDate transactionDate) {
        if (counterpartyIban == null || counterpartyIban.isBlank()) return;

        personBankAccountRepository.findByIban(counterpartyIban).ifPresent(pba -> {
            List<Lease> leases = leaseRepository
                    .findAllByTenantPersonIdOrderByStartDateDesc(pba.getPerson().getId());
            if (leases.isEmpty()) return;

            List<Lease> covering = leases.stream()
                    .filter(l -> !transactionDate.isBefore(l.getStartDate())
                            && !transactionDate.isAfter(l.getEndDate()))
                    .toList();

            Lease best;
            if (covering.size() == 1) {
                best = covering.get(0);
            } else if (covering.size() > 1) {
                best = covering.stream()
                        .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                        .findFirst()
                        .orElse(covering.get(0));
            } else {
                best = leases.stream()
                        .min(Comparator.comparingLong(
                                l -> Math.abs(ChronoUnit.DAYS.between(l.getEndDate(), transactionDate))))
                        .orElse(leases.get(0));
            }

            tx.setSuggestedLease(best);
            if (tx.getHousingUnit() == null) tx.setHousingUnit(best.getHousingUnit());
            if (tx.getBuilding() == null && best.getHousingUnit() != null) {
                tx.setBuilding(best.getHousingUnit().getBuilding());
            }
        });
    }

    private LocalDate parseDate(String[] cols, int index, DateTimeFormatter fmt) {
        return LocalDate.parse(safeGet(cols, index).trim(), fmt);
    }

    private BigDecimal parseDecimal(String[] cols, int index) {
        return new BigDecimal(safeGet(cols, index).trim().replace(",", ".").replace(" ", ""));
    }

    private String safeGet(String[] cols, int index) {
        return (index >= 0 && index < cols.length) ? cols[index].trim() : "";
    }
}
