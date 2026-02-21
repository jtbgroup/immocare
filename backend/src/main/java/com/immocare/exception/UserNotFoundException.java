package com.immocare.exception;

/**
 * Thrown when a requested user does not exist.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
