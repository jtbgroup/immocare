package com.immocare.exception;

/** Thrown when an estate name is already in use (case-insensitive). UC004_ESTATE_PLACEHOLDER. */
public class EstateNameTakenException extends RuntimeException {
    public EstateNameTakenException(String name) {
        super("An estate with this name already exists: " + name);
    }
}
