package com.immocare.model.dto;

import com.immocare.model.enums.BankAccountType;
import java.time.LocalDateTime;

public record BankAccountDTO(Long id, String label, String accountNumber,
    BankAccountType type, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {}
