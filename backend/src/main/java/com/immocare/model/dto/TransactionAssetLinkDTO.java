package com.immocare.model.dto;

import com.immocare.model.enums.AssetType;
import java.math.BigDecimal;

/**
 * Response DTO for a transaction asset link.
 * housingUnitId, unitNumber, buildingId, buildingName are resolved server-side
 * from the asset's ownership — they are never provided by the client.
 */
public record TransactionAssetLinkDTO(
        Long id,
        AssetType assetType,
        Long assetId,
        String assetLabel,
        Long housingUnitId,
        String unitNumber,
        Long buildingId,
        String buildingName,
        BigDecimal amount,
        String notes
) {}
