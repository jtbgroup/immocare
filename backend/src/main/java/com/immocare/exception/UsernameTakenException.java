package com.immocare.exception;

/**
 * Thrown when a username is already taken (BR-UC008-02).
 */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username already exists: " + username);
    }
}
