package com.immocare.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SavePersonBankAccountRequest(

        @NotBlank(message = "IBAN is required")
        @Size(max = 50)
        @Pattern(
                regexp = "[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}",
                message = "IBAN must be in valid format (e.g. BE32310076563402)"
        )
        String iban,

        @Size(max = 100)
        String label,

        boolean primary
) {}
