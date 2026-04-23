package com.immocare.controller;

import java.util.List;
import java.util.UUID;

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

import com.immocare.model.dto.AddRevisionRequest;
import com.immocare.model.dto.FireExtinguisherDTO.FireExtinguisherResponse;
import com.immocare.model.dto.FireExtinguisherSearchResultDTO;
import com.immocare.model.dto.SaveFireExtinguisherRequest;
import com.immocare.model.enums.AssetType;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.TransactionAssetLinkRepository;
import com.immocare.service.FireExtinguisherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Fire Extinguisher management.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all routes are now scoped to an estate.
 *
 * Endpoints:
 * GET    /api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers
 * POST   /api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers
 * GET    /api/v1/estates/{estateId}/fire-extinguishers/{id}
 * PUT    /api/v1/estates/{estateId}/fire-extinguishers/{id}
 * DELETE /api/v1/estates/{estateId}/fire-extinguishers/{id}
 * POST   /api/v1/estates/{estateId}/fire-extinguishers/{id}/revisions
 * DELETE /api/v1/estates/{estateId}/fire-extinguishers/{extId}/revisions/{revId}
 */
@RestController
@RequiredArgsConstructor
public class FireExtinguisherController {

    private final FireExtinguisherService service;
    private final TransactionAssetLinkRepository transactionAssetLinkRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;

    @GetMapping("/api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<List<FireExtinguisherResponse>> getByBuilding(
            @PathVariable UUID estateId,
            @PathVariable Long buildingId) {
        return ResponseEntity.ok(service.getByBuilding(estateId, buildingId));
    }

    @GetMapping("/api/v1/estates/{estateId}/fire-extinguishers/{id}")
    @PreAuthorize("@security.isMemberOf(#estateId)")
    public ResponseEntity<FireExtinguisherResponse> getById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getById(estateId, id));
    }

    @PostMapping("/api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<FireExtinguisherResponse> create(
            @PathVariable UUID estateId,
            @PathVariable Long buildingId,
            @Valid @RequestBody SaveFireExtinguisherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(estateId, buildingId, req));
    }

    @PutMapping("/api/v1/estates/{estateId}/fire-extinguishers/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<FireExtinguisherResponse> update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody SaveFireExtinguisherRequest req) {
        return ResponseEntity.ok(service.update(estateId, id, req));
    }

    @DeleteMapping("/api/v1/estates/{estateId}/fire-extinguishers/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        service.delete(estateId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/estates/{estateId}/fire-extinguishers/{id}/revisions")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<FireExtinguisherResponse> addRevision(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody AddRevisionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRevision(estateId, id, req));
    }

    @DeleteMapping("/api/v1/estates/{estateId}/fire-extinguishers/{extId}/revisions/{revId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Void> deleteRevision(
            @PathVariable UUID estateId,
            @PathVariable Long extId,
            @PathVariable Long revId) {
        service.deleteRevision(estateId, extId, revId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/fire-extinguishers/{id}/transaction-count")
    @PreAuthorize("isAuthenticated()")
    public long getTransactionCount(@PathVariable Long id) {
        return transactionAssetLinkRepository.countByAssetTypeAndAssetId(AssetType.FIRE_EXTINGUISHER, id);
    }

    /**
     * GET /api/v1/fire-extinguishers/search?q=&buildingId=
     * Asset picker endpoint for transaction forms.
     * Searches by identification number (case-insensitive, min 2 chars).
     * Optionally filtered by building.
     * Note: search endpoint kept without estate scope for transaction form pickers.
     */
    @GetMapping("/api/v1/fire-extinguishers/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FireExtinguisherSearchResultDTO>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Long buildingId) {

        String term = q.trim().toLowerCase();

        List<FireExtinguisherSearchResultDTO> results = (buildingId != null
                ? fireExtinguisherRepository.findByBuildingIdOrderByIdentificationNumberAsc(buildingId)
                : fireExtinguisherRepository.findAll())
                .stream()
                .filter(ext -> term.length() < 2 ||
                        ext.getIdentificationNumber().toLowerCase().contains(term))
                .map(ext -> new FireExtinguisherSearchResultDTO(
                        ext.getId(),
                        ext.getIdentificationNumber(),
                        ext.getBuilding().getId(),
                        ext.getBuilding().getName(),
                        ext.getUnit() != null ? ext.getUnit().getId() : null,
                        ext.getUnit() != null ? ext.getUnit().getUnitNumber() : null))
                .limit(20)
                .toList();

        return ResponseEntity.ok(results);
    }
}
