package com.immocare.exception;

/**
 * Thrown when an email address is already in use (BR-UC007-03).
 */
public class EmailTakenException extends RuntimeException {

    public EmailTakenException(String email) {
        super("Email already in use: " + email);
    }
}
