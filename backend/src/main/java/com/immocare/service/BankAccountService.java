package com.immocare.service;

import com.immocare.exception.BankAccountDuplicateLabelException;
import com.immocare.exception.BankAccountDuplicateNumberException;
import com.immocare.exception.BankAccountNotFoundException;
import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.model.dto.BankAccountDTO;
import com.immocare.model.dto.SaveBankAccountRequest;
import com.immocare.model.entity.BankAccount;
import com.immocare.model.entity.Estate;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.EstateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for BankAccount management.
 * UC016 Phase 4: all operations are now scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final EstateRepository estateRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository,
                               EstateRepository estateRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.estateRepository = estateRepository;
    }

    public List<BankAccountDTO> getAll(UUID estateId, boolean activeOnly) {
        List<BankAccount> accounts = activeOnly
                ? bankAccountRepository.findByEstateIdAndIsActiveTrueOrderByLabelAsc(estateId)
                : bankAccountRepository.findByEstateIdOrderByLabelAsc(estateId);
        return accounts.stream().map(this::toDTO).toList();
    }

    @Transactional
    public BankAccountDTO create(UUID estateId, SaveBankAccountRequest req) {
        Estate estate = findEstateOrThrow(estateId);

        if (bankAccountRepository.existsByEstateIdAndLabelIgnoreCase(estateId, req.label())) {
            throw new BankAccountDuplicateLabelException("A bank account with this label already exists.");
        }
        if (bankAccountRepository.existsByEstateIdAndAccountNumber(estateId, req.accountNumber())) {
            throw new BankAccountDuplicateNumberException("This IBAN is already registered.");
        }

        BankAccount account = new BankAccount();
        account.setEstate(estate);
        applyRequest(account, req);
        return toDTO(bankAccountRepository.save(account));
    }

    @Transactional
    public BankAccountDTO update(UUID estateId, Long id, SaveBankAccountRequest req) {
        verifyBankAccountBelongsToEstate(estateId, id);

        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new BankAccountNotFoundException("Bank account not found: " + id));

        if (bankAccountRepository.existsByEstateIdAndLabelIgnoreCaseAndIdNot(estateId, req.label(), id)) {
            throw new BankAccountDuplicateLabelException("A bank account with this label already exists.");
        }
        if (bankAccountRepository.existsByEstateIdAndAccountNumberAndIdNot(estateId, req.accountNumber(), id)) {
            throw new BankAccountDuplicateNumberException("This IBAN is already registered.");
        }

        applyRequest(account, req);
        return toDTO(bankAccountRepository.save(account));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void verifyBankAccountBelongsToEstate(UUID estateId, Long bankAccountId) {
        if (!bankAccountRepository.existsById(bankAccountId)) {
            throw new BankAccountNotFoundException("Bank account not found: " + bankAccountId);
        }
        if (!bankAccountRepository.existsByEstateIdAndId(estateId, bankAccountId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }

    private void applyRequest(BankAccount account, SaveBankAccountRequest req) {
        account.setLabel(req.label());
        account.setAccountNumber(req.accountNumber());
        account.setType(req.type());
        account.setActive(req.isActive());
    }

    private BankAccountDTO toDTO(BankAccount a) {
        return new BankAccountDTO(a.getId(), a.getLabel(), a.getAccountNumber(),
                a.getType(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
