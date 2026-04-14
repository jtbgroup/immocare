package com.immocare.exception;

/** Thrown when a user tries to access an estate they are not a member of. UC016. */
public class EstateAccessDeniedException extends RuntimeException {
    public EstateAccessDeniedException() {
        super("Access denied to this estate");
    }
}
