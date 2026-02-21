package com.immocare.exception;

/**
 * Exception thrown when attempting to delete a housing unit
 * that still has associated data (rooms, PEB history, rent history, water meter history).
 */
public class HousingUnitHasDataException extends RuntimeException {

  private final long roomCount;

  public HousingUnitHasDataException(Long unitId, long roomCount) {
    super(buildMessage(unitId, roomCount));
    this.roomCount = roomCount;
  }

  public long getRoomCount() {
    return roomCount;
  }

  private static String buildMessage(Long unitId, long roomCount) {
    StringBuilder sb = new StringBuilder("Cannot delete housing unit ").append(unitId).append(".");
    if (roomCount > 0) {
      sb.append(" It has ").append(roomCount).append(" room(s).");
    }
    return sb.toString();
  }
}
