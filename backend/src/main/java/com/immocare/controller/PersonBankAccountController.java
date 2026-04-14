package com.immocare.controller;

import java.util.List;

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

@RestController
@RequestMapping("/api/v1/persons/{personId}/bank-accounts")
@PreAuthorize("isAuthenticated()")
public class PersonBankAccountController {

    private final PersonBankAccountService service;

    public PersonBankAccountController(PersonBankAccountService service) {
        this.service = service;
    }

    /** GET /api/v1/persons/{personId}/bank-accounts */
    @GetMapping
    public List<PersonBankAccountDTO> getAll(@PathVariable Long personId) {
        return service.getByPerson(personId);
    }

    /** POST /api/v1/persons/{personId}/bank-accounts */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonBankAccountDTO create(@PathVariable Long personId,
            @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.create(personId, req);
    }

    /** PUT /api/v1/persons/{personId}/bank-accounts/{id} */
    @PutMapping("/{id}")
    public PersonBankAccountDTO update(@PathVariable Long personId,
            @PathVariable Long id,
            @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.update(personId, id, req);
    }

    /** DELETE /api/v1/persons/{personId}/bank-accounts/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long personId, @PathVariable Long id) {
        service.delete(personId, id);
    }
}
