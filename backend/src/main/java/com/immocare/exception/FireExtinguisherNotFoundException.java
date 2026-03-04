package com.immocare.exception;

public class FireExtinguisherNotFoundException extends RuntimeException {

    public FireExtinguisherNotFoundException(Long id) {
        super("Fire extinguisher not found: " + id);
    }
}
