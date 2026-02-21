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

  /**
   * Handle BuildingNotFoundException.
   * 
   * @param ex the exception
   * @return 404 Not Found response
   */
  @ExceptionHandler(BuildingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBuildingNotFound(BuildingNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        "Building not found",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Handle BuildingHasUnitsException.
   * 
   * @param ex the exception
   * @return 400 Bad Request response with unit count
   */
  @ExceptionHandler(BuildingHasUnitsException.class)
  public ResponseEntity<Map<String, Object>> handleBuildingHasUnits(
      BuildingHasUnitsException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Cannot delete building");
    error.put("message", ex.getMessage());
    error.put("unitCount", ex.getUnitCount());
    error.put("timestamp", LocalDateTime.now());
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }


  /**
   * Handle HousingUnitNotFoundException.
   */
  @ExceptionHandler(HousingUnitNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleHousingUnitNotFound(HousingUnitNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        "Housing unit not found",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Handle HousingUnitHasDataException — deletion blocked.
   */
  @ExceptionHandler(HousingUnitHasDataException.class)
  public ResponseEntity<Map<String, Object>> handleHousingUnitHasData(
      HousingUnitHasDataException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Cannot delete housing unit");
    error.put("message", ex.getMessage());
    error.put("roomCount", ex.getRoomCount());
    error.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Handle IllegalArgumentException — business rule violations (duplicate unit number, etc.).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(),
        "Business rule violation",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Handle validation errors from @Valid.
   * 
   * @param ex the exception
   * @return 400 Bad Request response with field errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Validation failed");
    response.put("message", "Invalid input data");
    response.put("fieldErrors", errors);
    response.put("timestamp", LocalDateTime.now());
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Handle all other exceptions.
   * 
   * @param ex the exception
   * @return 500 Internal Server Error response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal server error",
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Standard error response structure.
   */
  public record ErrorResponse(
      int status,
      String error,
      String message,
      LocalDateTime timestamp
  ) {
  }
}
