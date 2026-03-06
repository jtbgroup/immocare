package com.immocare.exception;

public class TransactionValidationException extends RuntimeException {
    public TransactionValidationException(String message) { super(message); }
}
