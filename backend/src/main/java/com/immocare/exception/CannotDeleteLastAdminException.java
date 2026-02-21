package com.immocare.exception;

/**
 * Thrown when deleting the target user would leave the system with no ADMIN (BR-UC007-06).
 */
public class CannotDeleteLastAdminException extends RuntimeException {

    public CannotDeleteLastAdminException() {
        super("Cannot delete the last administrator account");
    }
}
