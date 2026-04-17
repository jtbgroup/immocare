package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.AlertDTO;
import com.immocare.service.AlertService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for cross-cutting alerts.
 *
 * <p>
 * Aggregates alerts from all sources (leases, boilers, …) into a single
 * endpoint.
 * Contextual endpoints on each resource controller (/leases/alerts,
 * /boilers/alerts)
 * are kept for inline banners on detail pages.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // All endpoints require authentication, but no specific role
public class AlertController {

    private final AlertService alertService;

    /** GET /api/v1/alerts — all pending alerts, sorted by deadline ASC */
    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAll(UUID estateId) {
        return ResponseEntity.ok(alertService.getAll(estateId));
    }

    /** GET /api/v1/alerts/count — total count for the bell badge */
    @GetMapping("/count")
    public ResponseEntity<Integer> getCount(UUID estateId) {
        return ResponseEntity.ok(alertService.getCount(estateId));
    }
}
