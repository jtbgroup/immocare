package com.immocare.exception;

/**
 * Thrown when a meter is not found or is already closed.
 * Maps to HTTP 409 Conflict (via GlobalExceptionHandler).
 */
public class MeterNotFoundException extends RuntimeException {

    public MeterNotFoundException(Long id) {
        super("Meter not found or already closed: " + id);
    }
}
