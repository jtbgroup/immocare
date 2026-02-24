package com.immocare.exception;
public class LeaseNotEditableException extends RuntimeException {
    public LeaseNotEditableException(Long id, String status) {
        super("Lease " + id + " cannot be edited because it is " + status);
    }
}
