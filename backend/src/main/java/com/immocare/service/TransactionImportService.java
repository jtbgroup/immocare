package com.immocare.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.ImportPreviewRowDTO;
import com.immocare.model.dto.ImportRowEnrichmentDTO;
import com.immocare.model.dto.SubcategorySuggestionDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.BankAccount;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.ImportBatch;
import com.immocare.model.entity.ParsedTransaction;
import com.immocare.model.entity.PersonBankAccount;
import com.immocare.model.entity.TagSubcategory;
import com.immocare.model.entity.TransactionParser;
import com.immocare.model.enums.TagMatchField;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.ImportBatchRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.TagLearningRuleRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionParserRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service handling the 3-step transaction import flow:
 * 1. preview — parse only, no persistence, returns rows with suggestions
 * 2. import — parse + apply enrichments + persist
 *
 * Direction is always determined by the parser (sign of amount in CSV).
 * There is no manual direction assignment step for CSV imports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private final TransactionParserRegistry parserRegistry;
    private final FinancialTransactionRepository transactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TagLearningRuleRepository learningRuleRepository;
    private final TagSubcategoryRepository subcategoryRepository;
    private final PersonBankAccountRepository personBankAccountRepository;
    private final LeaseRepository leaseRepository;
    private final HousingUnitRepository housingUnitRepository;

    // ─── Preview ──────────────────────────────────────────────────────────────

    /**
     * Parse the file and return enriched preview rows without persisting anything.
     */
    public List<ImportPreviewRowDTO> previewFile(MultipartFile file, String parserCode)
            throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);
        List<ParsedTransaction> parsed = parseFile(file, parser);

        return parsed.stream()
                .map(this::toPreviewRow)
                .collect(Collectors.toList());
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    /**
     * Parse the file, apply enrichments, and persist transactions.
     * Rows with a matching enrichment are saved as CONFIRMED; others as DRAFT.
     * Duplicate fingerprints are skipped silently.
     */
    @Transactional
    public ImportBatchResultDTO importFile(
            MultipartFile file,
            String parserCode,
            Long bankAccountId,
            List<ImportRowEnrichmentDTO> enrichments,
            Set<String> selectedFingerprints,
            AppUser currentUser) throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);
        List<ParsedTransaction> parsed = parseFile(file, parser);

        // Build enrichment lookup by fingerprint
        Map<String, ImportRowEnrichmentDTO> enrichmentMap = enrichments.stream()
                .collect(Collectors.toMap(ImportRowEnrichmentDTO::fingerprint, Function.identity(),
                        (a, b) -> b));

        // Resolve bank account
        BankAccount bankAccount = bankAccountId != null
                ? bankAccountRepository.findById(bankAccountId).orElse(null)
                : null;

        // Create import batch
        ImportBatch batch = new ImportBatch();
        batch.setFilename(file.getOriginalFilename());
        batch.setTotalRows(parsed.size());
        batch.setCreatedBy(currentUser);
        importBatchRepository.save(batch);

        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<ImportBatchResultDTO.RowError> errors = new ArrayList<>();

        for (ParsedTransaction row : parsed) {

            // Skip if not in selected fingerprints (when selection is provided)
            if (!selectedFingerprints.isEmpty() && !selectedFingerprints.contains(row.getFingerprint())) {
                continue;
            }

            // Duplicate detection
            if (transactionRepository.existsByImportFingerprint(row.getFingerprint())) {
                log.debug("Duplicate fingerprint skipped: {}", row.getFingerprint());
                duplicateCount++;
                continue;
            }

            try {
                ImportRowEnrichmentDTO enrichment = enrichmentMap.get(row.getFingerprint());
                FinancialTransaction tx = buildTransaction(row, enrichment, bankAccount, batch, currentUser);
                transactionRepository.save(tx);

                // Update learning rules if subcategory was assigned
                if (tx.getSubcategory() != null) {
                    updateLearningRules(row, tx.getSubcategory());
                }

                importedCount++;

            } catch (Exception e) {
                log.error("Error importing row {}: {}", row.getRowNumber(), e.getMessage(), e);
                errorCount++;
                errors.add(new ImportBatchResultDTO.RowError(
                        row.getRowNumber(), row.getRawLine(), e.getMessage()));
            }
        }

        // Update batch counters
        batch.setImportedCount(importedCount);
        batch.setDuplicateCount(duplicateCount);
        batch.setErrorCount(errorCount);
        importBatchRepository.save(batch);

        return new ImportBatchResultDTO(
                batch.getId(), parsed.size(), importedCount, duplicateCount, errorCount, errors);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private List<ParsedTransaction> parseFile(MultipartFile file, TransactionParser parser)
            throws ParseException {
        try {
            return parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new ParseException("Cannot read uploaded file: " + e.getMessage(), e);
        }
    }

    /**
     * Convert a parsed row to a preview DTO with suggestions.
     */
    private ImportPreviewRowDTO toPreviewRow(ParsedTransaction row) {

        boolean duplicate = transactionRepository.existsByImportFingerprint(row.getFingerprint());

        Long duplicateTransactionId = duplicate
                ? transactionRepository.findIdByImportFingerprint(row.getFingerprint())
                : null;

        SubcategorySuggestionDTO suggestion = suggestSubcategory(row);
        ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = suggestLease(row);

        // Direction is always known from the CSV sign — convert to API enum
        TransactionDirection direction = toApiDirection(row.getDirection());

        return new ImportPreviewRowDTO(
                row.getRowNumber(),
                row.getRawLine(),
                row.getTransactionDate(),
                row.getAmount(),
                direction,
                row.getDescription(),
                row.getCounterpartyName(),
                row.getCounterpartyAccount(),
                row.getFingerprint(),
                duplicate,
                duplicateTransactionId,
                suggestion,
                leaseSuggestion,
                null // no parse error
        );
    }

    /**
     * Build a FinancialTransaction entity from a parsed row and optional
     * enrichment.
     * Direction comes from the parser (sign of CSV amount) — enrichment may
     * override it.
     */
    private FinancialTransaction buildTransaction(
            ParsedTransaction row,
            ImportRowEnrichmentDTO enrichment,
            BankAccount bankAccount,
            ImportBatch batch,
            AppUser currentUser) {

        FinancialTransaction tx = new FinancialTransaction();

        // Reference
        tx.setReference(generateReference(currentUser));

        // Dates
        tx.setTransactionDate(row.getTransactionDate());
        tx.setValueDate(row.getValueDate()); // populated from "Date valeur" column
        tx.setAccountingMonth(computeAccountingMonth(row, enrichment));

        // Amount and direction — direction from CSV sign, overridable via enrichment
        tx.setAmount(row.getAmount());
        tx.setDirection(resolveDirection(row, enrichment));

        // Counterparty
        tx.setDescription(row.getDescription());
        tx.setCounterpartyName(row.getCounterpartyName());
        tx.setCounterpartyAccount(row.getCounterpartyAccount());

        // External reference from Keytrade "Extrait" column
        tx.setExternalReference(row.getExternalReference());

        // Deduplication fingerprint
        tx.setImportFingerprint(row.getFingerprint());

        // Source
        tx.setSource(TransactionSource.IMPORT);
        tx.setImportBatch(batch);
        tx.setBankAccount(bankAccount);

        // Status: CONFIRMED if enrichment provided, DRAFT otherwise
        boolean hasEnrichment = enrichment != null;
        tx.setStatus(hasEnrichment ? TransactionStatus.CONFIRMED : TransactionStatus.DRAFT);

        // Apply enrichment
        if (hasEnrichment) {
            applyEnrichment(tx, enrichment);
        } else {
            // Auto-apply subcategory suggestion even for DRAFT
            SubcategorySuggestionDTO suggestion = suggestSubcategory(row);
            if (suggestion != null) {
                subcategoryRepository.findById(suggestion.subcategoryId())
                        .ifPresent(tx::setSubcategory);
            }
            // Auto-apply lease suggestion
            ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = suggestLease(row);
            if (leaseSuggestion != null) {
                leaseRepository.findById(leaseSuggestion.leaseId())
                        .ifPresent(tx::setSuggestedLease);
                housingUnitRepository.findById(leaseSuggestion.unitId())
                        .ifPresent(tx::setHousingUnit);
            }
        }

        return tx;
    }

    /**
     * Resolve direction: enrichment override takes precedence over parser-detected
     * direction.
     */
    private TransactionDirection resolveDirection(ParsedTransaction row, ImportRowEnrichmentDTO enrichment) {
        if (enrichment != null && enrichment.directionOverride() != null) {
            return TransactionDirection.valueOf(enrichment.directionOverride());
        }
        return toApiDirection(row.getDirection());
    }

    /**
     * Convert internal ParsedTransaction.Direction to API TransactionDirection
     * enum.
     * Direction is always set by the CSV parser — this should never return null.
     */
    private TransactionDirection toApiDirection(ParsedTransaction.Direction direction) {
        if (direction == null) {
            throw new IllegalStateException(
                    "Parser produced a transaction with null direction. "
                            + "The Keytrade CSV parser always determines direction from the amount sign.");
        }
        return switch (direction) {
            case INCOME -> TransactionDirection.INCOME;
            case EXPENSE -> TransactionDirection.EXPENSE;
        };
    }

    private void applyEnrichment(FinancialTransaction tx, ImportRowEnrichmentDTO enrichment) {
        if (enrichment.subcategoryId() != null) {
            subcategoryRepository.findById(enrichment.subcategoryId())
                    .ifPresent(tx::setSubcategory);
        }
        if (enrichment.leaseId() != null) {
            leaseRepository.findById(enrichment.leaseId())
                    .ifPresent(tx::setLease);
        }
        if (enrichment.housingUnitId() != null) {
            housingUnitRepository.findById(enrichment.housingUnitId())
                    .ifPresent(tx::setHousingUnit);
        }
        if (enrichment.buildingId() != null) {
            // building is set indirectly — service resolves from housingUnit when present
        }
    }

    /**
     * Compute accounting month from transaction date.
     * Always the first day of the transaction month.
     * Enrichment can override this.
     */
    private LocalDate computeAccountingMonth(ParsedTransaction row, ImportRowEnrichmentDTO enrichment) {
        // TODO: apply AccountingMonthRule learning if subcategory is known
        return row.getTransactionDate().withDayOfMonth(1);
    }

    /**
     * Suggest best subcategory using learning rules.
     * Tries counterparty account first (most reliable), then description.
     */
    private SubcategorySuggestionDTO suggestSubcategory(ParsedTransaction row) {
        int minConf = 1;

        // Try counterparty IBAN
        if (row.getCounterpartyAccount() != null) {
            var rules = learningRuleRepository.findSuggestions(
                    TagMatchField.COUNTERPARTY_ACCOUNT, row.getCounterpartyAccount(), minConf);
            if (!rules.isEmpty()) {
                var rule = rules.get(0);
                var sub = rule.getSubcategory();
                return new SubcategorySuggestionDTO(
                        sub.getId(), sub.getName(),
                        sub.getCategory().getId(), sub.getCategory().getName(),
                        rule.getConfidence());
            }
        }

        // Try description
        if (row.getDescription() != null && !row.getDescription().isBlank()) {
            var rules = learningRuleRepository.findSuggestions(
                    TagMatchField.DESCRIPTION, row.getDescription(), minConf);
            if (!rules.isEmpty()) {
                var rule = rules.get(0);
                var sub = rule.getSubcategory();
                return new SubcategorySuggestionDTO(
                        sub.getId(), sub.getName(),
                        sub.getCategory().getId(), sub.getCategory().getName(),
                        rule.getConfidence());
            }
        }

        return null;
    }

    /**
     * Suggest a lease by matching the counterparty IBAN against known person bank
     * accounts.
     */
    private ImportPreviewRowDTO.SuggestedLeaseDTO suggestLease(ParsedTransaction row) {
        if (row.getCounterpartyAccount() == null) {
            return null;
        }

        Optional<PersonBankAccount> pba = personBankAccountRepository.findByIban(row.getCounterpartyAccount());

        if (pba.isEmpty()) {
            return null;
        }

        var person = pba.get().getPerson();
        var leases = leaseRepository.findAllByTenantPersonIdOrderByStartDateDesc(person.getId());

        if (leases.isEmpty()) {
            return null;
        }

        var lease = leases.get(0);
        var unit = lease.getHousingUnit();
        var building = unit.getBuilding();

        return new ImportPreviewRowDTO.SuggestedLeaseDTO(
                lease.getId(),
                unit.getId(),
                unit.getUnitNumber(),
                building.getId(),
                building.getName(),
                person.getId(),
                person.getFirstName() + " " + person.getLastName());
    }

    /**
     * Reinforce or create a learning rule when a subcategory is confirmed at import
     * time.
     */
    private void updateLearningRules(ParsedTransaction row, TagSubcategory subcategory) {
        if (row.getCounterpartyAccount() != null) {
            upsertRule(TagMatchField.COUNTERPARTY_ACCOUNT, row.getCounterpartyAccount(), subcategory);
        }
    }

    private void upsertRule(TagMatchField field, String value, TagSubcategory subcategory) {
        learningRuleRepository
                .findByMatchFieldAndMatchValueIgnoreCaseAndSubcategoryId(field, value, subcategory.getId())
                .ifPresentOrElse(
                        rule -> {
                            rule.setConfidence(rule.getConfidence() + 1);
                            rule.setLastMatchedAt(java.time.LocalDateTime.now());
                            learningRuleRepository.save(rule);
                        },
                        () -> {
                            var rule = new com.immocare.model.entity.TagLearningRule();
                            rule.setMatchField(field);
                            rule.setMatchValue(value);
                            rule.setSubcategory(subcategory);
                            rule.setConfidence(1);
                            rule.setLastMatchedAt(java.time.LocalDateTime.now());
                            learningRuleRepository.save(rule);
                        });
    }

    private String generateReference(AppUser currentUser) {
        long seq = transactionRepository.nextRefSequence();
        return "TXN-" + seq;
    }
}