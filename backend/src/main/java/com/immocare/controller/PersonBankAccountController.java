package com.immocare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.PersonBankAccountDTO;
import com.immocare.model.dto.SavePersonBankAccountRequest;
import com.immocare.service.PersonBankAccountService;

import jakarta.validation.Valid;

/**
 * REST controller for PersonBankAccount management.
 * UC004_ESTATE_PLACEHOLDER Phase 3: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/persons/{personId}/bank-accounts
 *   POST   /api/v1/estates/{estateId}/persons/{personId}/bank-accounts
 *   PUT    /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}
 *   DELETE /api/v1/estates/{estateId}/persons/{personId}/bank-accounts/{id}
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/persons/{personId}/bank-accounts")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class PersonBankAccountController {

    private final PersonBankAccountService service;

    public PersonBankAccountController(PersonBankAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<PersonBankAccountDTO> getAll(
            @PathVariable UUID estateId,
            @PathVariable Long personId) {
        return service.getByPerson(estateId, personId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public PersonBankAccountDTO create(
            @PathVariable UUID estateId,
            @PathVariable Long personId,
            @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.create(estateId, personId, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public PersonBankAccountDTO update(
            @PathVariable UUID estateId,
            @PathVariable Long personId,
            @PathVariable Long id,
            @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.update(estateId, personId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public void delete(
            @PathVariable UUID estateId,
            @PathVariable Long personId,
            @PathVariable Long id) {
        service.delete(estateId, personId, id);
    }
}
