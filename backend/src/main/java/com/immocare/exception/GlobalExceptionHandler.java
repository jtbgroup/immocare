package com.immocare.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error response format across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BuildingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBuildingNotFound(BuildingNotFoundException ex) {
    return notFound("Building not found", ex.getMessage());
  }

  @ExceptionHandler(BuildingHasUnitsException.class)
  public ResponseEntity<Map<String, Object>> handleBuildingHasUnits(BuildingHasUnitsException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Cannot delete building");
    error.put("message", ex.getMessage());
    error.put("unitCount", ex.getUnitCount());
    error.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HousingUnitNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleHousingUnitNotFound(HousingUnitNotFoundException ex) {
    return notFound("Housing unit not found", ex.getMessage());
  }

  @ExceptionHandler(HousingUnitHasDataException.class)
  public ResponseEntity<Map<String, Object>> handleHousingUnitHasData(HousingUnitHasDataException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Cannot delete housing unit");
    error.put("message", ex.getMessage());
    error.put("roomCount", ex.getRoomCount());
    error.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  // ─── UC004 - PEB Scores ────────────────────────────────────────────────────

  /** BR-UC004-03: score_date cannot be in the future. */
  @ExceptionHandler(InvalidPebScoreDateException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPebScoreDate(InvalidPebScoreDateException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(), "Invalid PEB score date", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /** BR-UC004-04: valid_until must be after score_date. */
  @ExceptionHandler(InvalidValidityPeriodException.class)
  public ResponseEntity<ErrorResponse> handleInvalidValidityPeriod(InvalidValidityPeriodException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(), "Invalid validity period", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(PebScoreNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePebScoreNotFound(PebScoreNotFoundException ex) {
    return notFound("PEB score not found", ex.getMessage());
  }

  // ─── UC008 - Meters ───────────────────────────────────────────────────────

  /**
   * Meter not found or already closed → 409 Conflict.
   * Using 409 instead of 404 to signal a state conflict (meter exists but is
   * closed).
   */
  @ExceptionHandler(MeterNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleMeterNotFound(MeterNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(), "Meter not found or already closed", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Meter business rule violation → 409 Conflict.
   * E.g., startDate in future, endDate before startDate, newStartDate before
   * currentStartDate.
   */
  @ExceptionHandler(MeterBusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleMeterBusinessRule(MeterBusinessRuleException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(), "Meter business rule violation", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  // ─── Generic ──────────────────────────────────────────────────────────────

  // --- IllegalArgumentException (nationalId duplicate) → 409 ---
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 409);
    body.put("error", "DUPLICATE");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      errors.put(fieldName, error.getDefaultMessage());
    });
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Validation failed");
    response.put("message", "Invalid input data");
    response.put("fieldErrors", errors);
    response.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private ResponseEntity<ErrorResponse> notFound(String error, String message) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), error, message, LocalDateTime.now()));
  }

  public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
  }

  // --- PersonNotFoundException → 404 ---
  @ExceptionHandler(PersonNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handlePersonNotFound(PersonNotFoundException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 404);
    body.put("error", "NOT_FOUND");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  // --- PersonReferencedException → 409 ---
  @ExceptionHandler(PersonReferencedException.class)
  public ResponseEntity<Map<String, Object>> handlePersonReferenced(PersonReferencedException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 409);
    body.put("error", "PERSON_REFERENCED");
    body.put("message", "This person cannot be deleted because they are still referenced.");
    body.put("ownedBuildings", ex.getOwnedBuildings());
    body.put("ownedUnits", ex.getOwnedUnits());
    body.put("activeLeases", ex.getActiveLeases());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

}
