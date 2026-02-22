package com.immocare.controller;

import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.service.PebScoreService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for PEB score management.
 * UC004 - Manage PEB Scores (US017-US020).
 *
 * Endpoints:
 * POST /api/v1/housing-units/{unitId}/peb-scores          → addScore
 * GET  /api/v1/housing-units/{unitId}/peb-scores          → getHistory
 * GET  /api/v1/housing-units/{unitId}/peb-scores/current  → getCurrentScore
 * GET  /api/v1/housing-units/{unitId}/peb-scores/improvements → getImprovementSummary
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
