package com.immocare.model.dto;

import com.immocare.model.enums.SubcategoryDirection;
import jakarta.validation.constraints.*;

public record SaveTagSubcategoryRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max = 100) String name,
    @NotNull SubcategoryDirection direction,
    String description) {}
