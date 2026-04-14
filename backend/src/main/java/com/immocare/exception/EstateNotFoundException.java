package com.immocare.exception;

import java.util.UUID;

/** Thrown when an estate cannot be found by the given ID. UC016. */
public class EstateNotFoundException extends RuntimeException {
    public EstateNotFoundException(UUID id) {
        super("Estate not found: " + id);
    }
}
