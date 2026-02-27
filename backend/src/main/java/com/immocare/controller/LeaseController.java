package com.immocare.controller;

import com.immocare.model.dto.*;
import com.immocare.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LeaseController {

    private final LeaseService leaseService;

    /** GET /api/v1/housing-units/{unitId}/leases */
    @GetMapping("/api/v1/housing-units/{unitId}/leases")
    public ResponseEntity<List<LeaseSummaryDTO>> getByUnit(@PathVariable Long unitId) {
        return ResponseEntity.ok(leaseService.getByUnit(unitId));
    }

    /** GET /api/v1/leases/{id} */
    @GetMapping("/api/v1/leases/{id}")
    public ResponseEntity<LeaseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaseService.getById(id));
    }

    /** POST /api/v1/leases */
    @PostMapping("/api/v1/leases")
    public ResponseEntity<LeaseDTO> create(@Valid @RequestBody CreateLeaseRequest req,
                                           @RequestParam(defaultValue = "false") boolean activate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaseService.create(req, activate));
    }

    /** PUT /api/v1/leases/{id} */
    @PutMapping("/api/v1/leases/{id}")
    public ResponseEntity<LeaseDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateLeaseRequest req) {
        return ResponseEntity.ok(leaseService.update(id, req));
    }

    /** PATCH /api/v1/leases/{id}/status */
    @PatchMapping("/api/v1/leases/{id}/status")
    public ResponseEntity<LeaseDTO> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody ChangeLeaseStatusRequest req) {
        return ResponseEntity.ok(leaseService.changeStatus(id, req));
    }

    /** POST /api/v1/leases/{id}/tenants */
    @PostMapping("/api/v1/leases/{id}/tenants")
    public ResponseEntity<LeaseDTO> addTenant(@PathVariable Long id,
                                               @Valid @RequestBody AddTenantRequest req) {
        return ResponseEntity.ok(leaseService.addTenant(id, req));
    }

    /** DELETE /api/v1/leases/{id}/tenants/{personId} */
    @DeleteMapping("/api/v1/leases/{id}/tenants/{personId}")
    public ResponseEntity<LeaseDTO> removeTenant(@PathVariable Long id,
                                                  @PathVariable Long personId) {
        return ResponseEntity.ok(leaseService.removeTenant(id, personId));
    }

    /** POST /api/v1/leases/{id}/rent-adjustments — Adjust rent or charges */
    @PostMapping("/api/v1/leases/{id}/rent-adjustments")
    public ResponseEntity<LeaseDTO> adjustRent(@PathVariable Long id,
                                                @Valid @RequestBody AdjustRentRequest req) {
        return ResponseEntity.ok(leaseService.adjustRent(id, req));
    }

    /** GET /api/v1/leases/alerts */
    @GetMapping("/api/v1/leases/alerts")
    public ResponseEntity<List<LeaseAlertDTO>> getAlerts() {
        return ResponseEntity.ok(leaseService.getAlerts());
    }
}
