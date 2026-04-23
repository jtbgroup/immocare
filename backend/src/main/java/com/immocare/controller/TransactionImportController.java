package com.immocare.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.ImportBatchSummaryDTO;
import com.immocare.model.dto.ImportPreviewRowDTO;
import com.immocare.model.dto.ImportRowEnrichmentDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.service.TransactionImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for the 3-step transaction import flow.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   POST /api/v1/estates/{estateId}/transactions/preview  — parse only, no persistence
 *   POST /api/v1/estates/{estateId}/transactions/import   — parse + persist
 *   GET  /api/v1/estates/{estateId}/transactions/import-batches — import history
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/estates/{estateId}/transactions")
@RequiredArgsConstructor
@PreAuthorize("@security.isMemberOf(#estateId)")
public class TransactionImportController {

    private final TransactionImportService importService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ─── Preview ──────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/estates/{estateId}/transactions/preview
     * Parse the file and return enriched rows without persisting anything.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<?> previewFile(
            @PathVariable UUID estateId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("parserCode") String parserCode) {
        try {
            List<ImportPreviewRowDTO> rows = importService.previewFile(estateId, file, parserCode.trim());
            return ResponseEntity.ok(rows);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        }
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/estates/{estateId}/transactions/import
     * Parse + apply per-row enrichments + persist.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<ImportBatchResultDTO> importFile(
            @PathVariable UUID estateId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("parserCode") String parserCode,
            @RequestPart(value = "bankAccountId", required = false) String bankAccountIdStr,
            @RequestPart(value = "enrichments", required = false) String enrichmentsJson,
            @RequestPart(value = "selectedFingerprints", required = false) String selectedFingerprintsJson,
            @AuthenticationPrincipal AppUser currentUser) {

        Long bankAccountId = parseId(bankAccountIdStr);

        List<ImportRowEnrichmentDTO> enrichments = List.of();
        if (enrichmentsJson != null && !enrichmentsJson.isBlank()) {
            try {
                enrichments = MAPPER.readValue(
                        enrichmentsJson, new TypeReference<List<ImportRowEnrichmentDTO>>() {});
            } catch (Exception e) {
                log.warn("Could not deserialize enrichments JSON: {}", e.getMessage());
            }
        }

        Set<String> selectedFingerprints = java.util.Collections.emptySet();
        if (selectedFingerprintsJson != null && !selectedFingerprintsJson.isBlank()) {
            try {
                List<String> fpList = MAPPER.readValue(
                        selectedFingerprintsJson, new TypeReference<List<String>>() {});
                selectedFingerprints = new java.util.HashSet<>(fpList);
            } catch (Exception e) {
                log.warn("Could not deserialize selectedFingerprints JSON: {}", e.getMessage());
            }
        }

        try {
            ImportBatchResultDTO result = importService.importFile(
                    estateId, file, parserCode.trim(), bankAccountId,
                    enrichments, selectedFingerprints, currentUser);
            return ResponseEntity.ok(result);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        }
    }

    // ─── Import history ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/estates/{estateId}/transactions/import-batches
     * Returns the import batch history for the estate, newest first.
     */
    @GetMapping("/import-batches")
    public ResponseEntity<List<ImportBatchSummaryDTO>> getImportBatches(
            @PathVariable UUID estateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(importService.getImportBatches(estateId, page, size));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Long parseId(String str) {
        return (str != null && !str.isBlank()) ? Long.parseLong(str.trim()) : null;
    }
}
