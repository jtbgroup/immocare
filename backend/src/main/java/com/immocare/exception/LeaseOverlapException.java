package com.immocare.exception;
public class LeaseOverlapException extends RuntimeException {
    public LeaseOverlapException(Long unitId) {
        super("An ACTIVE or DRAFT lease already exists for housing unit " + unitId);
    }
}
