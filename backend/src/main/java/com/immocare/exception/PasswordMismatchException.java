package com.immocare.exception;

/**
 * Thrown when {@code password} and {@code confirmPassword} do not match.
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Passwords do not match");
    }
}
