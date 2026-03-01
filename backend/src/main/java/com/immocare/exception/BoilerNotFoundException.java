package com.immocare.exception;

/** Thrown when a boiler is not found — UC011. Maps to HTTP 404. */
public class BoilerNotFoundException extends RuntimeException {

    public BoilerNotFoundException(Long id) {
        super("Boiler not found: " + id);
    }
}
