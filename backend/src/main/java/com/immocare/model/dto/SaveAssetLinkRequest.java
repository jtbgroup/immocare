package com.immocare.model.dto;

import com.immocare.model.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for linking a transaction to a physical asset.
 * housingUnitId and buildingId are intentionally absent — they are resolved
 * server-side from the asset's ownership (BR-UC015-14).
 * amount is optional: null = full transaction amount attributed to this asset.
 * When multiple links exist, the sum of non-null amounts must not exceed the
 * transaction total (BR-UC015-15).
 */
public record SaveAssetLinkRequest(
        @NotNull AssetType assetType,
        @NotNull Long assetId,
        @DecimalMin(value = "0.01") BigDecimal amount,
        String notes
) {}
