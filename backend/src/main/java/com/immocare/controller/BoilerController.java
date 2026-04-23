package com.immocare.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.BoilerSearchResultDTO;
import com.immocare.model.dto.SaveBoilerRequest;
import com.immocare.model.enums.AssetType;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.TransactionAssetLinkRepository;
import com.immocare.service.BoilerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for UC012 — Manage Boilers.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BoilerController {

    private static final String HOUSING_UNIT = "HOUSING_UNIT";
    private static final String BUILDING = "BUILDING";

    private final BoilerService boilerService;
    private final TransactionAssetLinkRepository transactionAssetLinkRepository;
    private final BoilerRepository boilerRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;

    @GetMapping("/api/v1/boilers/alerts")
    public ResponseEntity<List<BoilerDTO>> getServiceAlerts() {
        return ResponseEntity.ok(boilerService.getServiceAlerts(null));
    }

    /**
     * GET /api/v1/boilers/search?q=&buildingId=
     * Asset picker endpoint for transaction forms.
     * Searches by brand, model or serial number (case-insensitive, min 2 chars).
     * Optionally filtered by building.
     */
    @GetMapping("/api/v1/boilers/search")
    public ResponseEntity<List<BoilerSearchResultDTO>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Long buildingId) {

        String term = q.trim().toLowerCase();

        List<BoilerSearchResultDTO> results = boilerRepository.findAll().stream()
                .filter(b -> {
                    if (term.length() >= 2) {
                        String brand = b.getBrand() != null ? b.getBrand().toLowerCase() : "";
                        String model = b.getModel() != null ? b.getModel().toLowerCase() : "";
                        String serial = b.getSerialNumber() != null ? b.getSerialNumber().toLowerCase() : "";
                        if (!brand.contains(term) && !model.contains(term) && !serial.contains(term)) {
                            return false;
                        }
                    }
                    if (buildingId != null) {
                        Long boilerBuildingId = resolveBuildingId(b.getOwnerType(), b.getOwnerId());
                        return buildingId.equals(boilerBuildingId);
                    }
                    return true;
                })
                .map(b -> {
                    String label = buildLabel(b.getBrand(), b.getModel(), b.getSerialNumber());
                    String unitNumber = null;
                    String buildingName = null;
                    if (HOUSING_UNIT.equals(b.getOwnerType())) {
                        var unitOpt = housingUnitRepository.findById(b.getOwnerId());
                        if (unitOpt.isPresent()) {
                            unitNumber = unitOpt.get().getUnitNumber();
                            buildingName = unitOpt.get().getBuilding().getName();
                        }
                    } else if (BUILDING.equals(b.getOwnerType())) {
                        buildingName = buildingRepository.findById(b.getOwnerId())
                                .map(bld -> bld.getName()).orElse(null);
                    }
                    return new BoilerSearchResultDTO(b.getId(), label, b.getOwnerType(), b.getOwnerId(),
                            unitNumber, buildingName);
                })
                .limit(20)
                .toList();

        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/v1/housing-units/{unitId}/boilers")
    public ResponseEntity<List<BoilerDTO>> getByUnit(@PathVariable Long unitId) {
        return ResponseEntity.ok(boilerService.getBoilers(HOUSING_UNIT, unitId));
    }

    @PostMapping("/api/v1/housing-units/{unitId}/boilers")
    public ResponseEntity<BoilerDTO> createForUnit(@PathVariable Long unitId,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boilerService.create(HOUSING_UNIT, unitId, req));
    }

    @GetMapping("/api/v1/buildings/{buildingId}/boilers")
    public ResponseEntity<List<BoilerDTO>> getByBuilding(@PathVariable Long buildingId) {
        return ResponseEntity.ok(boilerService.getBoilers(BUILDING, buildingId));
    }

    @PostMapping("/api/v1/buildings/{buildingId}/boilers")
    public ResponseEntity<BoilerDTO> createForBuilding(@PathVariable Long buildingId,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boilerService.create(BUILDING, buildingId, req));
    }

    @GetMapping("/api/v1/boilers/{id}")
    public ResponseEntity<BoilerDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(boilerService.getById(id));
    }

    @PutMapping("/api/v1/boilers/{id}")
    public ResponseEntity<BoilerDTO> update(@PathVariable Long id,
            @Valid @RequestBody SaveBoilerRequest req) {
        return ResponseEntity.ok(boilerService.update(id, req));
    }

    @DeleteMapping("/api/v1/boilers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boilerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/boilers/{boilerId}/transaction-count")
    public long getTransactionCount(@PathVariable Long boilerId) {
        return transactionAssetLinkRepository.countByAssetTypeAndAssetId(AssetType.BOILER, boilerId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Long resolveBuildingId(String ownerType, Long ownerId) {
        if (BUILDING.equals(ownerType))
            return ownerId;
        if (HOUSING_UNIT.equals(ownerType)) {
            return housingUnitRepository.findById(ownerId)
                    .map(u -> u.getBuilding().getId())
                    .orElse(null);
        }
        return null;
    }

    private String buildLabel(String brand, String model, String serial) {
        StringBuilder sb = new StringBuilder();
        if (brand != null && !brand.isBlank())
            sb.append(brand);
        if (model != null && !model.isBlank()) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(model);
        }
        if (serial != null && !serial.isBlank()) {
            if (sb.length() > 0)
                sb.append(" — ");
            sb.append(serial);
        }
        return sb.length() > 0 ? sb.toString() : "Boiler";
    }
}
