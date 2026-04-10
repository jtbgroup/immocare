package com.immocare.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FireExtinguisherController {

    private final FireExtinguisherService service;
    private final TransactionAssetLinkRepository transactionAssetLinkRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;

    @GetMapping("/api/v1/buildings/{buildingId}/fire-extinguishers")
    public ResponseEntity<List<FireExtinguisherResponse>> getByBuilding(@PathVariable Long buildingId) {
        return ResponseEntity.ok(service.getByBuilding(buildingId));
    }

    @GetMapping("/api/v1/fire-extinguishers/{id}")
    public ResponseEntity<FireExtinguisherResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/api/v1/buildings/{buildingId}/fire-extinguishers")
    public ResponseEntity<FireExtinguisherResponse> create(
            @PathVariable Long buildingId,
            @Valid @RequestBody SaveFireExtinguisherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(buildingId, req));
    }

    @PutMapping("/api/v1/fire-extinguishers/{id}")
    public ResponseEntity<FireExtinguisherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveFireExtinguisherRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/api/v1/fire-extinguishers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/fire-extinguishers/{id}/revisions")
    public ResponseEntity<FireExtinguisherResponse> addRevision(
            @PathVariable Long id,
            @Valid @RequestBody AddRevisionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRevision(id, req));
    }

    @DeleteMapping("/api/v1/fire-extinguishers/{extId}/revisions/{revId}")
    public ResponseEntity<Void> deleteRevision(
            @PathVariable Long extId,
            @PathVariable Long revId) {
        service.deleteRevision(extId, revId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/fire-extinguishers/{id}/transaction-count")
    public long getTransactionCount(@PathVariable Long id) {
        return transactionAssetLinkRepository.countByAssetTypeAndAssetId(AssetType.FIRE_EXTINGUISHER, id);
    }

    /**
     * GET /api/v1/fire-extinguishers/search?q=&buildingId=
     * Asset picker endpoint for transaction forms.
     * Searches by identification number (case-insensitive, min 2 chars).
     * Optionally filtered by building.
     */
    @GetMapping("/api/v1/fire-extinguishers/search")
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
