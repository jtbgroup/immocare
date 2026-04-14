package com.immocare.exception;

/** Thrown when a user is not a member of the given estate. UC016. */
public class EstateMemberNotFoundException extends RuntimeException {
    public EstateMemberNotFoundException() {
        super("User is not a member of this estate");
    }
}
