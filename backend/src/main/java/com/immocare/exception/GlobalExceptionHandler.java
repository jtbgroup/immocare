package com.immocare.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error response format across the application.
 *
 * UC004_ESTATE_PLACEHOLDER Phase 1: added handlers for all estate-related
 * exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ─── UC001 - Buildings ────────────────────────────────────────────────────

  @ExceptionHandler(BuildingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBuildingNotFound(BuildingNotFoundException ex) {
    return notFound("Building not found", ex.getMessage());
  }

  @ExceptionHandler(BuildingHasUnitsException.class)
  public ResponseEntity<Map<String, Object>> handleBuildingHasUnits(BuildingHasUnitsException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Cannot delete building");
    body.put("message", ex.getMessage());
    body.put("unitCount", ex.getUnitCount());
    body.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  // ─── UC002 - Housing Units ────────────────────────────────────────────────

  @ExceptionHandler(HousingUnitNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleHousingUnitNotFound(HousingUnitNotFoundException ex) {
    return notFound("Housing unit not found", ex.getMessage());
  }

  @ExceptionHandler(HousingUnitHasDataException.class)
  public ResponseEntity<Map<String, Object>> handleHousingUnitHasData(HousingUnitHasDataException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Cannot delete housing unit");
    body.put("message", ex.getMessage());
    body.put("roomCount", ex.getRoomCount());
    body.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  // ─── UC004 - Persons ─────────────────────────────────────────────────────

  @ExceptionHandler(PersonNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handlePersonNotFound(PersonNotFoundException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 404);
    body.put("error", "NOT_FOUND");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

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

  // ─── UC005 - PEB Scores ───────────────────────────────────────────────────

  @ExceptionHandler(InvalidPebScoreDateException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPebScoreDate(InvalidPebScoreDateException ex) {
    return badRequest("Invalid PEB score date", ex.getMessage());
  }

  @ExceptionHandler(InvalidValidityPeriodException.class)
  public ResponseEntity<ErrorResponse> handleInvalidValidityPeriod(InvalidValidityPeriodException ex) {
    return badRequest("Invalid validity period", ex.getMessage());
  }

  @ExceptionHandler(PebScoreNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePebScoreNotFound(PebScoreNotFoundException ex) {
    return notFound("PEB score not found", ex.getMessage());
  }

  // ─── UC009 - Meters ───────────────────────────────────────────────────────

  @ExceptionHandler(MeterNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleMeterNotFound(MeterNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(), "Meter not found or already closed", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(MeterBusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleMeterBusinessRule(MeterBusinessRuleException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.CONFLICT.value(), "Meter business rule violation", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  // ─── UC011 - Leases ───────────────────────────────────────────────────────

  @ExceptionHandler(LeaseNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleLeaseNotFound(LeaseNotFoundException ex) {
    return mapError(404, "NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(LeaseOverlapException.class)
  public ResponseEntity<Map<String, Object>> handleLeaseOverlap(LeaseOverlapException ex) {
    return mapError(409, "LEASE_OVERLAP", ex.getMessage());
  }

  @ExceptionHandler(LeaseNotEditableException.class)
  public ResponseEntity<Map<String, Object>> handleLeaseNotEditable(LeaseNotEditableException ex) {
    return mapError(422, "LEASE_NOT_EDITABLE", ex.getMessage());
  }

  @ExceptionHandler(LeaseStatusTransitionException.class)
  public ResponseEntity<Map<String, Object>> handleLeaseStatusTransition(LeaseStatusTransitionException ex) {
    return mapError(422, "INVALID_STATUS_TRANSITION", ex.getMessage());
  }

  // ─── UC012 - Boilers ─────────────────────────────────────────────────────

  @ExceptionHandler(BoilerNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBoilerNotFound(BoilerNotFoundException ex) {
    return notFound("Boiler not found", ex.getMessage());
  }

  // ─── UC014 - Platform Configuration ──────────────────────────────────────

  @ExceptionHandler(PlatformConfigNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePlatformConfigNotFound(PlatformConfigNotFoundException ex) {
    return notFound("Platform configuration key not found", ex.getMessage());
  }

  // ─── UC013 - Fire Extinguishers ───────────────────────────────────────────

  @ExceptionHandler(FireExtinguisherNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleFireExtinguisherNotFound(FireExtinguisherNotFoundException ex) {
    return notFound("Fire extinguisher not found", ex.getMessage());
  }

  @ExceptionHandler(FireExtinguisherRevisionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleFireExtinguisherRevisionNotFound(
      FireExtinguisherRevisionNotFoundException ex) {
    return notFound("Revision record not found", ex.getMessage());
  }

  @ExceptionHandler(FireExtinguisherDuplicateNumberException.class)
  public ResponseEntity<ErrorResponse> handleFireExtinguisherDuplicate(FireExtinguisherDuplicateNumberException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "DUPLICATE", ex.getMessage(), LocalDateTime.now()));
  }

  // ─── UC015 - Financial Transactions ──────────────────────────────────────

  @ExceptionHandler(TransactionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
    return notFound("Transaction not found", ex.getMessage());
  }

  @ExceptionHandler(TransactionNotEditableException.class)
  public ResponseEntity<ErrorResponse> handleTransactionNotEditable(TransactionNotEditableException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse(422, "TRANSACTION_NOT_EDITABLE", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(TransactionValidationException.class)
  public ResponseEntity<ErrorResponse> handleTransactionValidation(TransactionValidationException ex) {
    return badRequest("Transaction validation error", ex.getMessage());
  }

  @ExceptionHandler(SubcategoryDirectionMismatchException.class)
  public ResponseEntity<ErrorResponse> handleSubcategoryDirectionMismatch(SubcategoryDirectionMismatchException ex) {
    return badRequest("Subcategory direction mismatch", ex.getMessage());
  }

  @ExceptionHandler(SubcategoryInUseException.class)
  public ResponseEntity<ErrorResponse> handleSubcategoryInUse(SubcategoryInUseException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "SUBCATEGORY_IN_USE", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(SubcategoryNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSubcategoryNotFound(SubcategoryNotFoundException ex) {
    return notFound("Subcategory not found", ex.getMessage());
  }

  @ExceptionHandler(CategoryNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex) {
    return notFound("Category not found", ex.getMessage());
  }

  @ExceptionHandler(CategoryHasSubcategoriesException.class)
  public ResponseEntity<ErrorResponse> handleCategoryHasSubcategories(CategoryHasSubcategoriesException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "CATEGORY_HAS_SUBCATEGORIES", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(BankAccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountNotFound(BankAccountNotFoundException ex) {
    return notFound("Bank account not found", ex.getMessage());
  }

  @ExceptionHandler(BankAccountDuplicateLabelException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountDuplicateLabel(BankAccountDuplicateLabelException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "DUPLICATE_LABEL", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(BankAccountDuplicateNumberException.class)
  public ResponseEntity<ErrorResponse> handleBankAccountDuplicateNumber(BankAccountDuplicateNumberException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "DUPLICATE_IBAN", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(ImportBatchNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleImportBatchNotFound(ImportBatchNotFoundException ex) {
    return notFound("Import batch not found", ex.getMessage());
  }

  @ExceptionHandler(AssetLinkValidationException.class)
  public ResponseEntity<ErrorResponse> handleAssetLinkValidation(AssetLinkValidationException ex) {
    return badRequest("Asset link validation error", ex.getMessage());
  }

  // ─── UC0XX - Users ────────────────────────────────────────────────────────

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    return notFound("User not found", ex.getMessage());
  }

  @ExceptionHandler(EmailTakenException.class)
  public ResponseEntity<ErrorResponse> handleEmailTaken(EmailTakenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "EMAIL_TAKEN", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(UsernameTakenException.class)
  public ResponseEntity<ErrorResponse> handleUsernameTaken(UsernameTakenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "USERNAME_TAKEN", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(CannotDeleteLastAdminException.class)
  public ResponseEntity<ErrorResponse> handleCannotDeleteLastAdmin(CannotDeleteLastAdminException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "LAST_PLATFORM_ADMIN", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(CannotDeleteSelfException.class)
  public ResponseEntity<ErrorResponse> handleCannotDeleteSelf(CannotDeleteSelfException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "CANNOT_DELETE_SELF", ex.getMessage(), LocalDateTime.now()));
  }

  // ─── UC004_ESTATE_PLACEHOLDER - Estates
  // ──────────────────────────────────────────────────────

  @ExceptionHandler(EstateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEstateNotFound(EstateNotFoundException ex) {
    return notFound("Estate not found", ex.getMessage());
  }

  @ExceptionHandler(EstateNameTakenException.class)
  public ResponseEntity<ErrorResponse> handleEstateNameTaken(EstateNameTakenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "ESTATE_NAME_TAKEN",
            "An estate with this name already exists", LocalDateTime.now()));
  }

  @ExceptionHandler(EstateHasBuildingsException.class)
  public ResponseEntity<Map<String, Object>> handleEstateHasBuildings(EstateHasBuildingsException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 409);
    body.put("error", "ESTATE_HAS_BUILDINGS");
    body.put("message", ex.getMessage());
    body.put("buildingCount", ex.getBuildingCount());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(EstateMemberNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEstateMemberNotFound(EstateMemberNotFoundException ex) {
    return notFound("Estate member not found", ex.getMessage());
  }

  @ExceptionHandler(EstateMemberAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEstateMemberAlreadyExists(EstateMemberAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "ESTATE_MEMBER_EXISTS", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(EstateLastManagerException.class)
  public ResponseEntity<ErrorResponse> handleEstateLastManager(EstateLastManagerException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "ESTATE_LAST_MANAGER", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(EstateSelfOperationException.class)
  public ResponseEntity<ErrorResponse> handleEstateSelfOperation(EstateSelfOperationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "ESTATE_SELF_OPERATION", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(EstateAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleEstateAccessDenied(EstateAccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "ESTATE_ACCESS_DENIED", ex.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(BoilerValidityRuleDuplicateException.class)
  public ResponseEntity<ErrorResponse> handleBoilerValidityRuleDuplicate(
      BoilerValidityRuleDuplicateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "BOILER_VALIDITY_RULE_DUPLICATE",
            ex.getMessage(), LocalDateTime.now()));
  }

  // HTTP 422 is used here rather than 409 because the estate does not yet exist —
  // this is a request validation failure, not a state conflict.
  // If your project uses 409 consistently for all estate errors, change to
  // CONFLICT.
  @ExceptionHandler(NoManagerInMembersException.class)
  public ResponseEntity<ErrorResponse> handleNoManagerInMembers(NoManagerInMembersException ex) {
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .body(new ErrorResponse(422, "NO_MANAGER_IN_MEMBERS", ex.getMessage(), LocalDateTime.now()));
  }

  // ─── Generic ──────────────────────────────────────────────────────────────

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return mapError(409, "DUPLICATE", ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      fieldErrors.put(fieldName, error.getDefaultMessage());
    });
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Validation failed");
    body.put("message", "Invalid input data");
    body.put("fieldErrors", fieldErrors);
    body.put("timestamp", LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(UnexpectedRollbackException.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedRollback(UnexpectedRollbackException ex) {
    Throwable rootCause = ex;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Import failed",
        rootCause.getMessage(),
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private ResponseEntity<ErrorResponse> notFound(String error, String message) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), error, message, LocalDateTime.now()));
  }

  private ResponseEntity<ErrorResponse> badRequest(String error, String message) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), error, message, LocalDateTime.now()));
  }

  private ResponseEntity<Map<String, Object>> mapError(int status, String code, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status);
    body.put("error", code);
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }

  public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
  }
}
