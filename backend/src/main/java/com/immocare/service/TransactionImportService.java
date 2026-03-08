package com.immocare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.immocare.model.entity.TransactionParser;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.ImportBatchRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionParserRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private final TransactionParserRegistry parserRegistry;
    private final ImportBatchRepository importBatchRepo;
    private final FinancialTransactionRepository transactionRepo;
    private final BankAccountRepository bankAccountRepo;
    private final LeaseRepository leaseRepo;
    private final PersonBankAccountRepository personBankAccountRepo;
    private final TagSubcategoryRepository tagSubcategoryRepo;
    private final HousingUnitRepository housingUnitRepo;
    private final BuildingRepository buildingRepo;
    private final LearningService learningService;

    // ── Preview ───────────────────────────────────────────────────────────────

    /**
     * Parse a file and return enriched preview rows WITHOUT persisting anything.
     * Each row carries: duplicate flag, subcategory suggestion, lease suggestion.
     */
    @Transactional(readOnly = true)
    public List<ImportPreviewRowDTO> previewFile(
            MultipartFile file,
            String parserCode) throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);

        List<ParsedTransaction> parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("File parsing failed: " + e.getMessage(), e);
        }

        // Pre-load active leases once for the whole batch
        var activeLeases = leaseRepo.findAllActiveWithTenants();

        List<ImportPreviewRowDTO> rows = new ArrayList<>(parsed.size());

        for (ParsedTransaction p : parsed) {

            // ── Duplicate check ───────────────────────────────────────────
            boolean duplicate = p.getFingerprint() != null
                    && transactionRepo.existsByImportFingerprint(p.getFingerprint());

            // ── Subcategory suggestion (only if direction is known) ────────
            SubcategorySuggestionDTO subSuggestion = null;
            if (p.getDirection() != null) {
                TransactionDirection dir = p.getDirection() == ParsedTransaction.Direction.INCOME
                        ? TransactionDirection.INCOME
                        : TransactionDirection.EXPENSE;
                List<SubcategorySuggestionDTO> suggestions = learningService.suggestSubcategory(
                        p.getCounterpartyAccount(), p.getCounterpartyName(),
                        p.getDescription(), dir, 1);
                if (!suggestions.isEmpty()) {
                    subSuggestion = suggestions.get(0);
                }
            }

            // ── Lease suggestion via person IBAN ──────────────────────────
            ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = null;
            if (p.getCounterpartyAccount() != null && !p.getCounterpartyAccount().isBlank()
                    && p.getDirection() == ParsedTransaction.Direction.INCOME) {

                var pbaOpt = personBankAccountRepo.findByIban(p.getCounterpartyAccount());
                if (pbaOpt.isPresent()) {
                    Long personId = pbaOpt.get().getPerson().getId();
                    var person = pbaOpt.get().getPerson();

                    var matchedLease = activeLeases.stream()
                            .filter(l -> l.getTenants().stream()
                                    .anyMatch(t -> t.getPerson().getId().equals(personId)))
                            .findFirst();

                    if (matchedLease.isPresent()) {
                        var lease = matchedLease.get();
                        var unit = lease.getHousingUnit();
                        leaseSuggestion = new ImportPreviewRowDTO.SuggestedLeaseDTO(
                                lease.getId(),
                                unit.getId(),
                                unit.getUnitNumber(),
                                unit.getBuilding().getName(),
                                personId,
                                person.getFirstName() + " " + person.getLastName());
                    }
                }
            }

            TransactionDirection direction = p.getDirection() == null ? null
                    : p.getDirection() == ParsedTransaction.Direction.INCOME
                            ? TransactionDirection.INCOME
                            : TransactionDirection.EXPENSE;

            rows.add(new ImportPreviewRowDTO(
                    p.getRowNumber(),
                    p.getRawLine(),
                    p.getTransactionDate(),
                    p.getAmount(),
                    direction,
                    p.getDescription(),
                    p.getCounterpartyName(),
                    p.getCounterpartyAccount(),
                    p.getFingerprint(),
                    duplicate,
                    subSuggestion,
                    leaseSuggestion,
                    null));
        }

        return rows;
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Parse and persist a file, applying per-row enrichments from the frontend.
     *
     * <p>
     * Logic per row:
     * <ol>
     * <li>Skip if duplicate (fingerprint already in DB)</li>
     * <li>Build FinancialTransaction from parsed data</li>
     * <li>Look up enrichment by fingerprint — apply subcategory, lease, unit,
     * building,
     * direction override</li>
     * <li>If enrichment found → status = CONFIRMED + reinforce learning; else
     * DRAFT</li>
     * <li>If no enrichment → fall back to suggestLeaseFromCounterpartyIban
     * (suggestedLease only)</li>
     * </ol>
     *
     * @param enrichments per-row enrichments keyed by fingerprint; may be empty
     */
    @Transactional
    public ImportBatchResultDTO importFile(
            MultipartFile file,
            String parserCode,
            Long bankAccountId,
            List<ImportRowEnrichmentDTO> enrichments,
            AppUser currentUser) throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);

        BankAccount bankAccount = bankAccountId != null
                ? bankAccountRepo.findById(bankAccountId).orElse(null)
                : null;

        List<ParsedTransaction> parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("File parsing failed: " + e.getMessage(), e);
        }

        // Build fingerprint → enrichment lookup map
        Map<String, ImportRowEnrichmentDTO> enrichmentMap = enrichments.stream()
                .filter(e -> e.fingerprint() != null && !e.fingerprint().isBlank())
                .collect(Collectors.toMap(
                        ImportRowEnrichmentDTO::fingerprint,
                        Function.identity(),
                        (a, b) -> a)); // keep first on collision

        ImportBatch batch = new ImportBatch();
        batch.setFilename(file.getOriginalFilename());
        batch.setTotalRows(parsed.size());
        batch.setCreatedBy(currentUser);
        importBatchRepo.save(batch);

        int imported = 0, duplicates = 0;

        for (ParsedTransaction p : parsed) {

            // ── Duplicate check ───────────────────────────────────────────
            if (p.getFingerprint() != null
                    && transactionRepo.existsByImportFingerprint(p.getFingerprint())) {
                log.debug("Duplicate skipped: fingerprint={}", p.getFingerprint());
                duplicates++;
                continue;
            }

            log.debug("Saving row {}: date={} amount={} direction={} counterparty={}",
                    p.getRowNumber(), p.getTransactionDate(), p.getAmount(),
                    p.getDirection(), p.getCounterpartyName());

            // ── Build base transaction ─────────────────────────────────────
            FinancialTransaction tx = new FinancialTransaction();

            String year = String.valueOf(p.getTransactionDate().getYear());
            long seq = transactionRepo.nextRefSequence();
            tx.setReference("TXN-" + year + "-" + String.format("%05d", seq));

            tx.setTransactionDate(p.getTransactionDate());
            tx.setAccountingMonth(p.getTransactionDate().withDayOfMonth(1));
            tx.setAmount(p.getAmount());
            tx.setDescription(p.getDescription());
            tx.setCounterpartyName(p.getCounterpartyName());
            tx.setCounterpartyAccount(p.getCounterpartyAccount());
            tx.setImportFingerprint(p.getFingerprint());
            tx.setSource(TransactionSource.IMPORT);
            tx.setImportBatch(batch);
            tx.setBankAccount(bankAccount);

            // Direction: parser value, may be overridden by enrichment below
            TransactionDirection direction = p.getDirection() == null
                    ? TransactionDirection.EXPENSE // fallback for CSV without sign
                    : p.getDirection() == ParsedTransaction.Direction.INCOME
                            ? TransactionDirection.INCOME
                            : TransactionDirection.EXPENSE;
            tx.setDirection(direction);

            // ── Apply enrichment if present ────────────────────────────────
            ImportRowEnrichmentDTO enrichment = p.getFingerprint() != null
                    ? enrichmentMap.get(p.getFingerprint())
                    : null;

            if (enrichment != null) {
                applyEnrichment(tx, enrichment);
                tx.setStatus(TransactionStatus.CONFIRMED);
                transactionRepo.save(tx);
                reinforceLearning(tx);
            } else {
                tx.setStatus(TransactionStatus.DRAFT);
                transactionRepo.save(tx);
                suggestLeaseFromCounterpartyIban(tx);
            }

            imported++;
        }

        batch.setImportedCount(imported);
        batch.setDuplicateCount(duplicates);
        importBatchRepo.save(batch);

        log.info("Import complete: batchId={} imported={} duplicates={} enriched={}",
                batch.getId(), imported, duplicates, enrichmentMap.size());

        return new ImportBatchResultDTO(batch.getId(), parsed.size(), imported, duplicates, 0,
                List.of());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Apply user-provided enrichment onto an unsaved transaction.
     * Direction override is applied first so downstream validations are consistent.
     */
    private void applyEnrichment(FinancialTransaction tx, ImportRowEnrichmentDTO e) {

        // Direction override
        if (e.directionOverride() != null && !e.directionOverride().isBlank()) {
            try {
                tx.setDirection(TransactionDirection.valueOf(e.directionOverride()));
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown directionOverride '{}' — keeping parser value", e.directionOverride());
            }
        }

        // Subcategory
        if (e.subcategoryId() != null) {
            tx.setSubcategory(tagSubcategoryRepo.getReferenceById(e.subcategoryId()));
        }

        // Housing unit + cascade building
        if (e.housingUnitId() != null) {
            var unit = housingUnitRepo.getReferenceById(e.housingUnitId());
            tx.setHousingUnit(unit);
            // Always derive building from unit to keep them consistent
            tx.setBuilding(unit.getBuilding());
        } else if (e.buildingId() != null) {
            tx.setBuilding(buildingRepo.getReferenceById(e.buildingId()));
        }

        // Lease (income only — business rule BR-UC014-02)
        if (e.leaseId() != null && tx.getDirection() == TransactionDirection.INCOME) {
            tx.setLease(leaseRepo.getReferenceById(e.leaseId()));
            tx.setSuggestedLease(null);
        }
    }

    /**
     * Reinforce the learning engine after a confirmed enrichment.
     * Mirrors the logic in FinancialTransactionService.reinforceLearning().
     */
    private void reinforceLearning(FinancialTransaction tx) {
        if (tx.getSubcategory() == null)
            return;
        if (tx.getCounterpartyAccount() == null || tx.getCounterpartyAccount().isBlank())
            return;
        learningService.reinforceTagRule(tx.getSubcategory().getId(), tx.getCounterpartyAccount());
    }

    /**
     * For DRAFT rows: auto-suggest a lease from the counterparty IBAN if possible.
     * Sets suggestedLease + housingUnit + building on the transaction.
     */
    private void suggestLeaseFromCounterpartyIban(FinancialTransaction tx) {
        if (tx.getCounterpartyAccount() == null || tx.getCounterpartyAccount().isBlank())
            return;
        if (tx.getDirection() != TransactionDirection.INCOME)
            return;
        if (tx.getSuggestedLease() != null)
            return;

        personBankAccountRepo.findByIban(tx.getCounterpartyAccount()).ifPresent(pba -> {
            Long personId = pba.getPerson().getId();
            leaseRepo.findAllActiveWithTenants().stream()
                    .filter(l -> l.getTenants().stream()
                            .anyMatch(t -> t.getPerson().getId().equals(personId)))
                    .findFirst()
                    .ifPresent(lease -> {
                        tx.setSuggestedLease(lease);
                        tx.setHousingUnit(lease.getHousingUnit());
                        tx.setBuilding(lease.getHousingUnit().getBuilding());
                        transactionRepo.save(tx);
                        log.debug("Suggested lease {} for tx fingerprint={}",
                                lease.getId(), tx.getImportFingerprint());
                    });
        });
    }
}
