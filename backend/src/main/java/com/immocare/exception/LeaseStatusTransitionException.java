package com.immocare.exception;
public class LeaseStatusTransitionException extends RuntimeException {
    public LeaseStatusTransitionException(String from, String to) {
        super("Cannot transition lease from " + from + " to " + to);
    }
}
