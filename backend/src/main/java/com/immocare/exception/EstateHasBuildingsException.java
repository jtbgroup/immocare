package com.immocare.exception;

/** Thrown when attempting to delete an estate that still contains buildings. UC016 BR-UC016-09. */
public class EstateHasBuildingsException extends RuntimeException {
    private final int buildingCount;

    public EstateHasBuildingsException(int buildingCount) {
        super("Cannot delete estate: " + buildingCount + " building(s) exist");
        this.buildingCount = buildingCount;
    }

    public int getBuildingCount() { return buildingCount; }
}
