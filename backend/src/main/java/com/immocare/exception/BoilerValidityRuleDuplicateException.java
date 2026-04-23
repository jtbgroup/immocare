package com.immocare.exception;

/**
 * Thrown when a boiler service validity rule with the same valid_from date
 * already exists within the estate — UC004_ESTATE_PLACEHOLDER Phase 5.
 * Maps to HTTP 409 Conflict.
 */
public class BoilerValidityRuleDuplicateException extends RuntimeException {

    public BoilerValidityRuleDuplicateException(String message) {
        super(message);
    }
}
