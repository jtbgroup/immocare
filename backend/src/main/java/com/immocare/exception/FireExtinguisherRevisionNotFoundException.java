package com.immocare.exception;

public class FireExtinguisherRevisionNotFoundException extends RuntimeException {

    public FireExtinguisherRevisionNotFoundException(Long id) {
        super("Revision record not found: " + id);
    }
}
