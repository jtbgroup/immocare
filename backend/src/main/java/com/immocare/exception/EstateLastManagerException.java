package com.immocare.exception;

/** Thrown when an operation would leave an estate without any MANAGER. UC004_ESTATE_PLACEHOLDER BR-UC004_ESTATE_PLACEHOLDER-02. */
public class EstateLastManagerException extends RuntimeException {
    public EstateLastManagerException() {
        super("Cannot remove the last manager of an estate");
    }
}
