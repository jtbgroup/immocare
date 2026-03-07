package com.immocare.model.dto;

import java.time.LocalDateTime;

public record PersonBankAccountDTO(
        Long id,
        Long personId,
        String iban,
        String label,
        boolean primary,
        LocalDateTime createdAt
) {}
