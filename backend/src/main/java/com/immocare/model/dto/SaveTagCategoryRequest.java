package com.immocare.model.dto;

import jakarta.validation.constraints.*;

public record SaveTagCategoryRequest(@NotBlank @Size(max = 100) String name, String description) {}
