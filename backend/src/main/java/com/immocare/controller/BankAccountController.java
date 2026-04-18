package com.immocare.controller;

import com.immocare.model.dto.BankAccountDTO;
import com.immocare.model.dto.SaveBankAccountRequest;
import com.immocare.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for BankAccount management.
 * UC016 Phase 4: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/bank-accounts
 *   POST   /api/v1/estates/{estateId}/bank-accounts
 *   PUT    /api/v1/estates/{estateId}/bank-accounts/{id}
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/bank-accounts")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public List<BankAccountDTO> getAll(
            @PathVariable UUID estateId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return bankAccountService.getAll(estateId, activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public BankAccountDTO create(
            @PathVariable UUID estateId,
            @Valid @RequestBody SaveBankAccountRequest req) {
        return bankAccountService.create(estateId, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public BankAccountDTO update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody SaveBankAccountRequest req) {
        return bankAccountService.update(estateId, id, req);
    }
}
