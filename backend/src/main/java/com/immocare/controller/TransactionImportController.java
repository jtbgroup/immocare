package com.immocare.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.ImportPreviewRowDTO;
import com.immocare.model.dto.ImportRowEnrichmentDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.service.TransactionImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoints for the 3-step transaction import flow.
 *
 * POST /api/v1/transactions/preview — parse only, no persistence, returns rows with suggestions
 * POST /api/v1/transactions/import  — parse + apply enrichments + persist as DRAFT or CONFIRMED
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionImportController {

    private final TransactionImportService importService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ─── Preview ──────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/transactions/preview
     * Parse the file and return enriched rows without persisting anything.
     *
     * Multipart parts:
     *   file       — CSV or PDF
     *   parserCode — e.g. "keytrade-csv-20260102"
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("parserCode") String parserCode) {
        try {
            List<ImportPreviewRowDTO> rows = importService.previewFile(file, parserCode.trim());
            return ResponseEntity.ok(rows);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        }
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/transactions/import
     * Parse + apply per-row enrichments + persist.
     * Rows matched by fingerprint to an enrichment are saved as CONFIRMED;
     * unmatched rows are saved as DRAFT.
     *
     * Multipart parts:
     *   file          — CSV or PDF
     *   parserCode    — e.g. "keytrade-csv-20260102"
     *   bankAccountId — (optional) own bank account id
     *   enrichments   — (optional) JSON array of ImportRowEnrichmentDTO
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResultDTO> importFile(
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
                    file, parserCode.trim(), bankAccountId, enrichments, selectedFingerprints, currentUser);
            return ResponseEntity.ok(result);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ImportBatchResultDTO.error(e.getMessage()));
        }
    }

    private Long parseId(String str) {
        return (str != null && !str.isBlank()) ? Long.parseLong(str.trim()) : null;
    }
}
