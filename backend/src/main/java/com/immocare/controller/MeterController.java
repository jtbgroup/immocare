package com.immocare.controller;

import com.immocare.model.dto.AddMeterRequest;
import com.immocare.model.dto.MeterDTO;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.service.MeterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for UC008 - Manage Meters.
 *
 * <p>Two sets of endpoints sharing the same service:
 * <ul>
 *   <li>{@code /api/v1/housing-units/{unitId}/meters}</li>
 *   <li>{@code /api/v1/buildings/{buildingId}/meters}</li>
 * </ul>
 *
 * <p>The {@code ownerType} is resolved from the URL path.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MeterController {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING     = "BUILDING";

    private final MeterService meterService;

    // ═════════════════════════════════════════════════════════════════════════
    // HOUSING UNIT METERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/housing-units/{unitId}/meters
     *     → full history  (status absent or anything)
     * GET /api/v1/housing-units/{unitId}/meters?status=active
     *     → active meters only
     */
    @GetMapping("/api/v1/housing-units/{unitId}/meters")
    public ResponseEntity<List<MeterDTO>> getUnitMeters(
            @PathVariable Long unitId,
            @RequestParam(required = false) String status) {

        List<MeterDTO> result = "active".equalsIgnoreCase(status)
                ? meterService.getActiveMeters(HOUSING_UNIT, unitId)
                : meterService.getMeterHistory(HOUSING_UNIT, unitId);

        return ResponseEntity.ok(result);
    }

    /** POST /api/v1/housing-units/{unitId}/meters → 201 Created */
    @PostMapping("/api/v1/housing-units/{unitId}/meters")
    public ResponseEntity<MeterDTO> addUnitMeter(
            @PathVariable Long unitId,
            @Valid @RequestBody AddMeterRequest request) {

        MeterDTO created = meterService.addMeter(HOUSING_UNIT, unitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/v1/housing-units/{unitId}/meters/{meterId}/replace → 200 OK */
    @PutMapping("/api/v1/housing-units/{unitId}/meters/{meterId}/replace")
    public ResponseEntity<MeterDTO> replaceUnitMeter(
            @PathVariable Long unitId,
            @PathVariable Long meterId,
            @Valid @RequestBody ReplaceMeterRequest request) {

        MeterDTO newMeter = meterService.replaceMeter(HOUSING_UNIT, unitId, meterId, request);
        return ResponseEntity.ok(newMeter);
    }

    /** DELETE /api/v1/housing-units/{unitId}/meters/{meterId} → 204 No Content */
    @DeleteMapping("/api/v1/housing-units/{unitId}/meters/{meterId}")
    public ResponseEntity<Void> removeUnitMeter(
            @PathVariable Long unitId,
            @PathVariable Long meterId,
            @Valid @RequestBody RemoveMeterRequest request) {

        meterService.removeMeter(HOUSING_UNIT, unitId, meterId, request.endDate());
        return ResponseEntity.noContent().build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUILDING METERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/buildings/{buildingId}/meters
     *     → full history  (status absent or anything)
     * GET /api/v1/buildings/{buildingId}/meters?status=active
     *     → active meters only
     */
    @GetMapping("/api/v1/buildings/{buildingId}/meters")
    public ResponseEntity<List<MeterDTO>> getBuildingMeters(
            @PathVariable Long buildingId,
            @RequestParam(required = false) String status) {

        List<MeterDTO> result = "active".equalsIgnoreCase(status)
                ? meterService.getActiveMeters(BUILDING, buildingId)
                : meterService.getMeterHistory(BUILDING, buildingId);

        return ResponseEntity.ok(result);
    }

    /** POST /api/v1/buildings/{buildingId}/meters → 201 Created */
    @PostMapping("/api/v1/buildings/{buildingId}/meters")
    public ResponseEntity<MeterDTO> addBuildingMeter(
            @PathVariable Long buildingId,
            @Valid @RequestBody AddMeterRequest request) {

        MeterDTO created = meterService.addMeter(BUILDING, buildingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/v1/buildings/{buildingId}/meters/{meterId}/replace → 200 OK */
    @PutMapping("/api/v1/buildings/{buildingId}/meters/{meterId}/replace")
    public ResponseEntity<MeterDTO> replaceBuildingMeter(
            @PathVariable Long buildingId,
            @PathVariable Long meterId,
            @Valid @RequestBody ReplaceMeterRequest request) {

        MeterDTO newMeter = meterService.replaceMeter(BUILDING, buildingId, meterId, request);
        return ResponseEntity.ok(newMeter);
    }

    /** DELETE /api/v1/buildings/{buildingId}/meters/{meterId} → 204 No Content */
    @DeleteMapping("/api/v1/buildings/{buildingId}/meters/{meterId}")
    public ResponseEntity<Void> removeBuildingMeter(
            @PathVariable Long buildingId,
            @PathVariable Long meterId,
            @Valid @RequestBody RemoveMeterRequest request) {

        meterService.removeMeter(BUILDING, buildingId, meterId, request.endDate());
        return ResponseEntity.noContent().build();
    }
}
