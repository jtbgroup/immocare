package com.immocare.model.dto;

import java.time.LocalDateTime;

public record TagCategoryDTO(Long id, String name, String description,
    int subcategoryCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
