package com.immocare.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FireExtinguisherDTO {

    public record FireExtinguisherResponse(
        Long id,
        Long buildingId,
        Long unitId,
        String unitNumber,
        String identificationNumber,
        String notes,
        List<FireExtinguisherRevisionResponse> revisions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    public record FireExtinguisherRevisionResponse(
        Long id,
        Long fireExtinguisherId,
        java.time.LocalDate revisionDate,
        String notes,
        LocalDateTime createdAt
    ) {}
}
