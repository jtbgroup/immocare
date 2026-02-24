package com.immocare.model.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for removing (closing) an active meter without replacement.
 */
public record RemoveMeterRequest(

        @NotNull(message = "End date is required")
        LocalDate endDate

) {}
