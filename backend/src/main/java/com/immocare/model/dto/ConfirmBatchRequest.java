package com.immocare.model.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmBatchRequest(@NotNull Long batchId) {}
