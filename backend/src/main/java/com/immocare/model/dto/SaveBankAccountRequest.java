package com.immocare.model.dto;

import com.immocare.model.enums.BankAccountType;
import jakarta.validation.constraints.*;

public record SaveBankAccountRequest(
    @NotBlank @Size(max = 100) String label,
    @NotBlank @Size(max = 50)  String accountNumber,
    @NotNull BankAccountType type,
    boolean isActive) {}
