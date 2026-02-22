package com.immocare.exception;

/**
 * Thrown when a PEB score date is invalid (e.g. in the future).
 * UC004 - BR-UC004-03.
 */
public class InvalidPebScoreDateException extends RuntimeException {

    public InvalidPebScoreDateException(String message) {
        super(message);
    }
}
