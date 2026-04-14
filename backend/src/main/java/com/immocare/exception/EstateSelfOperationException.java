package com.immocare.exception;

/** Thrown when a user attempts to modify their own membership. UC016 BR-UC016-03/04. */
public class EstateSelfOperationException extends RuntimeException {
    public EstateSelfOperationException(String message) {
        super(message);
    }
}
