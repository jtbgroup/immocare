package com.immocare.controller;

import com.immocare.model.dto.*;
import com.immocare.service.PersonService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@PreAuthorize("hasRole('ADMIN')")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * GET /api/v1/persons?page=0&size=20&sort=lastName,asc&search=dupont
     * Paginated person list with optional search.
     */
    @GetMapping
    public ResponseEntity<Page<PersonSummaryDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort,
            @RequestParam(required = false) String search) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(personService.getAll(search, pageable));
    }

    /**
     * GET /api/v1/persons/search?q=dupont
     * Picker search — max 10 results, min 2 chars.
     */
    @GetMapping("/search")
    public ResponseEntity<List<PersonSummaryDTO>> searchForPicker(@RequestParam String q) {
        return ResponseEntity.ok(personService.searchForPicker(q));
    }

    /**
     * GET /api/v1/persons/{id}
     * Full person details including owned buildings/units and leases.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PersonDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    /**
     * POST /api/v1/persons
     * Create a new person.
     */
    @PostMapping
    public ResponseEntity<PersonDTO> create(@Valid @RequestBody CreatePersonRequest request) {
        PersonDTO created = personService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/v1/persons/{id}
     * Update an existing person.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PersonDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdatePersonRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    /**
     * DELETE /api/v1/persons/{id}
     * Delete a person. Returns 409 if still referenced as owner or tenant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
