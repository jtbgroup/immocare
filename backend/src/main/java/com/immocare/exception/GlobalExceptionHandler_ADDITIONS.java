// ─── ADD these two handlers inside GlobalExceptionHandler ──────────────────
// Place them after the handleBuildingHasUnits handler.

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

// ─── Also add to BuildingService.deleteBuilding() ──────────────────────────
// Replace the stub comment with:
//   long unitCount = housingUnitRepository.countByBuildingId(id);
// and inject HousingUnitRepository in the constructor.
