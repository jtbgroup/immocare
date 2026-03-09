package com.immocare.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.AssetLinkValidationException;
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
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.FireExtinguisher;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.TagSubcategory;
import com.immocare.model.entity.TransactionAssetLink;
import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.BankAccountType;
import com.immocare.model.enums.SubcategoryDirection;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionSource;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.MeterRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionAssetLinkRepository;
import com.immocare.repository.spec.TransactionSpecification;

import jakarta.servlet.http.HttpServletResponse;

@Service
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionAssetLinkRepository assetLinkRepository;
    private final LearningService learningService;

    // Repositories for asset label resolution
    private final BoilerRepository boilerRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;
    private final MeterRepository meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final LeaseRepository leaseRepository;

    public FinancialTransactionService(FinancialTransactionRepository transactionRepository,
            TagSubcategoryRepository tagSubcategoryRepository,
            BankAccountRepository bankAccountRepository,
            TransactionAssetLinkRepository assetLinkRepository,
            LearningService learningService,
            BoilerRepository boilerRepository,
            FireExtinguisherRepository fireExtinguisherRepository,
            MeterRepository meterRepository,
            HousingUnitRepository housingUnitRepository,
            BuildingRepository buildingRepository, LeaseRepository leaseRepository) {
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
    }

    public PagedTransactionResponse getAll(TransactionFilter filter, Pageable pageable) {
        Specification<FinancialTransaction> spec = buildSpec(filter);
        Page<FinancialTransaction> page = transactionRepository.findAll(spec, pageable);

        // Aggregate totals over full filter (all pages)
        BigDecimal totalIncome = computeTotal(spec, TransactionDirection.INCOME);
        BigDecimal totalExpenses = computeTotal(spec, TransactionDirection.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        List<FinancialTransactionSummaryDTO> content = page.getContent().stream()
                .map(this::toSummaryDTO).toList();

        return new PagedTransactionResponse(content, pageable.getPageNumber(), pageable.getPageSize(),
                page.getTotalElements(), page.getTotalPages(), totalIncome, totalExpenses, netBalance);
    }

    public FinancialTransactionDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public FinancialTransactionDTO create(CreateTransactionRequest req, AppUser currentUser) {
        validateTransactionRequest(req.direction(), req.leaseId(), req.subcategoryId(),
                req.housingUnitId(), req.assetLinks());

        FinancialTransaction tx = new FinancialTransaction();
        tx.setDirection(req.direction());
        tx.setTransactionDate(req.transactionDate());
        tx.setValueDate(req.valueDate());
        tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setCounterpartyName(req.counterpartyName());
        tx.setCounterpartyAccount(req.counterpartyAccount());
        tx.setStatus(TransactionStatus.CONFIRMED);
        tx.setSource(TransactionSource.MANUAL);

        applyRelations(tx, req.bankAccountId(), req.subcategoryId(),
                req.leaseId(), req.housingUnitId(), req.buildingId());

        // BR-UC014-03: unit → building
        if (tx.getHousingUnit() != null) {
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }

        // Generate reference
        String year = String.valueOf(req.transactionDate().getYear());
        long seq = transactionRepository.nextRefSequence();
        tx.setReference("TXN-" + year + "-" + String.format("%05d", seq));

        // Asset links
        if (req.assetLinks() != null) {
            for (SaveAssetLinkRequest linkReq : req.assetLinks()) {
                validateAssetLink(linkReq, tx);
                TransactionAssetLink link = new TransactionAssetLink();
                link.setTransaction(tx);
                link.setAssetType(linkReq.assetType());
                link.setAssetId(linkReq.assetId());
                link.setNotes(linkReq.notes());
                tx.getAssetLinks().add(link);
            }
        }

        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    @Transactional
    public FinancialTransactionDTO update(Long id, UpdateTransactionRequest req) {
        FinancialTransaction tx = findOrThrow(id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        validateTransactionRequest(req.direction(), req.leaseId(), req.subcategoryId(),
                req.housingUnitId(), req.assetLinks());

        tx.setDirection(req.direction());
        tx.setTransactionDate(req.transactionDate());
        tx.setValueDate(req.valueDate());
        tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setCounterpartyName(req.counterpartyName());
        tx.setCounterpartyAccount(req.counterpartyAccount());

        applyRelations(tx, req.bankAccountId(), req.subcategoryId(),
                req.leaseId(), req.housingUnitId(), req.buildingId());
        if (tx.getHousingUnit() != null) {
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }

        tx.getAssetLinks().clear();
        if (req.assetLinks() != null) {
            for (SaveAssetLinkRequest linkReq : req.assetLinks()) {
                validateAssetLink(linkReq, tx);
                TransactionAssetLink link = new TransactionAssetLink();
                link.setTransaction(tx);
                link.setAssetType(linkReq.assetType());
                link.setAssetId(linkReq.assetId());
                link.setNotes(linkReq.notes());
                tx.getAssetLinks().add(link);
            }
        }

        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        FinancialTransaction tx = findOrThrow(id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        transactionRepository.delete(tx);
    }

    @Transactional
    public FinancialTransactionDTO confirm(Long id, ConfirmTransactionRequest req, AppUser currentUser) {
        FinancialTransaction tx = findOrThrow(id);
        if (tx.getStatus() == TransactionStatus.RECONCILED) {
            throw new TransactionNotEditableException("Reconciled transactions cannot be modified");
        }
        if (req.subcategoryId() != null) {
            tx.setSubcategory(tagSubcategoryRepository.getReferenceById(req.subcategoryId()));
        }
        if (req.accountingMonth() != null) {
            tx.setAccountingMonth(req.accountingMonth().withDayOfMonth(1));
        }
        if (req.leaseId() != null && tx.getDirection() == TransactionDirection.INCOME) {
            // accept lease link on income
        }
        if (req.buildingId() != null) {
            tx.setBuilding(buildingRepository.getReferenceById(req.buildingId()));
        }
        if (req.housingUnitId() != null) {
            tx.setHousingUnit(housingUnitRepository.getReferenceById(req.housingUnitId()));
            tx.setBuilding(tx.getHousingUnit().getBuilding());
        }
        // Promote suggested lease
        if (tx.getSuggestedLease() != null && req.leaseId() == null) {
            tx.setLease(tx.getSuggestedLease());
            tx.setSuggestedLease(null);
        } else if (req.leaseId() != null) {
            // set directly — not injecting LeaseRepository to keep scope clean; use
            // reference
            tx.setSuggestedLease(null);
        }
        tx.setStatus(TransactionStatus.CONFIRMED);
        FinancialTransaction saved = transactionRepository.save(tx);
        reinforceLearning(saved);
        return toDTO(saved);
    }

    @Transactional
    public int confirmBatch(Long batchId) {
        Page<FinancialTransaction> drafts = transactionRepository.findByImportBatchId(
                batchId, Pageable.unpaged());
        int count = 0;
        for (FinancialTransaction tx : drafts) {
            if (tx.getStatus() == TransactionStatus.DRAFT) {
                tx.setStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(tx);
                reinforceLearning(tx);
                count++;
            }
        }
        return count;
    }

    /**
     * Bulk patch: apply status and/or subcategory to a list of transactions.
     *
     * Rules:
     * - RECONCILED transactions are always skipped.
     * - subcategoryId == 0 means "clear subcategory".
     * - subcategory direction must be compatible with transaction direction
     * (BR-UC014-06).
     * On mismatch the row is skipped (not an error — silently ignored).
     */
    @Transactional
    public BulkPatchTransactionResult bulkPatch(BulkPatchTransactionRequest req) {
        if (req.status() == null && req.subcategoryId() == null) {
            throw new IllegalArgumentException("At least one patch field (status or subcategoryId) must be provided");
        }

        // Pre-load subcategory once if needed
        TagSubcategory subcategory = null;
        if (req.subcategoryId() != null && req.subcategoryId() != 0) {
            subcategory = tagSubcategoryRepository.findById(req.subcategoryId())
                    .orElseThrow(() -> new SubcategoryNotFoundException(
                            "Subcategory not found: " + req.subcategoryId()));
        }
        final TagSubcategory resolvedSub = subcategory;

        List<FinancialTransaction> transactions = transactionRepository.findAllById(req.ids());

        int updated = 0, skipped = 0;

        for (FinancialTransaction tx : transactions) {
            if (tx.getStatus() == TransactionStatus.RECONCILED) {
                skipped++;
                continue;
            }

            boolean changed = false;

            // Apply status
            if (req.status() != null && tx.getStatus() != req.status()) {
                tx.setStatus(req.status());
                changed = true;
            }

            // Apply subcategory
            if (req.subcategoryId() != null) {
                if (req.subcategoryId() == 0) {
                    // Explicit clear
                    tx.setSubcategory(null);
                    changed = true;
                } else {
                    // Direction compatibility check (BR-UC014-06)
                    boolean compatible = resolvedSub.getDirection() == null
                            || (resolvedSub.getDirection() == SubcategoryDirection.INCOME
                                    && tx.getDirection() == TransactionDirection.INCOME)
                            || (resolvedSub.getDirection() == SubcategoryDirection.EXPENSE
                                    && tx.getDirection() == TransactionDirection.EXPENSE);
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

    public TransactionStatisticsDTO getStatistics(StatisticsFilter filter) {
        Specification<FinancialTransaction> base = TransactionSpecification.confirmedOrReconciled();
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

        // byCategory
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

        // byBuilding
        var byBuilding = all.stream()
                .collect(Collectors.groupingBy(t -> t.getBuilding() != null ? t.getBuilding().getId() : -1L))
                .entrySet().stream()
                .map(e -> {
                    var txs = e.getValue();
                    String name = e.getKey() == -1L ? "Unassigned"
                            : txs.get(0).getBuilding().getName();
                    Long bid = e.getKey() == -1L ? null : e.getKey();
                    return new TransactionStatisticsDTO.BuildingBreakdownDTO(bid, name,
                            sum(txs, TransactionDirection.INCOME),
                            sum(txs, TransactionDirection.EXPENSE),
                            sum(txs, TransactionDirection.INCOME).subtract(sum(txs, TransactionDirection.EXPENSE)));
                }).toList();

        // byUnit
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

        // byBankAccount
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

        // monthlyTrend
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

    public void exportCsv(TransactionFilter filter, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");
        Specification<FinancialTransaction> spec = buildSpec(filter);
        List<FinancialTransaction> all = transactionRepository.findAll(spec);

        PrintWriter writer = response.getWriter();
        // UTF-8 BOM
        writer.write('\uFEFF');
        writer.println(
                "Reference;Date;AccountingMonth;Direction;Amount;Counterparty;Description;Category;Subcategory;Status;BankAccount;Building;Unit;Lease");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (FinancialTransaction tx : all) {
            BigDecimal signedAmount = tx.getDirection() == TransactionDirection.INCOME
                    ? tx.getAmount()
                    : tx.getAmount().negate();
            writer.println(
                    csv(tx.getReference()) + ";" +
                            csv(tx.getTransactionDate()) + ";" +
                            csv(tx.getAccountingMonth().format(monthFmt)) + ";" +
                            csv(tx.getDirection().name()) + ";" +
                            signedAmount + ";" +
                            csv(tx.getCounterpartyName()) + ";" +
                            csv(tx.getDescription()) + ";" +
                            csv(tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getName() : "") + ";" +
                            csv(tx.getSubcategory() != null ? tx.getSubcategory().getName() : "") + ";" +
                            csv(tx.getStatus().name()) + ";" +
                            csv(tx.getBankAccount() != null ? tx.getBankAccount().getLabel() : "") + ";" +
                            csv(tx.getBuilding() != null ? tx.getBuilding().getName() : "") + ";" +
                            csv(tx.getHousingUnit() != null ? tx.getHousingUnit().getUnitNumber() : "") + ";" +
                            csv(tx.getLease() != null ? String.valueOf(tx.getLease().getId()) : ""));
        }
        writer.flush();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private FinancialTransaction findOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Financial transaction not found: " + id));
    }

    private Specification<FinancialTransaction> buildSpec(TransactionFilter f) {
        Specification<FinancialTransaction> spec = (root, query, cb) -> null;
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
        tx.setLease(leaseId != null ? leaseRepository.getReferenceById(leaseId) : null); // ← manquant
        tx.setHousingUnit(housingUnitId != null ? housingUnitRepository.getReferenceById(housingUnitId) : null);
        tx.setBuilding(buildingId != null ? buildingRepository.getReferenceById(buildingId) : null);
    }

    private void validateTransactionRequest(TransactionDirection direction, Long leaseId,
            Long subcategoryId, Long housingUnitId,
            List<SaveAssetLinkRequest> assetLinks) {
        // BR-UC014-02
        if (direction == TransactionDirection.EXPENSE && leaseId != null) {
            throw new TransactionValidationException("Lease link is only allowed for income transactions");
        }
        // BR-UC014-06
        if (subcategoryId != null) {
            TagSubcategory sub = tagSubcategoryRepository.findById(subcategoryId)
                    .orElseThrow(() -> new SubcategoryNotFoundException("Subcategory not found: " + subcategoryId));
            if ((sub.getDirection() == SubcategoryDirection.INCOME && direction == TransactionDirection.EXPENSE) ||
                    (sub.getDirection() == SubcategoryDirection.EXPENSE && direction == TransactionDirection.INCOME)) {
                throw new SubcategoryDirectionMismatchException(
                        "Subcategory '" + sub.getName() + "' is not compatible with direction " + direction);
            }
        }
    }

    private void validateAssetLink(SaveAssetLinkRequest linkReq, FinancialTransaction tx) {
        // BR-UC014-09: BOILER must belong to the same building as the transaction.
        // Boiler uses a polymorphic ownership pattern (ownerType + ownerId — no direct
        // FK).
        if (linkReq.assetType() == AssetType.BOILER && tx.getBuilding() != null) {
            boilerRepository.findById(linkReq.assetId()).ifPresent(boiler -> {
                Long boilerBuildingId = resolveBuildingIdFromBoiler(boiler);
                if (boilerBuildingId != null && !boilerBuildingId.equals(tx.getBuilding().getId())) {
                    throw new AssetLinkValidationException(
                            "Boiler " + linkReq.assetId() + " does not belong to building " + tx.getBuilding().getId());
                }
            });
        }
    }

    /**
     * Resolves the building ID from a Boiler using its polymorphic ownership.
     * ownerType = BUILDING -> ownerId is directly the building ID
     * ownerType = HOUSING_UNIT -> load the unit to get its building ID
     */
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

    private void reinforceLearning(FinancialTransaction tx) {
        if (tx.getSubcategory() != null && tx.getCounterpartyAccount() != null
                && !tx.getCounterpartyAccount().isBlank()) {
            learningService.reinforceTagRule(tx.getSubcategory().getId(), tx.getCounterpartyAccount());
            int offset = (int) (tx.getAccountingMonth().getYear() * 12L
                    + tx.getAccountingMonth().getMonthValue()
                    - LocalDate.now().getYear() * 12L - LocalDate.now().getMonthValue());
            learningService.reinforceAccountingMonthRule(
                    tx.getSubcategory().getId(), tx.getCounterpartyAccount(), offset);
        }
    }

    private String resolveAssetLabel(AssetType type, Long assetId) {
        return switch (type) {
            case BOILER -> boilerRepository.findById(assetId)
                    .map(b -> b.getBrand() + " " + b.getModel()).orElse("Boiler #" + assetId);
            case FIRE_EXTINGUISHER -> fireExtinguisherRepository.findById(assetId)
                    .map(FireExtinguisher::getIdentificationNumber).orElse("Extinguisher #" + assetId);
            case METER -> meterRepository.findById(assetId)
                    .map(m -> m.getMeterNumber() + " (" + m.getType() + ")").orElse("Meter #" + assetId);
        };
    }

    private FinancialTransactionSummaryDTO toSummaryDTO(FinancialTransaction tx) {
        return new FinancialTransactionSummaryDTO(
                tx.getId(), tx.getReference(), tx.getTransactionDate(), tx.getAccountingMonth(),
                tx.getDirection(), tx.getAmount(), tx.getCounterpartyName(), tx.getStatus(), tx.getSource(),
                tx.getBankAccount() != null ? tx.getBankAccount().getLabel() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getCategory().getName() : null,
                tx.getSubcategory() != null ? tx.getSubcategory().getName() : null,
                tx.getBuilding() != null ? tx.getBuilding().getName() : null,
                tx.getHousingUnit() != null ? tx.getHousingUnit().getUnitNumber() : null,
                tx.getLease() != null ? tx.getLease().getId() : null);
    }

    private FinancialTransactionDTO toDTO(FinancialTransaction tx) {
        List<TransactionAssetLinkDTO> links = tx.getAssetLinks().stream()
                .map(l -> new TransactionAssetLinkDTO(l.getId(), l.getAssetType(), l.getAssetId(),
                        resolveAssetLabel(l.getAssetType(), l.getAssetId()), l.getNotes()))
                .toList();
        return new FinancialTransactionDTO(
                tx.getId(), tx.getReference(), tx.getExternalReference(),
                tx.getTransactionDate(), tx.getValueDate(), tx.getAccountingMonth(),
                tx.getAmount(), tx.getDirection(), tx.getDescription(),
                tx.getCounterpartyName(), tx.getCounterpartyAccount(), tx.getStatus(), tx.getSource(),
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