package com.immocare.model.dto;

public record SubcategorySuggestionDTO(
    Long subcategoryId, String subcategoryName,
    Long categoryId, String categoryName, int confidence
) {}
