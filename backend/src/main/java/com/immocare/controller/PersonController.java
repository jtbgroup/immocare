package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.service.PersonService;

import jakarta.validation.Valid;

/**
 * REST controller for Person management.
 * UC004_ESTATE_PLACEHOLDER Phase 3: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/persons              - paginated list with optional search
 *   GET    /api/v1/estates/{estateId}/persons/picker       - picker search (max 10)
 *   GET    /api/v1/estates/{estateId}/persons/{id}         - full person details
 *   POST   /api/v1/estates/{estateId}/persons              - create person
 *   PUT    /api/v1/estates/{estateId}/persons/{id}         - update person
 *   DELETE /api/v1/estates/{estateId}/persons/{id}         - delete person
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/persons")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * GET /api/v1/estates/{estateId}/persons?page=0&size=20&sort=lastName,asc&search=dupont
     */
    @GetMapping
    public ResponseEntity<Page<PersonSummaryDTO>> getAll(
            @PathVariable UUID estateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort,
            @RequestParam(required = false) String search) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(personService.getAll(estateId, search, pageable));
    }

    /**
     * GET /api/v1/estates/{estateId}/persons/picker?q=dupont
     * Picker search — max 10 results, min 2 chars.
     */
    @GetMapping("/picker")
    public ResponseEntity<List<PersonSummaryDTO>> searchForPicker(
            @PathVariable UUID estateId,
            @RequestParam String q) {
        return ResponseEntity.ok(personService.searchForPicker(estateId, q));
    }

    /**
     * GET /api/v1/estates/{estateId}/persons/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PersonDTO> getById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        return ResponseEntity.ok(personService.getById(estateId, id));
    }

    /**
     * POST /api/v1/estates/{estateId}/persons
     */
    @PostMapping
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<PersonDTO> create(
            @PathVariable UUID estateId,
            @Valid @RequestBody CreatePersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.create(estateId, request));
    }

    /**
     * PUT /api/v1/estates/{estateId}/persons/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<PersonDTO> update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonRequest request) {
        return ResponseEntity.ok(personService.update(estateId, id, request));
    }

    /**
     * DELETE /api/v1/estates/{estateId}/persons/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        personService.delete(estateId, id);
        return ResponseEntity.noContent().build();
    }
}
