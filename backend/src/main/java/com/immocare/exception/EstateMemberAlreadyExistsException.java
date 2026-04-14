package com.immocare.exception;

/** Thrown when a user is already a member of an estate. UC016. */
public class EstateMemberAlreadyExistsException extends RuntimeException {
    public EstateMemberAlreadyExistsException() {
        super("This user is already a member of this estate");
    }
}
