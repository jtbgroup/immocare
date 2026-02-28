package com.immocare.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * GET /api/v1/leases
     *
     * Global paginated lease list. All parameters are optional.
     * Defaults to status=ACTIVE, page=0, size=20, sorted by startDate DESC.
     *
     * Supported filters (extensible — add new @RequestParam + LeaseFilterParams
     * field):
     * ?status=ACTIVE,DRAFT — comma-separated LeaseStatus values
     * ?leaseType=MAIN_RESIDENCE_9Y — single LeaseType value
     * ?buildingId=3 — filter by building
     * ?housingUnitId=12 — filter by unit
     * ?startDateFrom=2023-01-01 — start date range (ISO-8601)
     * ?startDateTo=2024-12-31
     * ?endDateFrom=2024-01-01 — end date range
     * ?endDateTo=2025-12-31
     * ?rentMin=500 — rent range
     * ?rentMax=1500
     * ?page=0&size=20&sort=startDate,desc
     */
    @GetMapping("/api/v1/leases")
    public ResponseEntity<Page<LeaseGlobalSummaryDTO>> getAll(
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
            params.setStatuses(status.stream()
                    .map(LeaseStatus::valueOf)
                    .collect(Collectors.toList()));
        }
        if (leaseType != null)
            params.setLeaseType(LeaseType.valueOf(leaseType));
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
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(leaseService.getAll(params, pageable));
    }
}
