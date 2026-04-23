package com.immocare.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BoilerServiceDTOs.AddBoilerServiceRecordRequest;
import com.immocare.model.dto.BoilerServiceDTOs.BoilerServiceRecordDTO;
import com.immocare.service.BoilerServiceHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for boiler maintenance history — UC012 (UC011.005/UC011.006). */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BoilerServiceController {

    private final BoilerServiceHistoryService historyService;

    /** GET /api/v1/boilers/{boilerId}/services — UC011.006 */
    @GetMapping("/api/v1/boilers/{boilerId}/services")
    public ResponseEntity<List<BoilerServiceRecordDTO>> getHistory(
            @PathVariable Long boilerId) {
        return ResponseEntity.ok(historyService.getHistory(boilerId));
    }

    /** POST /api/v1/boilers/{boilerId}/services — UC011.005 */
    @PostMapping("/api/v1/boilers/{boilerId}/services")
    public ResponseEntity<BoilerServiceRecordDTO> addRecord(
            @PathVariable Long boilerId,
            @Valid @RequestBody AddBoilerServiceRecordRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historyService.addRecord(boilerId, req));
    }
}