package com.immocare.model.entity;

import com.immocare.model.enums.AssetType;

/**
 * Common ownership contract for equipment with type-specific lifecycles.
 */
public interface PropertyEquipment {

    AssetType getAssetType();

    String getOwnerType();

    Long getOwnerId();
}
