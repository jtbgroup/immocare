package com.immocare.model.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddRevisionRequest(
    @NotNull LocalDate revisionDate,
    @Size(max = 2000) String notes
) {}
