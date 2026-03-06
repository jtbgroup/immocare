package com.immocare.service;

import com.immocare.exception.BankAccountDuplicateLabelException;
import com.immocare.exception.BankAccountDuplicateNumberException;
import com.immocare.exception.BankAccountNotFoundException;
import com.immocare.model.dto.BankAccountDTO;
import com.immocare.model.dto.SaveBankAccountRequest;
import com.immocare.model.entity.BankAccount;
import com.immocare.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccountDTO> getAll(boolean activeOnly) {
        List<BankAccount> accounts = activeOnly
            ? bankAccountRepository.findByIsActiveTrueOrderByLabelAsc()
            : bankAccountRepository.findAllByOrderByLabelAsc();
        return accounts.stream().map(this::toDTO).toList();
    }

    @Transactional
    public BankAccountDTO create(SaveBankAccountRequest req) {
        if (bankAccountRepository.existsByLabelIgnoreCase(req.label())) {
            throw new BankAccountDuplicateLabelException("A bank account with this label already exists.");
        }
        if (bankAccountRepository.existsByAccountNumber(req.accountNumber())) {
            throw new BankAccountDuplicateNumberException("This IBAN is already registered.");
        }
        BankAccount account = new BankAccount();
        applyRequest(account, req);
        return toDTO(bankAccountRepository.save(account));
    }

    @Transactional
    public BankAccountDTO update(Long id, SaveBankAccountRequest req) {
        BankAccount account = bankAccountRepository.findById(id)
            .orElseThrow(() -> new BankAccountNotFoundException("Bank account not found: " + id));
        if (bankAccountRepository.existsByLabelIgnoreCaseAndIdNot(req.label(), id)) {
            throw new BankAccountDuplicateLabelException("A bank account with this label already exists.");
        }
        if (bankAccountRepository.existsByAccountNumberAndIdNot(req.accountNumber(), id)) {
            throw new BankAccountDuplicateNumberException("This IBAN is already registered.");
        }
        applyRequest(account, req);
        return toDTO(bankAccountRepository.save(account));
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
