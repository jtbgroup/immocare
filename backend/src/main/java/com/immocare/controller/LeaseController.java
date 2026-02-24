package com.immocare.controller;

import com.immocare.model.dto.*;
import com.immocare.service.LeaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    /** GET /api/v1/housing-units/{unitId}/leases — All leases for a unit */
    @GetMapping("/api/v1/housing-units/{unitId}/leases")
    public ResponseEntity<List<LeaseSummaryDTO>> getByUnit(@PathVariable Long unitId) {
        return ResponseEntity.ok(leaseService.getByUnit(unitId));
    }

    /** GET /api/v1/leases/{id} — Full lease details */
    @GetMapping("/api/v1/leases/{id}")
    public ResponseEntity<LeaseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaseService.getById(id));
    }

    /** POST /api/v1/leases?activate=false — Create lease (DRAFT by default) */
    @PostMapping("/api/v1/leases")
    public ResponseEntity<LeaseDTO> create(
            @Valid @RequestBody CreateLeaseRequest request,
            @RequestParam(defaultValue = "false") boolean activate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaseService.create(request, activate));
    }

    /** PUT /api/v1/leases/{id} — Update lease */
    @PutMapping("/api/v1/leases/{id}")
    public ResponseEntity<LeaseDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateLeaseRequest request) {
        return ResponseEntity.ok(leaseService.update(id, request));
    }

    /** PATCH /api/v1/leases/{id}/status — Change lease status (activate/finish/cancel) */
    @PatchMapping("/api/v1/leases/{id}/status")
    public ResponseEntity<LeaseDTO> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody ChangeLeaseStatusRequest request) {
        return ResponseEntity.ok(leaseService.changeStatus(id, request));
    }

    /** POST /api/v1/leases/{id}/tenants — Add tenant */
    @PostMapping("/api/v1/leases/{id}/tenants")
    public ResponseEntity<LeaseDTO> addTenant(@PathVariable Long id,
                                               @Valid @RequestBody AddTenantRequest request) {
        return ResponseEntity.ok(leaseService.addTenant(id, request));
    }

    /** DELETE /api/v1/leases/{id}/tenants/{personId} — Remove tenant */
    @DeleteMapping("/api/v1/leases/{id}/tenants/{personId}")
    public ResponseEntity<LeaseDTO> removeTenant(@PathVariable Long id,
                                                  @PathVariable Long personId) {
        return ResponseEntity.ok(leaseService.removeTenant(id, personId));
    }

    /** POST /api/v1/leases/{id}/indexations — Record indexation */
    @PostMapping("/api/v1/leases/{id}/indexations")
    public ResponseEntity<LeaseDTO> recordIndexation(@PathVariable Long id,
                                                      @Valid @RequestBody RecordIndexationRequest request) {
        return ResponseEntity.ok(leaseService.recordIndexation(id, request));
    }

    /** GET /api/v1/leases/{id}/indexations — Indexation history */
    @GetMapping("/api/v1/leases/{id}/indexations")
    public ResponseEntity<List<LeaseIndexationDTO>> getIndexations(@PathVariable Long id) {
        return ResponseEntity.ok(leaseService.getIndexationHistory(id));
    }

    /** GET /api/v1/leases/alerts — All pending alerts */
    @GetMapping("/api/v1/leases/alerts")
    public ResponseEntity<List<LeaseAlertDTO>> getAlerts() {
        return ResponseEntity.ok(leaseService.getAlerts());
    }
}
