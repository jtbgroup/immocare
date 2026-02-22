package com.immocare.controller;

import com.immocare.model.dto.AssignMeterRequest;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.dto.WaterMeterHistoryDTO;
import com.immocare.service.WaterMeterHistoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for UC006 — Manage Water Meters (US026-US030).
 *
 * <pre>
 * GET    /api/v1/housing-units/{unitId}/meters          → full history (US028)
 * GET    /api/v1/housing-units/{unitId}/meters/active   → current active meter (US026 AC1, US028 AC1)
 * POST   /api/v1/housing-units/{unitId}/meters          → assign first meter (US026)
 * PUT    /api/v1/housing-units/{unitId}/meters/replace  → replace meter (US027)
 * DELETE /api/v1/housing-units/{unitId}/meters/active   → remove meter (US029)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/housing-units/{unitId}/meters")
@PreAuthorize("hasRole('ADMIN')")
public class WaterMeterHistoryController {

    private final WaterMeterHistoryService meterService;

    public WaterMeterHistoryController(WaterMeterHistoryService meterService) {
        this.meterService = meterService;
    }

    /**
     * US028 — Full history sorted by installation date DESC.
     */
    @GetMapping
    public ResponseEntity<List<WaterMeterHistoryDTO>> getMeterHistory(@PathVariable Long unitId) {
        return ResponseEntity.ok(meterService.getMeterHistory(unitId));
    }

    /**
     * US026 AC1, US028 AC1 — Current active meter badge on unit details.
     * Returns 204 No Content when no meter has been assigned.
     */
    @GetMapping("/active")
    public ResponseEntity<WaterMeterHistoryDTO> getActiveMeter(@PathVariable Long unitId) {
        return meterService.getActiveMeter(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * US026 — Assign the first water meter to a unit.
     * Returns 409 Conflict if an active meter already exists.
     */
    @PostMapping
    public ResponseEntity<WaterMeterHistoryDTO> assignMeter(
            @PathVariable Long unitId,
            @Valid @RequestBody AssignMeterRequest request) {
        WaterMeterHistoryDTO created = meterService.assignMeter(unitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * US027 — Replace the current active meter with a new one.
     * Atomically closes the old meter and creates the new one.
     */
    @PutMapping("/replace")
    public ResponseEntity<WaterMeterHistoryDTO> replaceMeter(
            @PathVariable Long unitId,
            @Valid @RequestBody ReplaceMeterRequest request) {
        WaterMeterHistoryDTO newMeter = meterService.replaceMeter(unitId, request);
        return ResponseEntity.ok(newMeter);
    }

    /**
     * US029 — Remove the active meter without replacement.
     * Unit will have no active meter afterwards.
     */
    @DeleteMapping("/active")
    public ResponseEntity<WaterMeterHistoryDTO> removeMeter(
            @PathVariable Long unitId,
            @Valid @RequestBody RemoveMeterRequest request) {
        WaterMeterHistoryDTO removed = meterService.removeMeter(unitId, request);
        return ResponseEntity.ok(removed);
    }
}
