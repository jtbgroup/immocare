package com.immocare.exception;

public class FireExtinguisherDuplicateNumberException extends RuntimeException {

    public FireExtinguisherDuplicateNumberException() {
        super("An extinguisher with this identification number already exists in this building");
    }
}
