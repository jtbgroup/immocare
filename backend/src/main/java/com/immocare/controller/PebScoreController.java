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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.service.PebScoreService;

import jakarta.validation.Valid;

/**
 * REST controller for PEB score management.
 * UC005 - Manage PEB Scores (UC008.001-UC008.004).
 * UC004_ESTATE_PLACEHOLDER: all routes are now scoped to an estate.
 *
 * Endpoints:
 * POST /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores
 * PUT /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/{scoreId}
 * DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/{scoreId}
 * GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores
 * GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/current
 * GET /api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores/improvements
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/housing-units/{unitId}/peb-scores")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class PebScoreController {

    private final PebScoreService pebScoreService;

    public PebScoreController(PebScoreService pebScoreService) {
        this.pebScoreService = pebScoreService;
    }

    /** UC008.001 - Add PEB Score */
    @PostMapping
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<PebScoreDTO> addScore(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @Valid @RequestBody CreatePebScoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pebScoreService.addScore(unitId, request));
    }

    /** Update an existing PEB score (correction use case) */
    @PutMapping("/{scoreId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<PebScoreDTO> updateScore(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @PathVariable Long scoreId,
            @Valid @RequestBody CreatePebScoreRequest request) {
        return ResponseEntity.ok(pebScoreService.updateScore(unitId, scoreId, request));
    }

    /** Delete a PEB score entry */
    @DeleteMapping("/{scoreId}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Void> deleteScore(
            @PathVariable UUID estateId,
            @PathVariable Long unitId,
            @PathVariable Long scoreId) {
        pebScoreService.deleteScore(unitId, scoreId);
        return ResponseEntity.noContent().build();
    }

    /** UC008.002 - View PEB Score History */
    @GetMapping
    public ResponseEntity<List<PebScoreDTO>> getHistory(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return ResponseEntity.ok(pebScoreService.getHistory(unitId));
    }

    /** UC008.002 - Current score badge */
    @GetMapping("/current")
    public ResponseEntity<PebScoreDTO> getCurrentScore(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return pebScoreService.getCurrentScore(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** UC008.004 - Track PEB Score Improvements */
    @GetMapping("/improvements")
    public ResponseEntity<PebImprovementDTO> getImprovementSummary(
            @PathVariable UUID estateId,
            @PathVariable Long unitId) {
        return pebScoreService.getImprovementSummary(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}