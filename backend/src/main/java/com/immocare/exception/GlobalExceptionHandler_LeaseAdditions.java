package com.immocare.exception;

// ================================================================
// ADD THESE HANDLERS TO YOUR EXISTING GlobalExceptionHandler.java
// ================================================================
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class GlobalExceptionHandler_LeaseAdditions {

    @ExceptionHandler(LeaseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLeaseNotFound(LeaseNotFoundException ex) {
        return error(404, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(LeaseOverlapException.class)
    public ResponseEntity<Map<String, Object>> handleLeaseOverlap(LeaseOverlapException ex) {
        return error(409, "LEASE_OVERLAP", ex.getMessage());
    }

    @ExceptionHandler(LeaseNotEditableException.class)
    public ResponseEntity<Map<String, Object>> handleLeaseNotEditable(LeaseNotEditableException ex) {
        return error(422, "LEASE_NOT_EDITABLE", ex.getMessage());
    }

    @ExceptionHandler(LeaseStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleLeaseStatusTransition(LeaseStatusTransitionException ex) {
        return error(422, "INVALID_STATUS_TRANSITION", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(int status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
