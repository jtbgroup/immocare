package com.immocare.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.immocare.model.entity.Lease;
import com.immocare.model.entity.ParsedTransaction;
import com.immocare.model.entity.TransactionParser;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.ImportBatchRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PersonBankAccountRepository;
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
    private final PersonBankAccountRepository personBankAccountRepo;
    private final LeaseRepository leaseRepo;
    private final LearningService learningService;
    private final com.immocare.repository.TagSubcategoryRepository subcategoryRepo;
    private final com.immocare.repository.HousingUnitRepository housingUnitRepo;
    private final com.immocare.repository.BuildingRepository buildingRepo;

    // ─── Preview ──────────────────────────────────────────────────────────────

    /**
     * Parse the file and return enriched preview rows — nothing is persisted.
     * Each row carries:
     * - duplicate flag (fingerprint already in DB)
     * - suggested subcategory (from learning rules)
     * - suggested lease (from counterparty IBAN → person → lease)
     */
    public List<ImportPreviewRowDTO> previewFile(MultipartFile file, String parserCode)
            throws ParseException {

        TransactionParser parser = parserRegistry.getOrThrow(parserCode);

        List<ParsedTransaction> parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("File parsing failed: " + e.getMessage(), e);
        }

        return parsed.stream().map(p -> {
            if (p.getFingerprint() == null) {
                return new ImportPreviewRowDTO(
                        p.getRowNumber(), p.getRawLine(), null, null, null,
                        null, null, null, null, false, null, null, null,
                        "Parse error: missing fingerprint");
            }

            // Duplicate check
            boolean duplicate = transactionRepo.existsByImportFingerprint(p.getFingerprint());
            Long duplicateTxId = duplicate
                    ? transactionRepo.findIdByImportFingerprint(p.getFingerprint())
                    : null;

            // Subcategory suggestion
            TransactionDirection dir = toDirection(p.getDirection());
            List<SubcategorySuggestionDTO> suggestions = learningService.suggestSubcategory(
                    p.getCounterpartyAccount(), p.getCounterpartyName(),
                    p.getDescription(), dir, 1);
            SubcategorySuggestionDTO subcatSuggestion = suggestions.isEmpty() ? null : suggestions.get(0);

            // Lease suggestion
            ImportPreviewRowDTO.SuggestedLeaseDTO leaseSuggestion = suggestLeaseForPreview(p.getCounterpartyAccount(),
                    p.getTransactionDate());

            return new ImportPreviewRowDTO(
                    p.getRowNumber(),
                    p.getRawLine(),
                    p.getTransactionDate(),
                    p.getAmount(),
                    dir,
                    p.getDescription(),
                    p.getCounterpartyName(),
                    p.getCounterpartyAccount(),
                    p.getFingerprint(),
                    duplicate,
                    duplicateTxId,
                    subcatSuggestion,
                    leaseSuggestion,
                    null);
        }).collect(Collectors.toList());
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    /**
     * Parse + apply per-row enrichments + persist.
     *
     * Rows whose fingerprint is in {@code selectedFingerprints} are imported.
     * If an enrichment exists for a fingerprint, it is applied (subcategory,
     * lease, unit, building, direction override).
     * Rows with enrichments are saved as CONFIRMED; others as DRAFT.
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

        // Index enrichments by fingerprint for O(1) lookup
        Map<String, ImportRowEnrichmentDTO> enrichmentMap = enrichments == null
                ? Map.of()
                : enrichments.stream()
                        .filter(e -> e.fingerprint() != null)
                        .collect(Collectors.toMap(
                                ImportRowEnrichmentDTO::fingerprint,
                                Function.identity(),
                                (a, b) -> a));

        ImportBatch batch = new ImportBatch();
        batch.setFilename(file.getOriginalFilename());
        batch.setTotalRows(parsed.size());
        batch.setCreatedBy(currentUser);
        importBatchRepo.save(batch);

        int imported = 0, duplicates = 0;

        for (ParsedTransaction p : parsed) {

            // Skip rows not selected by user (when selection is non-empty)
            if (selectedFingerprints != null && !selectedFingerprints.isEmpty()
                    && !selectedFingerprints.contains(p.getFingerprint())) {
                continue;
            }

            // Duplicate check by fingerprint
            if (p.getFingerprint() != null
                    && transactionRepo.existsByImportFingerprint(p.getFingerprint())) {
                log.debug("Duplicate skipped: fingerprint={}", p.getFingerprint());
                duplicates++;
                continue;
            }

            log.debug("Saving row {}: date={} amount={} direction={} counterparty={}",
                    p.getRowNumber(), p.getTransactionDate(), p.getAmount(),
                    p.getDirection(), p.getCounterpartyName());

            ImportRowEnrichmentDTO enrichment = p.getFingerprint() != null
                    ? enrichmentMap.get(p.getFingerprint())
                    : null;

            // Resolve direction: enrichment override > parser > INCOME default
            TransactionDirection direction = resolveDirection(p.getDirection(), enrichment);

            FinancialTransaction tx = new FinancialTransaction();

            String year = String.valueOf(p.getTransactionDate().getYear());
            long seq = transactionRepo.nextRefSequence();
            tx.setReference("TXN-" + year + "-" + String.format("%05d", seq));

            tx.setTransactionDate(p.getTransactionDate());
            tx.setAccountingMonth(p.getTransactionDate().withDayOfMonth(1));
            tx.setAmount(p.getAmount());
            tx.setDirection(direction);
            tx.setDescription(p.getDescription());
            tx.setCounterpartyName(p.getCounterpartyName());
            tx.setCounterpartyAccount(p.getCounterpartyAccount());
            tx.setImportFingerprint(p.getFingerprint());
            tx.setSource(TransactionSource.IMPORT);
            tx.setImportBatch(batch);
            tx.setBankAccount(bankAccount);

            // Apply enrichments
            if (enrichment != null) {
                applyEnrichment(tx, enrichment);
                tx.setStatus(TransactionStatus.CONFIRMED);
            } else {
                // Auto-suggest lease even without explicit enrichment
                suggestLease(tx, p.getCounterpartyAccount(), p.getTransactionDate());
                tx.setStatus(TransactionStatus.DRAFT);
            }

            transactionRepo.save(tx);
            imported++;
        }

        batch.setImportedCount(imported);
        batch.setDuplicateCount(duplicates);
        batch.setErrorCount(0);
        importBatchRepo.save(batch);

        log.info("Import complete: batchId={} imported={} duplicates={}",
                batch.getId(), imported, duplicates);

        return new ImportBatchResultDTO(
                batch.getId(), parsed.size(), imported, duplicates, 0, List.of());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Lease suggestion for the preview endpoint — returns a lightweight DTO,
     * does not modify any entity.
     */
    private ImportPreviewRowDTO.SuggestedLeaseDTO suggestLeaseForPreview(
            String counterpartyIban, LocalDate transactionDate) {

        if (counterpartyIban == null || counterpartyIban.isBlank())
            return null;

        return personBankAccountRepo.findByIban(counterpartyIban)
                .map(pba -> {
                    List<Lease> leases = leaseRepo
                            .findAllByTenantPersonIdOrderByStartDateDesc(pba.getPerson().getId());
                    if (leases.isEmpty())
                        return null;

                    Lease best = pickBestLease(leases, transactionDate);

                    // Find the matched tenant's name
                    String personName = best.getTenants().stream()
                            .filter(lt -> lt.getPerson().getId().equals(pba.getPerson().getId()))
                            .findFirst()
                            .map(lt -> lt.getPerson().getLastName() + " " + lt.getPerson().getFirstName())
                            .orElse(pba.getPerson().getLastName() + " " + pba.getPerson().getFirstName());

                    return new ImportPreviewRowDTO.SuggestedLeaseDTO(
                            best.getId(),
                            best.getHousingUnit().getId(),
                            best.getHousingUnit().getUnitNumber(),
                            best.getHousingUnit().getBuilding().getId(),
                            best.getHousingUnit().getBuilding().getName(),
                            pba.getPerson().getId(),
                            personName);
                })
                .orElse(null);
    }

    /**
     * Lease suggestion for the import endpoint — sets fields directly on the tx.
     * Result stored in suggested_lease_id only — user confirms during review.
     */
    private void suggestLease(FinancialTransaction tx,
            String counterpartyIban,
            LocalDate transactionDate) {
        if (counterpartyIban == null || counterpartyIban.isBlank())
            return;

        personBankAccountRepo.findByIban(counterpartyIban).ifPresent(pba -> {
            List<Lease> leases = leaseRepo
                    .findAllByTenantPersonIdOrderByStartDateDesc(pba.getPerson().getId());
            if (leases.isEmpty())
                return;

            Lease best = pickBestLease(leases, transactionDate);
            tx.setSuggestedLease(best);
            if (tx.getHousingUnit() == null)
                tx.setHousingUnit(best.getHousingUnit());
            if (tx.getBuilding() == null && best.getHousingUnit() != null)
                tx.setBuilding(best.getHousingUnit().getBuilding());

            log.debug("Lease suggested: leaseId={} tenant={} status={}",
                    best.getId(), pba.getPerson().getLastName(), best.getStatus());
        });
    }

    /**
     * Picks the best matching lease for a given transaction date.
     *
     * Priority:
     * 1. Exactly one lease covers the date → use it.
     * 2. Several cover it → prefer ACTIVE, then most recent startDate.
     * 3. None covers it → closest endDate (historical import).
     */
    private Lease pickBestLease(List<Lease> leases, LocalDate transactionDate) {
        List<Lease> covering = leases.stream()
                .filter(l -> !transactionDate.isBefore(l.getStartDate())
                        && !transactionDate.isAfter(l.getEndDate()))
                .toList();

        if (covering.size() == 1)
            return covering.get(0);
        if (covering.size() > 1) {
            return covering.stream()
                    .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                    .findFirst()
                    .orElse(covering.get(0));
        }
        // Historical: closest endDate
        return leases.stream()
                .min(Comparator.comparingLong(
                        l -> Math.abs(ChronoUnit.DAYS.between(l.getEndDate(), transactionDate))))
                .orElse(leases.get(0));
    }

    /**
     * Applies user-provided enrichment to a transaction.
     * Non-null enrichment fields override auto-suggestions.
     */
    private void applyEnrichment(FinancialTransaction tx, ImportRowEnrichmentDTO e) {
        if (e.subcategoryId() != null) {
            subcategoryRepo.findById(e.subcategoryId()).ifPresent(tx::setSubcategory);
        }
        if (e.leaseId() != null) {
            leaseRepo.findById(e.leaseId()).ifPresent(tx::setLease);
        }
        if (e.housingUnitId() != null) {
            housingUnitRepo.findById(e.housingUnitId()).ifPresent(tx::setHousingUnit);
        }
        if (e.buildingId() != null) {
            buildingRepo.findById(e.buildingId()).ifPresent(tx::setBuilding);
        }
        // Propagate unit + building from lease when not explicitly set
        if (tx.getLease() != null) {
            if (tx.getHousingUnit() == null)
                tx.setHousingUnit(tx.getLease().getHousingUnit());
            if (tx.getBuilding() == null && tx.getLease().getHousingUnit() != null)
                tx.setBuilding(tx.getLease().getHousingUnit().getBuilding());
        }
    }

    private TransactionDirection resolveDirection(
            ParsedTransaction.Direction parsed, ImportRowEnrichmentDTO enrichment) {
        if (enrichment != null && enrichment.directionOverride() != null) {
            return TransactionDirection.valueOf(enrichment.directionOverride());
        }
        return toDirection(parsed);
    }

    private TransactionDirection toDirection(ParsedTransaction.Direction d) {
        if (d == null)
            return TransactionDirection.INCOME; // safe default
        return d == ParsedTransaction.Direction.INCOME
                ? TransactionDirection.INCOME
                : TransactionDirection.EXPENSE;
    }
}