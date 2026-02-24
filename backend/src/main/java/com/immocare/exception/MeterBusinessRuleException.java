package com.immocare.exception;

/**
 * Thrown when a business rule is violated during a meter operation.
 * Maps to HTTP 409 Conflict (via GlobalExceptionHandler).
 */
public class MeterBusinessRuleException extends RuntimeException {

    public MeterBusinessRuleException(String message) {
        super(message);
    }
}
