package com.immocare.model.dto;

import com.immocare.model.enums.AssetType;

public record TransactionAssetLinkDTO(Long id, AssetType assetType, Long assetId,
    String assetLabel, String notes) {}
