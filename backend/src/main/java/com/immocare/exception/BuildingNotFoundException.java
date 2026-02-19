package com.immocare.exception;

/**
 * Exception thrown when a building is not found in the system.
 */
public class BuildingNotFoundException extends RuntimeException {

  public BuildingNotFoundException(Long id) {
    super("Building not found with id: " + id);
  }

  public BuildingNotFoundException(String message) {
    super(message);
  }
}
