package com.immocare.exception;

/**
 * Thrown when valid_until is not after score_date.
 * UC005 - BR-UC005-04.
 */
public class InvalidValidityPeriodException extends RuntimeException {

    public InvalidValidityPeriodException(String message) {
        super(message);
    }
}
