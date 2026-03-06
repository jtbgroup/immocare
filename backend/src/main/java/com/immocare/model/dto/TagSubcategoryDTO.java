package com.immocare.model.dto;

import com.immocare.model.enums.SubcategoryDirection;
import java.time.LocalDateTime;

public record TagSubcategoryDTO(Long id, Long categoryId, String categoryName,
    String name, SubcategoryDirection direction, String description,
    long usageCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
