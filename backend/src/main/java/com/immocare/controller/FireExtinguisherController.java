package com.immocare.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.AddRevisionRequest;
import com.immocare.model.dto.FireExtinguisherDTO.FireExtinguisherResponse;
import com.immocare.model.dto.SaveFireExtinguisherRequest;
import com.immocare.service.FireExtinguisherService;

import jakarta.validation.Valid;

@RestController
public class FireExtinguisherController {

    private final FireExtinguisherService service;

    public FireExtinguisherController(FireExtinguisherService service) {
        this.service = service;
    }

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
        @Valid @RequestBody SaveFireExtinguisherRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(buildingId, req));
    }

    @PutMapping("/api/v1/fire-extinguishers/{id}")
    public ResponseEntity<FireExtinguisherResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody SaveFireExtinguisherRequest req
    ) {
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
        @Valid @RequestBody AddRevisionRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRevision(id, req));
    }

    @DeleteMapping("/api/v1/fire-extinguishers/{extId}/revisions/{revId}")
    public ResponseEntity<Void> deleteRevision(
        @PathVariable Long extId,
        @PathVariable Long revId
    ) {
        service.deleteRevision(extId, revId);
        return ResponseEntity.noContent().build();
    }
}
