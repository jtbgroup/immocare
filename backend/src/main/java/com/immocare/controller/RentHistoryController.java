package com.immocare.controller;

import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.service.RentHistoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for UC005 — Manage Rents.
 *
 * <p>Endpoints:
 * <pre>
 *   GET  /api/v1/housing-units/{unitId}/rents          → full history
 *   GET  /api/v1/housing-units/{unitId}/rents/current  → current rent
 *   POST /api/v1/housing-units/{unitId}/rents          → set or update rent
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/housing-units/{unitId}/rents")
@PreAuthorize("hasRole('ADMIN')")
public class RentHistoryController {

    private final RentHistoryService rentHistoryService;

    public RentHistoryController(RentHistoryService rentHistoryService) {
        this.rentHistoryService = rentHistoryService;
    }

    /**
     * US023 — Returns the full rent history for a unit, newest first.
     */
    @GetMapping
    public ResponseEntity<List<RentHistoryDTO>> getRentHistory(@PathVariable Long unitId) {
        return ResponseEntity.ok(rentHistoryService.getRentHistory(unitId));
    }

    /**
     * US023 AC1 — Returns the current (active) rent for a unit.
     * Returns 204 No Content when no rent has been set yet.
     */
    @GetMapping("/current")
    public ResponseEntity<RentHistoryDTO> getCurrentRent(@PathVariable Long unitId) {
        return rentHistoryService.getCurrentRent(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * US021 / US022 — Sets the initial rent or updates the existing rent.
     * The service detects which flow applies.
     */
    @PostMapping
    public ResponseEntity<RentHistoryDTO> setOrUpdateRent(
            @PathVariable Long unitId,
            @Valid @RequestBody SetRentRequest request) {
        RentHistoryDTO result = rentHistoryService.setOrUpdateRent(unitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
