package com.immocare.exception;

/**
 * Exception thrown when attempting to delete a housing unit
 * that still has associated data (rooms or equipment).
 */
public class HousingUnitHasDataException extends RuntimeException {

  private final long roomCount;
  private final long fireExtinguisherCount;

  public HousingUnitHasDataException(Long unitId, long roomCount, long fireExtinguisherCount) {
    super(buildMessage(unitId, roomCount, fireExtinguisherCount));
    this.roomCount = roomCount;
    this.fireExtinguisherCount = fireExtinguisherCount;
  }

  public long getRoomCount() {
    return roomCount;
  }

  public long getFireExtinguisherCount() {
    return fireExtinguisherCount;
  }

  private static String buildMessage(Long unitId, long roomCount, long fireExtinguisherCount) {
    StringBuilder sb = new StringBuilder("Cannot delete housing unit ").append(unitId).append(".");
    if (roomCount > 0) {
      sb.append(" It has ").append(roomCount).append(" room(s).");
    }
    if (fireExtinguisherCount > 0) {
      sb.append(" It has ").append(fireExtinguisherCount).append(" fire extinguisher(s).");
    }
    return sb.toString();
  }
}
