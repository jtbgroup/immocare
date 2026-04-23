package com.immocare.exception;

/** Thrown when a user attempts to modify their own membership. UC004_ESTATE_PLACEHOLDER BR-UC004_ESTATE_PLACEHOLDER-03/04. */
public class EstateSelfOperationException extends RuntimeException {
    public EstateSelfOperationException(String message) {
        super(message);
    }
}
