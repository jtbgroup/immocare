package com.immocare.exception;

/**
 * Thrown when a CreateEstateRequest contains a members list but none of the
 * entries has role MANAGER.
 * BR-UC003-02: An estate must always have at least one MANAGER.
 */
public class NoManagerInMembersException extends RuntimeException {

    public NoManagerInMembersException() {
        super("The members list must contain at least one entry with role MANAGER");
    }
}
