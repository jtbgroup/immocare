package com.immocare.exception;

/**
 * Thrown when a PEB score entry cannot be found by ID.
 */
public class PebScoreNotFoundException extends RuntimeException {

    public PebScoreNotFoundException(Long id) {
        super("PEB score not found with id: " + id);
    }
}
