package com.immocare.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.exception.EstateNotFoundException;
import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.ImportBatchSummaryDTO;
import com.immocare.model.dto.ImportPreviewRowDTO;
import com.immocare.model.dto.ImportRowEnrichmentDTO;
import com.immocare.model.dto.SubcategorySuggestionDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.BankAccount;
import com.immocare.model.entity.Estate;
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
import com.immocare.repository.EstateRepository;
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
 * Service handling the 3-step transaction import flow.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all operations are now scoped to an estate.
 *
 * Duplicate detection is now per-estate:
 *   A transaction with the same fingerprint in estate A does not block
 *   import of the same transaction in estate B.
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
    private final EstateRepository estateRepository;

    // ─── Import history ───────────────────────────────────────────────────────

    public List<ImportBatchSummaryDTO> getImportBatches(UUID estateId, int page, int size) {
        return importBatchRepository
                .findByEstateIdOrderByImportedAtDesc(
                        estateId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "importedAt")))
                .getContent()
                .stream()
                .map(b -> new ImportBatchSummaryDTO(
                        b.getId(),
                        b.getFilename(),
                        b.getTotalRows(),
                        b.getImportedCount(),
                        b.getDuplicateCount(),
                        b.getErrorCount(),
                        b.getImportedAt(),
                        b.getCreatedBy() != null ? b.getCreatedBy().getUsername() : null))
                .toList();
    }

    // ─── Preview ──────────────────────────────────────────────────────────────

    /**
     * Parse the file and return enriched preview rows without persisting anything.
     * Duplicate detection is now estate-scoped.
     */
    public List<ImportPreviewRowDTO> previewFile(UUID estateId, MultipartFile file, String parserCode)
            throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);
        List<ParsedTransaction> parsed = parseFile(file, parser);

        return parsed.stream()
                .map(row -> toPreviewRow(estateId, row))
                .collect(Collectors.toList());
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    /**
     * Parse the file, apply enrichments, and persist transactions — all scoped to the estate.
     */
    @Transactional
    public ImportBatchResultDTO importFile(
            UUID estateId,
            MultipartFile file,
            String parserCode,
            Long bankAccountId,
            List<ImportRowEnrichmentDTO> enrichments,
            Set<String> selectedFingerprints,
            AppUser currentUser) throws ParseException {

        Estate estate = findEstateOrThrow(estateId);
        TransactionParser parser = parserRegistry.getOrThrow(parserCode);
        List<ParsedTransaction> parsed = parseFile(file, parser);

        Map<String, ImportRowEnrichmentDTO> enrichmentMap = enrichments.stream()
                .collect(Collectors.toMap(ImportRowEnrichmentDTO::fingerprint, Function.identity(),
                        (a, b) -> b));

        // Resolve bank account — must belong to this estate
        BankAccount bankAccount = null;
        if (bankAccountId != null) {
            bankAccount = bankAccountRepository.findById(bankAccountId)
                    .filter(ba -> ba.getEstate().getId().equals(estateId))
                    .orElse(null);
        }

        ImportBatch batch = new ImportBatch();
        batch.setFilename(file.getOriginalFilename());
        batch.setTotalRows(parsed.size());
        batch.setCreatedBy(currentUser);
        batch.setEstate(estate);
        importBatchRepository.save(batch);

        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        List<ImportBatchResultDTO.RowError> errors = new ArrayList<>();

        for (ParsedTransaction row : parsed) {

            if (!selectedFingerprints.isEmpty() && !selectedFingerprints.contains(row.getFingerprint())) {
                continue;
            }

            // Estate-scoped duplicate detection
            if (transactionRepository.existsByImportFingerprintAndEstateId(row.getFingerprint(), estateId)) {
                log.debug("Duplicate fingerprint skipped for estate {}: {}", estateId, row.getFingerprint());
                duplicateCount++;
                continue;
            }

            try {
                ImportRowEnrichmentDTO enrichment = enrichmentMap.get(row.getFingerprint());
                FinancialTransaction tx = buildTransaction(estate, row, enrichment, bankAccount, batch, currentUser);
                transactionRepository.save(tx);

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
     * Convert a parsed row to a preview DTO with estate-scoped suggestions.
     */
    private ImportPreviewRowDTO toPreviewRow(UUID estateId, ParsedTransaction row) {

        // Estate-scoped duplicate detection
        boolean duplicate = transactionRepository.existsByImportFingerprintAndEstateId(
                row.getFingerprint(), estateId);

        Long duplicateTransactionId = duplicate
                ? transactionRepository.findIdByImportFingerprintAndEstateId(row.getFingerprint(), estateId)
                : null;

        SubcategorySuggestionDTO suggestion = suggestSubcategory(row);
        ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = suggestLease(estateId, row);

        TransactionDirection direction = toApiDirection(row.getDirection());

        return new ImportPreviewRowDTO(
                row.getRowNumber(),
                row.getRawLine(),
                row.getTransactionDate(),
                row.getAmount(),
                direction,
                row.getDescription(),
                row.getCounterpartyAccount(),
                row.getFingerprint(),
                duplicate,
                duplicateTransactionId,
                suggestion,
                leaseSuggestion,
                null);
    }

    /**
     * Build a FinancialTransaction entity from a parsed row, bound to the given estate.
     */
    private FinancialTransaction buildTransaction(
            Estate estate,
            ParsedTransaction row,
            ImportRowEnrichmentDTO enrichment,
            BankAccount bankAccount,
            ImportBatch batch,
            AppUser currentUser) {

        FinancialTransaction tx = new FinancialTransaction();

        tx.setEstate(estate);
        tx.setReference(generateReference(currentUser));
        tx.setTransactionDate(row.getTransactionDate());
        tx.setValueDate(row.getValueDate());
        tx.setAccountingMonth(computeAccountingMonth(row, enrichment));
        tx.setAmount(row.getAmount());
        tx.setDirection(resolveDirection(row, enrichment));
        tx.setDescription(row.getDescription());
        tx.setCounterpartyAccount(row.getCounterpartyAccount());
        tx.setExternalReference(row.getExternalReference());
        tx.setImportFingerprint(row.getFingerprint());
        tx.setSource(TransactionSource.IMPORT);
        tx.setImportBatch(batch);
        tx.setBankAccount(bankAccount);

        boolean hasEnrichment = enrichment != null;
        tx.setStatus(hasEnrichment ? TransactionStatus.CONFIRMED : TransactionStatus.DRAFT);

        if (hasEnrichment) {
            applyEnrichment(tx, enrichment);
        } else {
            // Auto-apply subcategory suggestion even for DRAFT
            SubcategorySuggestionDTO suggestion = suggestSubcategory(row);
            if (suggestion != null) {
                subcategoryRepository.findById(suggestion.subcategoryId())
                        .ifPresent(tx::setSubcategory);
            }
            // Auto-apply lease suggestion (scoped to estate)
            ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = suggestLease(estate.getId(), row);
            if (leaseSuggestion != null) {
                leaseRepository.findById(leaseSuggestion.leaseId())
                        .ifPresent(tx::setSuggestedLease);
                housingUnitRepository.findById(leaseSuggestion.unitId())
                        .ifPresent(tx::setHousingUnit);
            }
        }

        return tx;
    }

    private TransactionDirection resolveDirection(ParsedTransaction row, ImportRowEnrichmentDTO enrichment) {
        if (enrichment != null && enrichment.directionOverride() != null) {
            return TransactionDirection.valueOf(enrichment.directionOverride());
        }
        return toApiDirection(row.getDirection());
    }

    private TransactionDirection toApiDirection(ParsedTransaction.Direction direction) {
        if (direction == null) {
            throw new IllegalStateException(
                    "Parser produced a transaction with null direction.");
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
    }

    private LocalDate computeAccountingMonth(ParsedTransaction row, ImportRowEnrichmentDTO enrichment) {
        return row.getTransactionDate().withDayOfMonth(1);
    }

    /**
     * Suggests a subcategory using learning rules (not yet estate-scoped — global rules for now).
     * Learning rules will be estate-scoped in Phase 5.
     */
    private SubcategorySuggestionDTO suggestSubcategory(ParsedTransaction row) {
        int minConf = 1;

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
     * Suggests a lease by matching the counterparty IBAN against person bank accounts
     * that belong to the same estate (scoped in Phase 4).
     */
    private ImportPreviewRowDTO.SuggestedLeaseDTO suggestLease(UUID estateId, ParsedTransaction row) {
        if (row.getCounterpartyAccount() == null) {
            return null;
        }

        Optional<PersonBankAccount> pba = personBankAccountRepository.findByIban(row.getCounterpartyAccount());

        if (pba.isEmpty()) {
            return null;
        }

        var person = pba.get().getPerson();

        // Estate scope check: only suggest if the person belongs to this estate
        if (person.getEstate() == null || !person.getEstate().getId().equals(estateId)) {
            return null;
        }

        var leases = leaseRepository.findAllByTenantPersonIdOrderByStartDateDesc(person.getId());

        if (leases.isEmpty()) {
            return null;
        }

        var lease = leases.get(0);
        var unit = lease.getHousingUnit();
        var building = unit.getBuilding();

        // Additional estate scope check on the lease side
        if (!building.getEstate().getId().equals(estateId)) {
            return null;
        }

        return new ImportPreviewRowDTO.SuggestedLeaseDTO(
                lease.getId(),
                unit.getId(),
                unit.getUnitNumber(),
                building.getId(),
                building.getName(),
                person.getId(),
                person.getFirstName() + " " + person.getLastName());
    }

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

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }
}
