package com.immocare.exception;

/**
 * Exception thrown when a housing unit is not found in the system.
 */
public class HousingUnitNotFoundException extends RuntimeException {

  public HousingUnitNotFoundException(Long id) {
    super("Housing unit not found with id: " + id);
  }
}
