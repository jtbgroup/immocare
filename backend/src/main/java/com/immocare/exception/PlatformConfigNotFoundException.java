package com.immocare.exception;

/** Thrown when a platform config key is not found — UC014. Maps to HTTP 404. */
public class PlatformConfigNotFoundException extends RuntimeException {

    public PlatformConfigNotFoundException(String key) {
        super("Platform configuration key not found: " + key);
    }
}
