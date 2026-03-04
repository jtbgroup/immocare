package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveFireExtinguisherRequest(
    @NotBlank @Size(max = 50) String identificationNumber,
    Long unitId,
    @Size(max = 2000) String notes
) {}
