package com.immocare.exception;

/**
 * Thrown when an admin attempts to delete their own account (BR-UC007-05).
 */
public class CannotDeleteSelfException extends RuntimeException {

    public CannotDeleteSelfException() {
        super("You cannot delete your own account");
    }
}
