package com.immocare.model.dto;

import java.time.LocalDate;

public record AccountingMonthSuggestionDTO(LocalDate accountingMonth, int confidence) {}
