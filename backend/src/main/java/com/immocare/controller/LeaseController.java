package com.immocare.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.AddTenantRequest;
import com.immocare.model.dto.AdjustRentRequest;
import com.immocare.model.dto.ChangeLeaseStatusRequest;
import com.immocare.model.dto.CreateLeaseRequest;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.dto.LeaseDTO;
import com.immocare.model.dto.LeaseFilterParams;
import com.immocare.model.dto.LeaseGlobalSummaryDTO;
import com.immocare.model.dto.LeaseSummaryDTO;
import com.immocare.model.dto.UpdateLeaseRequest;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;
import com.immocare.service.LeaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Lease management.
 * UC004_ESTATE_PLACEHOLDER Phase 3: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/housing-units/{unitId}/leases          - leases by unit
 *   POST   /api/v1/estates/{estateId}/housing-units/{unitId}/leases          - create lease
 *   GET    /api/v1/estates/{estateId}/leases                                 - global paginated list
 *   GET    /api/v1/estates/{estateId}/leases/alerts                          - contextual alerts
 *   GET    /api/v1/estates/{estateId}/leases/{id}                            - get by id
 *   PUT    /api/v1/estates/{estateId}/leases/{id}                            - update
 *   PATCH  /api/v1/estates/{estateId}/leases/{id}/status                     - change status
 *   POST   /api/v1/estates/{estateId}/leases/{id}/tenants                    - add tenant
 *   DELETE /api/v1/estates/{estateId}/leases/{id}/tenants/{personId}         - remove tenant
 *   POST   /api/v1/estates/{estateId}/leases/{id}/rent-adjustments           - adjust rent
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("@security.isMemberOf(#estateId)")
public class LeaseController {

    private final LeaseService leaseService;

    /** GET /api/v1/estates/{estateId}/housing-units/{unitId}/leases */
    @GetMapping("/api/v1/estates/{estateId}/housing-units/{unitId}/leases")
    public ResponseEntity<List<LeaseSummaryDTO>> getByUnit(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return ResponseEntity.ok(leaseService.getByUnit(estateId, unitId));
    }

    /** POST /api/v1/estates/{estateId}/housing-units/{unitId}/leases */
    @PostMapping("/api/v1/estates/{estateId}/housing-units/{unitId}/leases")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> create(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @Valid @RequestBody CreateLeaseRequest req,
            @RequestParam(defaultValue = "false") boolean activate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaseService.create(estateId, unitId, req, activate));
    }

    /** GET /api/v1/estates/{estateId}/leases */
    @GetMapping("/api/v1/estates/{estateId}/leases")
    public ResponseEntity<Page<LeaseGlobalSummaryDTO>> getAll(
            @PathVariable UUID estateId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String leaseType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long housingUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo,
            @RequestParam(required = false) BigDecimal rentMin,
            @RequestParam(required = false) BigDecimal rentMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort) {

        LeaseFilterParams params = new LeaseFilterParams();
        if (status != null && !status.isEmpty()) {
            params.setStatuses(status.stream().map(LeaseStatus::valueOf).collect(Collectors.toList()));
        }
        if (leaseType != null) params.setLeaseType(LeaseType.valueOf(leaseType));
        params.setBuildingId(buildingId);
        params.setHousingUnitId(housingUnitId);
        params.setStartDateFrom(startDateFrom);
        params.setStartDateTo(startDateTo);
        params.setEndDateFrom(endDateFrom);
        params.setEndDateTo(endDateTo);
        params.setRentMin(rentMin);
        params.setRentMax(rentMax);

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(leaseService.getAll(estateId, params, pageable));
    }

    /** GET /api/v1/estates/{estateId}/leases/alerts */
    @GetMapping("/api/v1/estates/{estateId}/leases/alerts")
    public ResponseEntity<List<LeaseAlertDTO>> getAlerts(@PathVariable UUID estateId) {
        return ResponseEntity.ok(leaseService.getAlerts(estateId));
    }

    /** GET /api/v1/estates/{estateId}/leases/{id} */
    @GetMapping("/api/v1/estates/{estateId}/leases/{id}")
    public ResponseEntity<LeaseDTO> getById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        return ResponseEntity.ok(leaseService.getById(estateId, id));
    }

    /** PUT /api/v1/estates/{estateId}/leases/{id} */
    @PutMapping("/api/v1/estates/{estateId}/leases/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeaseRequest req) {
        return ResponseEntity.ok(leaseService.update(estateId, id, req));
    }

    /** PATCH /api/v1/estates/{estateId}/leases/{id}/status */
    @PatchMapping("/api/v1/estates/{estateId}/leases/{id}/status")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> changeStatus(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody ChangeLeaseStatusRequest req) {
        return ResponseEntity.ok(leaseService.changeStatus(estateId, id, req));
    }

    /** POST /api/v1/estates/{estateId}/leases/{id}/tenants */
    @PostMapping("/api/v1/estates/{estateId}/leases/{id}/tenants")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> addTenant(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody AddTenantRequest req) {
        return ResponseEntity.ok(leaseService.addTenant(estateId, id, req));
    }

    /** DELETE /api/v1/estates/{estateId}/leases/{id}/tenants/{personId} */
    @DeleteMapping("/api/v1/estates/{estateId}/leases/{id}/tenants/{personId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> removeTenant(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @PathVariable Long personId) {
        return ResponseEntity.ok(leaseService.removeTenant(estateId, id, personId));
    }

    /** POST /api/v1/estates/{estateId}/leases/{id}/rent-adjustments */
    @PostMapping("/api/v1/estates/{estateId}/leases/{id}/rent-adjustments")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<LeaseDTO> adjustRent(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody AdjustRentRequest req) {
        return ResponseEntity.ok(leaseService.adjustRent(estateId, id, req));
    }
}
