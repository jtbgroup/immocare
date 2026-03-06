package com.immocare.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record PagedTransactionResponse(
    List<FinancialTransactionSummaryDTO> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netBalance
) {}
