package com.immocare.model.dto;

import java.util.List;
public record PagedTransactionResponse(
    List<FinancialTransactionSummaryDTO> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
