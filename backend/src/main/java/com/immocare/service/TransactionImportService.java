package com.immocare.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
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
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.ImportBatchRepository;
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

    /**
     * Parse and persist a file using the specified parser strategy.
     * Any unexpected exception rolls back the entire batch.
     *
     * @param file          uploaded file (CSV or PDF)
     * @param parserCode    code identifying the parser (e.g.
     *                      "keytrade-pdf-20260301")
     * @param bankAccountId own bank account to link transactions to (optional)
     * @param currentUser   authenticated user
     */
    @Transactional
    public ImportBatchResultDTO importFile(
            MultipartFile file,
            String parserCode,
            Long bankAccountId,
            AppUser currentUser) throws ParseException {

        // ── 1. Resolve parser ─────────────────────────────────────────────
        TransactionParser parser = parserRegistry.getOrThrow(parserCode);

        BankAccount bankAccount = bankAccountId != null
                ? bankAccountRepo.findById(bankAccountId).orElse(null)
                : null;

        // ── 2. Parse file ─────────────────────────────────────────────────
        List<ParsedTransaction> parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("File parsing failed: " + e.getMessage(), e);
        }

        // ── 3. Create import batch ────────────────────────────────────────
        ImportBatch batch = new ImportBatch();
        batch.setFilename(file.getOriginalFilename());
        batch.setTotalRows(parsed.size());
        batch.setCreatedBy(currentUser);
        importBatchRepo.save(batch);

        // ── 4. Persist transactions ───────────────────────────────────────
        int imported = 0, duplicates = 0;

        for (ParsedTransaction p : parsed) {

            // Duplicate check
            if (p.getCounterpartyAccount() != null && !p.getCounterpartyAccount().isBlank()) {
                if (transactionRepo.existsByExternalReferenceAndTransactionDateAndAmount(
                        p.getCounterpartyAccount(), p.getTransactionDate(), p.getAmount())) {
                    log.debug("Duplicate skipped: counterpartyAccount={} date={} amount={}",
                            p.getCounterpartyAccount(), p.getTransactionDate(), p.getAmount());
                    duplicates++;
                    continue;
                }
            }

            // Log full details before save so any crash is traceable
            log.debug("Saving row {}: date={} amount={} direction={} counterparty={}",
                    p.getRowNumber(), p.getTransactionDate(), p.getAmount(),
                    p.getDirection(), p.getCounterpartyName());

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
            tx.setStatus(TransactionStatus.DRAFT);
            tx.setSource(TransactionSource.IMPORT);
            tx.setImportBatch(batch);
            tx.setBankAccount(bankAccount);

            tx.setDirection(p.getDirection() == ParsedTransaction.Direction.INCOME
                    ? TransactionDirection.INCOME
                    : TransactionDirection.EXPENSE);

            transactionRepo.save(tx);
            imported++;
        }

        // ── 5. Update batch counts ────────────────────────────────────────
        batch.setImportedCount(imported);
        batch.setDuplicateCount(duplicates);
        batch.setErrorCount(0);
        importBatchRepo.save(batch);

        log.info("Import complete: batchId={} imported={} duplicates={}", batch.getId(), imported, duplicates);

        return new ImportBatchResultDTO(
                batch.getId(), parsed.size(), imported, duplicates, 0, List.of());
    }
}