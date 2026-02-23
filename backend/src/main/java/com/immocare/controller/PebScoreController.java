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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.service.PebScoreService;

import jakarta.validation.Valid;

/**
 * REST controller for PEB score management.
 * UC004 - Manage PEB Scores (US017-US020).
 *
 * Endpoints:
 * POST /api/v1/housing-units/{unitId}/peb-scores → addScore
 * PUT /api/v1/housing-units/{unitId}/peb-scores/{scoreId} → updateScore
 * DELETE /api/v1/housing-units/{unitId}/peb-scores/{scoreId} → deleteScore
 * GET /api/v1/housing-units/{unitId}/peb-scores → getHistory
 * GET /api/v1/housing-units/{unitId}/peb-scores/current → getCurrentScore
 * GET /api/v1/housing-units/{unitId}/peb-scores/improvements →
 * getImprovementSummary
 */
@RestController
@RequestMapping("/api/v1/housing-units/{unitId}/peb-scores")
@PreAuthorize("hasRole('ADMIN')")
public class PebScoreController {

    private final PebScoreService pebScoreService;

    public PebScoreController(PebScoreService pebScoreService) {
        this.pebScoreService = pebScoreService;
    }

    /** US017 - Add PEB Score */
    @PostMapping
    public ResponseEntity<PebScoreDTO> addScore(
            @PathVariable Long unitId,
            @Valid @RequestBody CreatePebScoreRequest request) {
        PebScoreDTO created = pebScoreService.addScore(unitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Update an existing PEB score (correction use case) */
    @PutMapping("/{scoreId}")
    public ResponseEntity<PebScoreDTO> updateScore(
            @PathVariable Long unitId,
            @PathVariable Long scoreId,
            @Valid @RequestBody CreatePebScoreRequest request) {
        PebScoreDTO updated = pebScoreService.updateScore(unitId, scoreId, request);
        return ResponseEntity.ok(updated);
    }

    /** Delete a PEB score entry */
    @DeleteMapping("/{scoreId}")
    public ResponseEntity<Void> deleteScore(
            @PathVariable Long unitId,
            @PathVariable Long scoreId) {
        pebScoreService.deleteScore(unitId, scoreId);
        return ResponseEntity.ok().build();
    }

    /** US018 - View PEB Score History */
    @GetMapping
    public ResponseEntity<List<PebScoreDTO>> getHistory(@PathVariable Long unitId) {
        return ResponseEntity.ok(pebScoreService.getHistory(unitId));
    }

    /** US018 - Current score badge */
    @GetMapping("/current")
    public ResponseEntity<PebScoreDTO> getCurrentScore(@PathVariable Long unitId) {
        return pebScoreService.getCurrentScore(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** US020 - Track PEB Score Improvements */
    @GetMapping("/improvements")
    public ResponseEntity<PebImprovementDTO> getImprovementSummary(@PathVariable Long unitId) {
        return pebScoreService.getImprovementSummary(unitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}