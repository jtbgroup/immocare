package com.immocare.controller;

import com.immocare.model.dto.AddMeterRequest;
import com.immocare.model.dto.MeterDTO;
import com.immocare.model.dto.MeterSearchResultDTO;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.enums.AssetType;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.MeterRepository;
import com.immocare.repository.TransactionAssetLinkRepository;
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
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MeterController {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING = "BUILDING";

    private final MeterService meterService;
    private final MeterRepository meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;
    private final TransactionAssetLinkRepository transactionAssetLinkRepository;

    // ─── Housing unit meters ──────────────────────────────────────────────────

    @GetMapping("/api/v1/housing-units/{unitId}/meters")
    public ResponseEntity<List<MeterDTO>> getUnitMeters(
            @PathVariable Long unitId,
            @RequestParam(required = false) String status) {
        List<MeterDTO> result = "active".equalsIgnoreCase(status)
                ? meterService.getActiveMeters(HOUSING_UNIT, unitId)
                : meterService.getMeterHistory(HOUSING_UNIT, unitId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/v1/housing-units/{unitId}/meters")
    public ResponseEntity<MeterDTO> addUnitMeter(
            @PathVariable Long unitId,
            @Valid @RequestBody AddMeterRequest request) {
        MeterDTO created = meterService.addMeter(HOUSING_UNIT, unitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/v1/housing-units/{unitId}/meters/{meterId}/replace")
    public ResponseEntity<MeterDTO> replaceUnitMeter(
            @PathVariable Long unitId,
            @PathVariable Long meterId,
            @Valid @RequestBody ReplaceMeterRequest request) {
        MeterDTO newMeter = meterService.replaceMeter(HOUSING_UNIT, unitId, meterId, request);
        return ResponseEntity.ok(newMeter);
    }

    @DeleteMapping("/api/v1/housing-units/{unitId}/meters/{meterId}")
    public ResponseEntity<Void> removeUnitMeter(
            @PathVariable Long unitId,
            @PathVariable Long meterId,
            @Valid @RequestBody RemoveMeterRequest request) {
        meterService.removeMeter(HOUSING_UNIT, unitId, meterId, request.endDate());
        return ResponseEntity.noContent().build();
    }

    // ─── Building meters ──────────────────────────────────────────────────────

    @GetMapping("/api/v1/buildings/{buildingId}/meters")
    public ResponseEntity<List<MeterDTO>> getBuildingMeters(
            @PathVariable Long buildingId,
            @RequestParam(required = false) String status) {
        List<MeterDTO> result = "active".equalsIgnoreCase(status)
                ? meterService.getActiveMeters(BUILDING, buildingId)
                : meterService.getMeterHistory(BUILDING, buildingId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/v1/buildings/{buildingId}/meters")
    public ResponseEntity<MeterDTO> addBuildingMeter(
            @PathVariable Long buildingId,
            @Valid @RequestBody AddMeterRequest request) {
        MeterDTO created = meterService.addMeter(BUILDING, buildingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/v1/buildings/{buildingId}/meters/{meterId}/replace")
    public ResponseEntity<MeterDTO> replaceBuildingMeter(
            @PathVariable Long buildingId,
            @PathVariable Long meterId,
            @Valid @RequestBody ReplaceMeterRequest request) {
        MeterDTO newMeter = meterService.replaceMeter(BUILDING, buildingId, meterId, request);
        return ResponseEntity.ok(newMeter);
    }

    @DeleteMapping("/api/v1/buildings/{buildingId}/meters/{meterId}")
    public ResponseEntity<Void> removeBuildingMeter(
            @PathVariable Long buildingId,
            @PathVariable Long meterId,
            @Valid @RequestBody RemoveMeterRequest request) {
        meterService.removeMeter(BUILDING, buildingId, meterId, request.endDate());
        return ResponseEntity.noContent().build();
    }

    // ─── Search (asset picker) ────────────────────────────────────────────────

    /**
     * GET /api/v1/meters/search?q=&buildingId=
     * Asset picker endpoint for transaction forms.
     * Searches active meters by meter number, label or EAN code (case-insensitive, min 2 chars).
     * Optionally filtered by building.
     */
    @GetMapping("/api/v1/meters/search")
    public ResponseEntity<List<MeterSearchResultDTO>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Long buildingId) {

        String term = q.trim().toLowerCase();

        List<MeterSearchResultDTO> results = meterRepository.findAll().stream()
                .filter(m -> m.getEndDate() == null) // active only
                .filter(m -> {
                    if (term.length() >= 2) {
                        String num = m.getMeterNumber() != null ? m.getMeterNumber().toLowerCase() : "";
                        String label = m.getLabel() != null ? m.getLabel().toLowerCase() : "";
                        String ean = m.getEanCode() != null ? m.getEanCode().toLowerCase() : "";
                        if (!num.contains(term) && !label.contains(term) && !ean.contains(term)) {
                            return false;
                        }
                    }
                    if (buildingId != null) {
                        Long meterBuildingId = resolveBuildingId(m.getOwnerType(), m.getOwnerId());
                        return buildingId.equals(meterBuildingId);
                    }
                    return true;
                })
                .map(m -> {
                    String displayLabel = m.getMeterNumber() + " — " + m.getType()
                            + (m.getLabel() != null && !m.getLabel().isBlank() ? " (" + m.getLabel() + ")" : "");
                    String unitNumber = null;
                    String buildingName = null;
                    if (HOUSING_UNIT.equals(m.getOwnerType())) {
                        var unitOpt = housingUnitRepository.findById(m.getOwnerId());
                        if (unitOpt.isPresent()) {
                            unitNumber = unitOpt.get().getUnitNumber();
                            buildingName = unitOpt.get().getBuilding().getName();
                        }
                    } else if (BUILDING.equals(m.getOwnerType())) {
                        buildingName = buildingRepository.findById(m.getOwnerId())
                                .map(b -> b.getName()).orElse(null);
                    }
                    return new MeterSearchResultDTO(m.getId(), displayLabel, m.getType(),
                            m.getOwnerType(), m.getOwnerId(), unitNumber, buildingName);
                })
                .limit(20)
                .toList();

        return ResponseEntity.ok(results);
    }

    // ─── Transaction count ────────────────────────────────────────────────────

    @GetMapping("/api/v1/meters/{meterId}/transaction-count")
    public long getTransactionCount(@PathVariable Long meterId) {
        return transactionAssetLinkRepository.countByAssetTypeAndAssetId(AssetType.METER, meterId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Long resolveBuildingId(String ownerType, Long ownerId) {
        if (BUILDING.equals(ownerType)) return ownerId;
        if (HOUSING_UNIT.equals(ownerType)) {
            return housingUnitRepository.findById(ownerId)
                    .map(u -> u.getBuilding().getId())
                    .orElse(null);
        }
        return null;
    }
}
