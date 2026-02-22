package com.immocare.exception;

/**
 * Thrown when a Room is not found by the given ID.
 * UC003 - Manage Rooms.
 */
public class RoomNotFoundException extends RuntimeException {

  public RoomNotFoundException(Long id) {
    super("Room not found with id: " + id);
  }
}
