package com.immocare.controller;

import com.immocare.model.dto.BankAccountDTO;
import com.immocare.model.dto.SaveBankAccountRequest;
import com.immocare.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/api/v1/bank-accounts")
    public List<BankAccountDTO> getAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return bankAccountService.getAll(activeOnly);
    }

    @PostMapping("/api/v1/bank-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountDTO create(@Valid @RequestBody SaveBankAccountRequest req) {
        return bankAccountService.create(req);
    }

    @PutMapping("/api/v1/bank-accounts/{id}")
    public BankAccountDTO update(@PathVariable Long id, @Valid @RequestBody SaveBankAccountRequest req) {
        return bankAccountService.update(id, req);
    }
}
