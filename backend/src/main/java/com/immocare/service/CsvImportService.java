package com.immocare.service;

import com.immocare.model.dto.*;
import com.immocare.model.entity.*;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private final FinancialTransactionRepository transactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final BankAccountRepository bankAccountRepository;
    private final LearningService learningService;
    private final PlatformConfigService platformConfigService;

    public CsvImportService(FinancialTransactionRepository transactionRepository,
                            ImportBatchRepository importBatchRepository,
                            BankAccountRepository bankAccountRepository,
                            LearningService learningService,
                            PlatformConfigService platformConfigService) {
        this.transactionRepository = transactionRepository;
        this.importBatchRepository = importBatchRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.learningService = learningService;
        this.platformConfigService = platformConfigService;
    }

    public CsvMappingConfig loadMappingConfig() {
        return new CsvMappingConfig(
            platformConfigService.getString("csv.import.delimiter", ";"),
            platformConfigService.getString("csv.import.date_format", "dd/MM/yyyy"),
            platformConfigService.getInt("csv.import.skip_header_rows", 1),
            platformConfigService.getInt("csv.import.col.date", 0),
            platformConfigService.getInt("csv.import.col.amount", 1),
            platformConfigService.getInt("csv.import.col.description", 2),
            platformConfigService.getInt("csv.import.col.counterparty_name", 3),
            platformConfigService.getInt("csv.import.col.counterparty_account", 4),
            platformConfigService.getInt("csv.import.col.external_reference", 5),
            platformConfigService.getInt("csv.import.col.bank_account", 6),
            platformConfigService.getInt("csv.import.col.value_date", -1),
            platformConfigService.getInt("csv.import.suggestion.confidence.threshold", 3)
        );
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
                if (skipped < config.skipHeaderRows()) { skipped++; continue; }

                String rawLine = line;
                String[] cols = line.split(config.delimiter(), -1);

                try {
                    LocalDate transactionDate = parseDate(cols, config.colDate(), fmt);
                    BigDecimal rawAmount = parseDecimal(cols, config.colAmount());
                    TransactionDirection direction = rawAmount.compareTo(BigDecimal.ZERO) >= 0
                        ? TransactionDirection.INCOME : TransactionDirection.EXPENSE;
                    BigDecimal amount = rawAmount.abs();
                    String description = safeGet(cols, config.colDescription());
                    String counterpartyName = safeGet(cols, config.colCounterpartyName());
                    String counterpartyAccount = safeGet(cols, config.colCounterpartyAccount());
                    String externalReference = safeGet(cols, config.colExternalReference());
                    String bankAccountIban = safeGet(cols, config.colBankAccount());
                    LocalDate valueDate = config.colValueDate() >= 0
                        ? parseDate(cols, config.colValueDate(), fmt) : null;

                    rows.add(new ParsedCsvRow(rowNumber, rawLine, transactionDate, valueDate,
                        amount, direction, description, counterpartyName, counterpartyAccount,
                        externalReference, bankAccountIban, null));
                } catch (Exception e) {
                    rows.add(new ParsedCsvRow(rowNumber, rawLine, null, null, null, null,
                        null, null, null, null, null, e.getMessage()));
                }
            }
        } catch (Exception e) {
            rows.add(new ParsedCsvRow(0, "", null, null, null, null, null, null, null, null, null,
                "Failed to read file: " + e.getMessage()));
        }
        return rows;
    }

    @Transactional
    public ImportBatchResultDTO importBatch(List<ParsedCsvRow> rows, AppUser currentUser) {
        CsvMappingConfig config = loadMappingConfig();
        int minConfidence = config.suggestionConfidenceThreshold();

        ImportBatch batch = new ImportBatch();
        batch.setFilename("import");
        batch.setTotalRows(rows.size());
        batch.setCreatedBy(currentUser);
        batch = importBatchRepository.save(batch);

        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<ImportRowErrorDTO> errors = new ArrayList<>();

        for (ParsedCsvRow row : rows) {
            if (row.parseError() != null) {
                errorCount++;
                errors.add(new ImportRowErrorDTO(row.rowNumber(), row.rawLine(), row.parseError()));
                continue;
            }

            // Deduplication
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
                tx.setCounterpartyName(row.counterpartyName());
                tx.setCounterpartyAccount(row.counterpartyAccount());
                tx.setExternalReference(row.externalReference());
                tx.setStatus(TransactionStatus.DRAFT);
                tx.setSource(TransactionSource.CSV_IMPORT);
                tx.setImportBatch(batch);

                // Resolve bank account
                if (row.bankAccountIban() != null && !row.bankAccountIban().isBlank()) {
                    bankAccountRepository.findByAccountNumber(row.bankAccountIban())
                        .ifPresent(tx::setBankAccount);
                }

                // Suggest subcategory
                List<SubcategorySuggestionDTO> suggestions = learningService.suggestSubcategory(
                    row.counterpartyAccount(), row.counterpartyName(),
                    row.description(), row.direction(), minConfidence);
                Long subcategoryId = null;
                if (!suggestions.isEmpty()) {
                    // SubcategoryId set via repository lookup — just store the id via a reference
                    // This will be resolved properly; we store directly
                    subcategoryId = suggestions.get(0).subcategoryId();
                    // We need a TagSubcategory proxy — use getOne pattern
                    // Set accounting month suggestion based on subcategoryId
                }

                // Set accounting_month
                AccountingMonthSuggestionDTO monthSuggestion = learningService.suggestAccountingMonth(
                    subcategoryId, row.counterpartyAccount());
                tx.setAccountingMonth(monthSuggestion != null && monthSuggestion.confidence() > 0
                    ? monthSuggestion.accountingMonth()
                    : row.transactionDate().withDayOfMonth(1));

                // Generate reference
                String year = String.valueOf(row.transactionDate().getYear());
                String prefix = "TXN-" + year + "-%";
                int seq = transactionRepository.nextSequenceForYear(prefix);
                tx.setReference("TXN-" + year + "-" + String.format("%05d", seq));

                transactionRepository.save(tx);
                importedCount++;
            } catch (Exception e) {
                errorCount++;
                errors.add(new ImportRowErrorDTO(row.rowNumber(), row.rawLine(), e.getMessage()));
            }
        }

        batch.setImportedCount(importedCount);
        batch.setDuplicateCount(duplicateCount);
        batch.setErrorCount(errorCount);
        importBatchRepository.save(batch);

        return new ImportBatchResultDTO(batch.getId(), rows.size(), importedCount,
            duplicateCount, errorCount, errors);
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
