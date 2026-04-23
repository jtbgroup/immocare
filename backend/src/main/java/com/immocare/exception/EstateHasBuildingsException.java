package com.immocare.exception;

/** Thrown when attempting to delete an estate that still contains buildings. UC004_ESTATE_PLACEHOLDER BR-UC004_ESTATE_PLACEHOLDER-09. */
public class EstateHasBuildingsException extends RuntimeException {
    private final int buildingCount;

    public EstateHasBuildingsException(int buildingCount) {
        super("Cannot delete estate: " + buildingCount + " building(s) exist");
        this.buildingCount = buildingCount;
    }

    public int getBuildingCount() { return buildingCount; }
}
