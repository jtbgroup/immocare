package com.immocare.exception;

/**
 * Exception thrown when attempting to delete a building that contains housing units.
 * Business Rule BR-UC001-03: Cannot delete building with housing units.
 */
public class BuildingHasUnitsException extends RuntimeException {

  private final Long buildingId;
  private final long unitCount;

  public BuildingHasUnitsException(Long buildingId, long unitCount) {
    super(String.format("Cannot delete building. Building has %d housing unit(s)", unitCount));
    this.buildingId = buildingId;
    this.unitCount = unitCount;
  }

  public Long getBuildingId() {
    return buildingId;
  }

  public long getUnitCount() {
    return unitCount;
  }
}
