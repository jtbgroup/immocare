package com.immocare.exception;

/** Thrown when an operation would leave an estate without any MANAGER. UC016 BR-UC016-02. */
public class EstateLastManagerException extends RuntimeException {
    public EstateLastManagerException() {
        super("Cannot remove the last manager of an estate");
    }
}
