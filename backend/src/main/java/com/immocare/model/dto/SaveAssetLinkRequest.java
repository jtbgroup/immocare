package com.immocare.model.dto;

import com.immocare.model.enums.AssetType;
import jakarta.validation.constraints.NotNull;

public record SaveAssetLinkRequest(@NotNull AssetType assetType, @NotNull Long assetId, String notes) {}
