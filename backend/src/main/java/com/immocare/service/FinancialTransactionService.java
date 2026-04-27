package com.immocare.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.AssetLinkValidationException;
import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.exception.SubcategoryDirectionMismatchException;
import com.immocare.exception.SubcategoryNotFoundException;
import com.immocare.exception.TransactionNotEditableException;
import com.immocare.exception.TransactionNotFoundException;
import com.immocare.exception.TransactionValidationException;
import com.immocare.model.dto.BulkPatchTransactionRequest;
import com.immocare.model.dto.BulkPatchTransactionResult;
import com.immocare.model.dto.ConfirmTransactionRequest;
import com.immocare.model.dto.CreateTransactionRequest;
import com.immocare.model.dto.FinancialTransactionDTO;
import com.immocare.model.dto.FinancialTransactionSummaryDTO;
import com.immocare.model.dto.PagedTransactionResponse;
import com.immocare.model.dto.SaveAssetLinkRequest;
import com.immocare.model.dto.StatisticsFilter;
import com.immocare.model.dto.TransactionAssetLinkDTO;
import com.immocare.model.dto.TransactionFilter;
import com.immocare.model.dto.TransactionStatisticsDTO;
import com.immocare.model.dto.UpdateTransactionRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Boiler;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.FireExtinguisher;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.TagSubcategory;
import com.immocare.model.entity.TransactionAssetLink;
import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.BankAccountType;
import com.immocare.model.enums.SubcategoryDirection;
import com.immocare.model.enums.TagMatchField;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.MeterRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionAssetLinkRepository;
import com.immocare.repository.spec.TransactionSpecification;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Service for FinancialTransaction management.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all operations are now scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionAssetLinkRepository assetLinkRepository;
    private final LearningService learningService;
    private final BoilerRepository boilerRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;
    private final MeterRepository meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final LeaseRepository leaseRepository;
    private final EstateRepository estateRepository;

    public FinancialTransactionService(FinancialTransactionRepository transactionRepository,
            TagSubcategoryRepository tagSubcategoryRepository,
            BankAccountRepository bankAccountRepository,
            TransactionAssetLinkRepository assetLinkRepository,
            LearningService learningService,
            BoilerRepository boilerRepository,
            FireExtinguisherRepository fireExtinguisherRepository,
            MeterRepository meterRepository,
            HousingUnitRepository housingUnitRepository,
            BuildingRepository buildingRepository,
            LeaseRepository leaseRepository,
            EstateRepository estateRepository) {
        this.transactionRepository = transactionRepository;
        this.tagSubcategoryRepository = tagSubcategoryRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.assetLinkRepository = assetLinkRepository;
        this.learningService = learningService;
        this.boilerRepository = boilerRepository;
        this.fireExtinguisherRepository = fireExtinguisherRepository;
        this.meterRepository = meterRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.buildingRepository = buildingRepository;
        this.leaseRepository = leaseRepository;
        this.estateRepository = estateRepository;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public PagedTransactionResponse getAll(UUID estateId, TransactionFilter filter, Pageable pageable) {
        Specification<FinancialTransaction> spec = buildSpec(estateId, filter);
        Page<FinancialTransaction> page = transactionRepository.findAll(spec, pageable);

        BigDecimal totalIncome = computeTotal(spec, TransactionDirection.INCOME);
        BigDecimal totalExpenses = computeTotal(spec, TransactionDirection.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        List<FinancialTransactionSummaryDTO> content = page.getContent().stream()
                .map(this::toSummaryDTO).toList();

        return new PagedTransactionResponse(content, pageable.getPageNumber(), pageable.getPageSize(),
                page.getTotalElements(), page.getTotalPages(), totalIncome, totalExpenses, netBalance);
    }

    public FinancialTransactionDTO getById(UUID estateId, Long id) {
        return toDTO(findInEstate(estateId, id));
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDTO create(UUID estateId, CreateTransactionRequest req, AppUser currentUser) {
        Estate estate = findEstateOrThrow(estateId);

        validateTransactionRequest(estateId, req.direction(), req.leaseId(), req.subcategoryId(),
                req.housingUnitId(), req.assetLinks());

        FinancialTransaction tx = new FinancialTransaction();
        tx.setEstate(estate);
        tx.setDirection(req.direction());
        tx.setTransactionDate(req.transactionDate());
        tx.setValueDate(req.valueDate());
        tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setCounterpartyAccount(req.counterpartyAccount());
        tx.setStatus(TransactionStatus.CONFIRMED);
        tx.setSource(TransactionSource.MANUAL);

        applyRelations(tx, req.bankAccountId(), req.subcategoryId(),
                req.leaseId(), req.housingUnitId(), req.buildingId());

        if (tx.getHousingUnit() != null) {
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }

        int year = java.time.LocalDate.now().getYear();
        String prefix = "TXN-" + year + "-";
        int seq = transactionRepository.nextSequenceForYear(prefix + "%", estateId);
        String reference = prefix + String.format("%05d", seq);
        tx.setReference(reference);

        if (req.assetLinks() != null && !req.assetLinks().isEmpty()) {
            applyAssetLinks(tx, req.assetLinks(), req.amount());
        }

        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDTO update(UUID estateId, Long id, UpdateTransactionRequest req) {
        FinancialTransaction tx = findInEstate(estateId, id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        validateTransactionRequest(estateId, req.direction(), req.leaseId(), req.subcategoryId(),
                req.housingUnitId(), req.assetLinks());

        tx.setDirection(req.direction());
        tx.setTransactionDate(req.transactionDate());
        tx.setValueDate(req.valueDate());
        tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setCounterpartyAccount(req.counterpartyAccount());

        applyRelations(tx, req.bankAccountId(), req.subcategoryId(),
                req.leaseId(), req.housingUnitId(), req.buildingId());
        if (tx.getHousingUnit() != null) {
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }

        tx.getAssetLinks().clear();
        if (req.assetLinks() != null && !req.assetLinks().isEmpty()) {
            applyAssetLinks(tx, req.assetLinks(), req.amount());
        }

        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID estateId, Long id) {
        FinancialTransaction tx = findInEstate(estateId, id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        transactionRepository.delete(tx);
    }

    // ─── Confirm ──────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDTO confirm(UUID estateId, Long id, ConfirmTransactionRequest req, AppUser currentUser) {
        FinancialTransaction tx = findInEstate(estateId, id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        if (req.subcategoryId() != null) {
            verifySubcategoryBelongsToEstate(estateId, req.subcategoryId());
            tx.setSubcategory(tagSubcategoryRepository.getReferenceById(req.subcategoryId()));
        }
        if (req.accountingMonth() != null) {
            tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        }
        if (req.buildingId() != null) {
            tx.setBuilding(buildingRepository.getReferenceById(req.buildingId()));
        }
        if (req.housingUnitId() != null) {
            tx.setHousingUnit(housingUnitRepository.getReferenceById(req.housingUnitId()));
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }
        if (tx.getSuggestedLease() != null && req.leaseId() == null) {
            tx.setLease(tx.getSuggestedLease());
            tx.setSuggestedLease(null);
        } else if (req.leaseId() != null) {
            tx.setSuggestedLease(null);
        }
        tx.setStatus(TransactionStatus.CONFIRMED);
        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    // ─── Confirm batch ────────────────────────────────────────────────────────

    @Transactional
    public int confirmBatch(UUID estateId, Long batchId) {
        Page<FinancialTransaction> drafts = transactionRepository.findByImportBatchId(
                batchId, Pageable.unpaged());
        int count = 0;
        for (FinancialTransaction tx : drafts) {
            // Only confirm transactions belonging to this estate
            if (tx.getEstate() != null && !tx.getEstate().getId().equals(estateId)) {
                continue;
            }
            if (tx.getStatus() == TransactionStatus.DRAFT) {
                tx.setStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(tx);
                reinforceLearning(tx);
                count++;
            }
        }
        return count;
    }

    // ─── Bulk patch ───────────────────────────────────────────────────────────

    @Transactional
    public BulkPatchTransactionResult bulkPatch(UUID estateId, BulkPatchTransactionRequest req) {
        if (req.status() == null && req.subcategoryId() == null) {
            throw new IllegalArgumentException("At least one patch field (status or subcategoryId) must be provided");
        }

        TagSubcategory subcategory = null;
        if (req.subcategoryId() != null && req.subcategoryId() != 0) {
            verifySubcategoryBelongsToEstate(estateId, req.subcategoryId());
            subcategory = tagSubcategoryRepository.findById(req.subcategoryId())
                    .orElseThrow(() -> new SubcategoryNotFoundException(
                            "Subcategory not found: " + req.subcategoryId()));
        }
        final TagSubcategory resolvedSub = subcategory;

        List<FinancialTransaction> transactions = transactionRepository.findAllById(req.ids());

        int updated = 0, skipped = 0;

        for (FinancialTransaction tx : transactions) {
            // Skip transactions belonging to a different estate
            if (tx.getEstate() == null || !tx.getEstate().getId().equals(estateId)) {
                skipped++;
                continue;
            }
            if (tx.getStatus() == TransactionStatus.RECONCILED) {
                skipped++;
                continue;
            }

            boolean changed = false;

            if (req.status() != null && tx.getStatus() != req.status()) {
                tx.setStatus(req.status());
                changed = true;
            }

            if (req.subcategoryId() != null) {
                if (req.subcategoryId() == 0) {
                    tx.setSubcategory(null);
                    changed = true;
                } else {
                    boolean compatible = resolvedSub.getDirection() == null
                            || (resolvedSub.getDirection() == SubcategoryDirection.INCOME
                                    && tx.getDirection() == TransactionDirection.INCOME)
                            || (resolvedSub.getDirection() == SubcategoryDirection.EXPENSE
                                    && tx.getDirection() == TransactionDirection.EXPENSE)
                            || resolvedSub.getDirection() == SubcategoryDirection.BOTH;
                    if (compatible) {
                        tx.setSubcategory(resolvedSub);
                        changed = true;
                    } else {
                        skipped++;
                        continue;
                    }
                }
            }

            if (changed) {
                transactionRepository.save(tx);
                if (req.status() == TransactionStatus.CONFIRMED) {
                    reinforceLearning(tx);
                }
                updated++;
            }
        }

        return new BulkPatchTransactionResult(updated, skipped);
    }

    // ─── Statistics ───────────────────────────────────────────────────────────

    public TransactionStatisticsDTO getStatistics(UUID estateId, StatisticsFilter filter) {
        Specification<FinancialTransaction> base = TransactionSpecification.hasEstate(estateId)
                .and(TransactionSpecification.confirmedOrReconciled());

        if (filter.accountingFrom() != null)
            base = base.and(TransactionSpecification.withAccountingFrom(filter.accountingFrom()));
        if (filter.accountingTo() != null)
            base = base.and(TransactionSpecification.withAccountingTo(filter.accountingTo()));
        if (filter.buildingId() != null)
            base = base.and(TransactionSpecification.withBuildingId(filter.buildingId()));
        if (filter.unitId() != null)
            base = base.and(TransactionSpecification.withUnitId(filter.unitId()));
        if (filter.bankAccountId() != null)
            base = base.and(TransactionSpecification.withBankAccountId(filter.bankAccountId()));
        if (filter.direction() != null)
            base = base.and(TransactionSpecification.withDirection(filter.direction()));

        List<FinancialTransaction> all = transactionRepository.findAll(base);

        BigDecimal totalIncome = sum(all, TransactionDirection.INCOME);
        BigDecimal totalExpenses = sum(all, TransactionDirection.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        var byCategory = all.stream()
                .filter(t -> t.getSubcategory() != null)
                .collect(Collectors.groupingBy(t -> t.getSubcategory().getCategory().getId()))
                .entrySet().stream()
                .map(entry -> {
                    var txs = entry.getValue();
                    String catName = txs.get(0).getSubcategory().getCategory().getName();
                    BigDecimal catTotal = txs.stream().map(FinancialTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var subs = txs.stream()
                            .collect(Collectors.groupingBy(t -> t.getSubcategory().getId()))
                            .entrySet().stream()
                            .map(se -> {
                                var stxs = se.getValue();
                                BigDecimal subAmt = stxs.stream().map(FinancialTransaction::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                double pct = catTotal.compareTo(BigDecimal.ZERO) == 0 ? 0
                                        : subAmt.doubleValue() / catTotal.doubleValue() * 100.0;
                                return new TransactionStatisticsDTO.SubcategoryBreakdownDTO(
                                        se.getKey(), stxs.get(0).getSubcategory().getName(),
                                        stxs.get(0).getSubcategory().getDirection(),
                                        subAmt, stxs.size(), pct);
                            }).toList();
                    return new TransactionStatisticsDTO.CategoryBreakdownDTO(entry.getKey(), catName, subs, catTotal);
                }).toList();

        var byBuilding = all.stream()
                .collect(Collectors.groupingBy(t -> t.getBuilding() != null ? t.getBuilding().getId() : -1L))
                .entrySet().stream()
                .map(e -> {
                    var txs = e.getValue();
                    String name = e.getKey() == -1L ? "Unassigned" : txs.get(0).getBuilding().getName();
                    Long bid = e.getKey() == -1L ? null : e.getKey();
                    return new TransactionStatisticsDTO.BuildingBreakdownDTO(bid, name,
                            sum(txs, TransactionDirection.INCOME),
                            sum(txs, TransactionDirection.EXPENSE),
                            sum(txs, TransactionDirection.INCOME).subtract(sum(txs, TransactionDirection.EXPENSE)));
                }).toList();

        var byUnit = all.stream()
                .filter(t -> t.getHousingUnit() != null)
                .collect(Collectors.groupingBy(t -> t.getHousingUnit().getId()))
                .entrySet().stream()
                .map(e -> {
                    var txs = e.getValue();
                    HousingUnit u = txs.get(0).getHousingUnit();
                    return new TransactionStatisticsDTO.UnitBreakdownDTO(e.getKey(),
                            u.getUnitNumber(), u.getBuilding().getName(),
                            sum(txs, TransactionDirection.INCOME),
                            sum(txs, TransactionDirection.EXPENSE),
                            sum(txs, TransactionDirection.INCOME).subtract(sum(txs, TransactionDirection.EXPENSE)));
                }).toList();

        var byBankAccount = all.stream()
                .collect(Collectors.groupingBy(t -> t.getBankAccount() != null ? t.getBankAccount().getId() : -1L))
                .entrySet().stream()
                .map(e -> {
                    var txs = e.getValue();
                    Long baId = e.getKey() == -1L ? null : e.getKey();
                    String label = e.getKey() == -1L ? "Unassigned" : txs.get(0).getBankAccount().getLabel();
                    BankAccountType type = e.getKey() == -1L ? null : txs.get(0).getBankAccount().getType();
                    return new TransactionStatisticsDTO.BankAccountBreakdownDTO(baId, label, type,
                            sum(txs, TransactionDirection.INCOME),
                            sum(txs, TransactionDirection.EXPENSE),
                            sum(txs, TransactionDirection.INCOME).subtract(sum(txs, TransactionDirection.EXPENSE)));
                }).toList();

        var monthlyTrend = all.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAccountingMonth().getYear() * 100 + t.getAccountingMonth().getMonthValue()))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> {
                    var txs = e.getValue();
                    int year = e.getKey() / 100;
                    int month = e.getKey() % 100;
                    return new TransactionStatisticsDTO.MonthlyTrendDTO(year, month,
                            sum(txs, TransactionDirection.INCOME),
                            sum(txs, TransactionDirection.EXPENSE));
                }).toList();

        return new TransactionStatisticsDTO(totalIncome, totalExpenses, netBalance,
                byCategory, byBuilding, byUnit, byBankAccount, monthlyTrend);
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    public void exportCsv(UUID estateId, TransactionFilter filter, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");
        Specification<FinancialTransaction> spec = buildSpec(estateId, filter);
        List<FinancialTransaction> all = transactionRepository.findAll(spec);

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println(
                "Reference;ExternalReference;Date;AccountingMonth;Direction;Amount;Category;Subcategory;" +
                        "Status;BankAccount;CounterpartyAccount;Description;Building;Unit;Lease;Source");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (FinancialTransaction tx : all) {
            BigDecimal signedAmount = tx.getDirection() == TransactionDirection.INCOME
                    ? tx.getAmount()
                    : tx.getAmount().negate();
            writer.println(
                    csv(tx.getReference()) + ";" +
                            csv(tx.getExternalReference()) + ";" +
                            csv(tx.getTransactionDate()) + ";" +
                            csv(tx.getAccountingMonth().format(monthFmt)) + ";" +
                            csv(tx.getDirection().name()) + ";" +
                            signedAmount + ";" +
                            csv(tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getName() : "") + ";" +
                            csv(tx.getSubcategory() != null ? tx.getSubcategory().getName() : "") + ";" +
                            csv(tx.getStatus().name()) + ";" +
                            csv(tx.getBankAccount() != null ? tx.getBankAccount().getLabel() : "") + ";" +
                            csv(tx.getCounterpartyAccount()) + ";" +
                            csv(tx.getDescription()) + ";" +
                            csv(tx.getBuilding() != null ? tx.getBuilding().getName() : "") + ";" +
                            csv(tx.getHousingUnit() != null ? tx.getHousingUnit().getUnitNumber() : "") + ";" +
                            csv(tx.getLease() != null ? String.valueOf(tx.getLease().getId()) : "") + ";" +
                            csv(tx.getSource().name()));
        }
        writer.flush();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Finds a transaction and verifies it belongs to the given estate.
     */
    private FinancialTransaction findInEstate(UUID estateId, Long id) {
        FinancialTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Financial transaction not found: " + id));
        if (tx.getEstate() == null || !tx.getEstate().getId().equals(estateId)) {
            throw new EstateAccessDeniedException();
        }
        return tx;
    }

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }

    private void verifySubcategoryBelongsToEstate(UUID estateId, Long subcategoryId) {
        if (!tagSubcategoryRepository.existsByCategory_EstateIdAndId(estateId, subcategoryId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Specification<FinancialTransaction> buildSpec(UUID estateId, TransactionFilter f) {
        // Always scope to the estate
        Specification<FinancialTransaction> spec = TransactionSpecification.hasEstate(estateId);

        if (f.direction() != null)
            spec = spec.and(TransactionSpecification.withDirection(f.direction()));
        if (f.from() != null)
            spec = spec.and(TransactionSpecification.withDateFrom(f.from()));
        if (f.to() != null)
            spec = spec.and(TransactionSpecification.withDateTo(f.to()));
        if (f.accountingFrom() != null)
            spec = spec.and(TransactionSpecification.withAccountingFrom(f.accountingFrom()));
        if (f.accountingTo() != null)
            spec = spec.and(TransactionSpecification.withAccountingTo(f.accountingTo()));
        if (f.categoryId() != null)
            spec = spec.and(TransactionSpecification.withCategoryId(f.categoryId()));
        if (f.subcategoryId() != null)
            spec = spec.and(TransactionSpecification.withSubcategoryId(f.subcategoryId()));
        if (f.bankAccountId() != null)
            spec = spec.and(TransactionSpecification.withBankAccountId(f.bankAccountId()));
        if (f.buildingId() != null)
            spec = spec.and(TransactionSpecification.withBuildingId(f.buildingId()));
        if (f.unitId() != null)
            spec = spec.and(TransactionSpecification.withUnitId(f.unitId()));
        if (f.status() != null)
            spec = spec.and(TransactionSpecification.withStatus(f.status()));
        if (f.search() != null && !f.search().isBlank())
            spec = spec.and(TransactionSpecification.withSearch(f.search()));
        if (f.importBatchId() != null)
            spec = spec.and(TransactionSpecification.withImportBatchId(f.importBatchId()));
        if (f.assetType() != null && f.assetId() != null)
            spec = spec.and(TransactionSpecification.withAssetLink(f.assetType(), f.assetId()));

        return spec;
    }

    private BigDecimal computeTotal(Specification<FinancialTransaction> baseSpec, TransactionDirection direction) {
        Specification<FinancialTransaction> spec = baseSpec.and(TransactionSpecification.withDirection(direction));
        return transactionRepository.findAll(spec).stream()
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sum(List<FinancialTransaction> txs, TransactionDirection dir) {
        return txs.stream().filter(t -> t.getDirection() == dir)
                .map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyRelations(FinancialTransaction tx, Long bankAccountId, Long subcategoryId,
            Long leaseId, Long housingUnitId, Long buildingId) {
        tx.setBankAccount(bankAccountId != null ? bankAccountRepository.getReferenceById(bankAccountId) : null);
        tx.setSubcategory(subcategoryId != null ? tagSubcategoryRepository.getReferenceById(subcategoryId) : null);
        tx.setLease(leaseId != null ? leaseRepository.getReferenceById(leaseId) : null);
        tx.setHousingUnit(housingUnitId != null ? housingUnitRepository.getReferenceById(housingUnitId) : null);
        tx.setBuilding(buildingId != null ? buildingRepository.getReferenceById(buildingId) : null);
    }

    private void applyAssetLinks(FinancialTransaction tx, List<SaveAssetLinkRequest> linkRequests,
            BigDecimal txAmount) {
        BigDecimal partialSum = linkRequests.stream()
                .map(SaveAssetLinkRequest::amount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (partialSum.compareTo(txAmount) > 0) {
            throw new AssetLinkValidationException(
                    "Sum of asset link amounts (" + partialSum + ") exceeds transaction total (" + txAmount + ")");
        }

        for (SaveAssetLinkRequest linkReq : linkRequests) {
            if (linkReq.assetType() == AssetType.BOILER && tx.getBuilding() != null) {
                boilerRepository.findById(linkReq.assetId()).ifPresent(boiler -> {
                    Long boilerBuildingId = resolveBuildingIdFromBoiler(boiler);
                    if (boilerBuildingId != null && !boilerBuildingId.equals(tx.getBuilding().getId())) {
                        throw new AssetLinkValidationException(
                                "Boiler " + linkReq.assetId() + " does not belong to building "
                                        + tx.getBuilding().getId());
                    }
                });
            }

            TransactionAssetLink link = new TransactionAssetLink();
            link.setTransaction(tx);
            link.setAssetType(linkReq.assetType());
            link.setAssetId(linkReq.assetId());
            link.setAmount(linkReq.amount());
            link.setNotes(linkReq.notes());
            resolveAssetLinkContext(link);
            tx.getAssetLinks().add(link);
        }
    }

    private void resolveAssetLinkContext(TransactionAssetLink link) {
        switch (link.getAssetType()) {
            case BOILER -> boilerRepository.findById(link.getAssetId()).ifPresent(boiler -> {
                if ("HOUSING_UNIT".equals(boiler.getOwnerType())) {
                    housingUnitRepository.findById(boiler.getOwnerId()).ifPresent(unit -> {
                        link.setHousingUnit(unit);
                        link.setBuilding(unit.getBuilding());
                    });
                } else if ("BUILDING".equals(boiler.getOwnerType())) {
                    buildingRepository.findById(boiler.getOwnerId()).ifPresent(link::setBuilding);
                }
            });
            case FIRE_EXTINGUISHER -> fireExtinguisherRepository.findById(link.getAssetId()).ifPresent(ext -> {
                link.setBuilding(ext.getBuilding());
                if (ext.getUnit() != null) {
                    link.setHousingUnit(ext.getUnit());
                }
            });
            case METER -> meterRepository.findById(link.getAssetId()).ifPresent(meter -> {
                if ("HOUSING_UNIT".equals(meter.getOwnerType())) {
                    housingUnitRepository.findById(meter.getOwnerId()).ifPresent(unit -> {
                        link.setHousingUnit(unit);
                        link.setBuilding(unit.getBuilding());
                    });
                } else if ("BUILDING".equals(meter.getOwnerType())) {
                    buildingRepository.findById(meter.getOwnerId()).ifPresent(link::setBuilding);
                }
            });
        }
    }

    private Long resolveBuildingIdFromBoiler(Boiler boiler) {
        if ("BUILDING".equals(boiler.getOwnerType())) {
            return boiler.getOwnerId();
        }
        if ("HOUSING_UNIT".equals(boiler.getOwnerType())) {
            return housingUnitRepository.findById(boiler.getOwnerId())
                    .map(unit -> unit.getBuilding().getId())
                    .orElse(null);
        }
        return null;
    }

    private void validateTransactionRequest(UUID estateId, TransactionDirection direction, Long leaseId,
            Long subcategoryId, Long housingUnitId, List<SaveAssetLinkRequest> assetLinks) {
        if (direction == TransactionDirection.EXPENSE && leaseId != null) {
            throw new TransactionValidationException("Lease link is only allowed for income transactions");
        }
        if (subcategoryId != null) {
            TagSubcategory sub = tagSubcategoryRepository.findById(subcategoryId)
                    .orElseThrow(() -> new SubcategoryNotFoundException("Subcategory not found: " + subcategoryId));
            if ((sub.getDirection() == SubcategoryDirection.INCOME && direction == TransactionDirection.EXPENSE) ||
                    (sub.getDirection() == SubcategoryDirection.EXPENSE && direction == TransactionDirection.INCOME)) {
                throw new SubcategoryDirectionMismatchException(
                        "Subcategory '" + sub.getName() + "' is not compatible with direction " + direction);
            }
            // Verify subcategory belongs to this estate
            verifySubcategoryBelongsToEstate(estateId, subcategoryId);
        }
    }

    private void reinforceLearning(FinancialTransaction tx) {
        if (tx.getSubcategory() != null && tx.getCounterpartyAccount() != null
                && !tx.getCounterpartyAccount().isBlank()) {
            learningService.reinforceTagRule(tx.getSubcategory().getId(), TagMatchField.COUNTERPARTY_ACCOUNT,
                    tx.getCounterpartyAccount());
            int offset = (int) (tx.getAccountingMonth().getYear() * 12L
                    + tx.getAccountingMonth().getMonthValue()
                    - LocalDate.now().getYear() * 12L - LocalDate.now().getMonthValue());
            learningService.reinforceAccountingMonthRule(
                    tx.getSubcategory().getId(), tx.getCounterpartyAccount(), offset);
        }
        if (tx.getSubcategory() != null) {
            tx.getAssetLinks().stream()
                    .map(l -> l.getAssetType().name())
                    .distinct()
                    .forEach(assetTypeName -> learningService.reinforceTagRule(
                            tx.getSubcategory().getId(), TagMatchField.ASSET_TYPE, assetTypeName));
        }
    }

    private String resolveAssetLabel(AssetType type, Long assetId) {
        return switch (type) {
            case BOILER -> boilerRepository.findById(assetId)
                    .map(b -> (b.getBrand() != null ? b.getBrand() : "") + " "
                            + (b.getModel() != null ? b.getModel() : ""))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .orElse("Boiler #" + assetId);
            case FIRE_EXTINGUISHER -> fireExtinguisherRepository.findById(assetId)
                    .map(FireExtinguisher::getIdentificationNumber)
                    .orElse("Extinguisher #" + assetId);
            case METER -> meterRepository.findById(assetId)
                    .map(m -> m.getMeterNumber() + " (" + m.getType() + ")")
                    .orElse("Meter #" + assetId);
        };
    }

    private TransactionAssetLinkDTO toLinkDTO(TransactionAssetLink l) {
        HousingUnit unit = l.getHousingUnit();
        Building building = l.getBuilding();
        return new TransactionAssetLinkDTO(
                l.getId(),
                l.getAssetType(),
                l.getAssetId(),
                resolveAssetLabel(l.getAssetType(), l.getAssetId()),
                unit != null ? unit.getId() : null,
                unit != null ? unit.getUnitNumber() : null,
                building != null ? building.getId() : null,
                building != null ? building.getName() : null,
                l.getAmount(),
                l.getNotes());
    }

    private FinancialTransactionSummaryDTO toSummaryDTO(FinancialTransaction tx) {
        return new FinancialTransactionSummaryDTO(
                tx.getId(),
                tx.getReference(),
                tx.getTransactionDate(),
                tx.getAccountingMonth(),
                tx.getDirection(),
                tx.getAmount(),
                tx.getCounterpartyAccount(),
                tx.getStatus(),
                tx.getSource(),
                tx.getBankAccount() != null ? tx.getBankAccount().getLabel() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getName() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getName() : null,
                tx.getBuilding() != null ? tx.getBuilding().getName() : null,
                tx.getHousingUnit() != null ? tx.getHousingUnit().getUnitNumber() : null,
                tx.getLease() != null ? tx.getLease().getId() : null,
                tx.getSuggestedLease() != null ? tx.getSuggestedLease().getId() : null,
                tx.getBuilding() != null ? tx.getBuilding().getId() : null,
                tx.getHousingUnit() != null ? tx.getHousingUnit().getId() : null);
    }

    private FinancialTransactionDTO toDTO(FinancialTransaction tx) {
        List<TransactionAssetLinkDTO> links = tx.getAssetLinks().stream()
                .map(this::toLinkDTO)
                .toList();
        return new FinancialTransactionDTO(
                tx.getId(), tx.getReference(), tx.getExternalReference(),
                tx.getTransactionDate(), tx.getValueDate(), tx.getAccountingMonth(),
                tx.getAmount(), tx.getDirection(), tx.getDescription(),
                tx.getCounterpartyAccount(), tx.getStatus(), tx.getSource(),
                tx.getBankAccount() != null ? tx.getBankAccount().getId() : null,
                tx.getBankAccount() != null ? tx.getBankAccount().getLabel() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getId() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getName() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getId() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getName() : null,
                tx.getLease() != null ? tx.getLease().getId() : null,
                tx.getSuggestedLease() != null ? tx.getSuggestedLease().getId() : null,
                tx.getHousingUnit() != null ? tx.getHousingUnit().getId() : null,
                tx.getHousingUnit() != null ? tx.getHousingUnit().getUnitNumber() : null,
                tx.getBuilding() != null ? tx.getBuilding().getId() : null,
                tx.getBuilding() != null ? tx.getBuilding().getName() : null,
                tx.getImportBatch() != null ? tx.getImportBatch().getId() : null,
                links,
                tx.getStatus() != TransactionStatus.RECONCILED,
                tx.getCreatedAt(), tx.getUpdatedAt());
    }

    private String csv(Object value) {
        if (value == null)
            return "";
        String s = value.toString().replace("\"", "\"\"");
        return s.contains(";") || s.contains("\"") ? "\"" + s + "\"" : s;
    }
}
